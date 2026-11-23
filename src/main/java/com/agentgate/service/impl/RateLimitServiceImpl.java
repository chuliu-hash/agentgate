package com.agentgate.service.impl;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.service.RateLimitService;
import com.agentgate.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 教学说明：PostConstruct 加载单个 Lua 脚本；一次执行内原子完成令牌桶补充/扣减与日/月配额检查/双增，拒绝时不做任何写入。
 * 文件路径：src/main/java/com/agentgate/service/impl/RateLimitServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class RateLimitServiceImpl implements RateLimitService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String CONSUME_SCRIPT_PATH = "lua/rate_limit_consume.lua";

    private final StringRedisTemplate redisTemplate;

    /**
     * 统一提供日期、毫秒与 TTL 计算；生产使用 JVM 默认时区，测试可注入固定时钟。
     */
    private Clock clock = Clock.systemDefaultZone();

    private DefaultRedisScript<Long> consumeScript;

    /**
     * 初始化 Redis Lua 脚本资源及结果类型；真正的原子校验在请求执行脚本时发生。
     */
    @PostConstruct
    public void init() {
        consumeScript = new DefaultRedisScript<>();
        consumeScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(CONSUME_SCRIPT_PATH)));
        consumeScript.setResultType(Long.class);
    }

    /**
     * 单次 Lua 原子完成 QPS 令牌与日/月配额的检查与扣减：任一不足都不消耗其余资源。
     * 扣减成功后，即使后续上游调用失败也不自动回退。
     */
    @Override
    public void checkAndConsume(Long appId, ApiDefinition api, QuotaPlan plan) {
        // 没有套餐就无法确定限流和配额规则，拒绝继续扣减。
        if (plan == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
        }
        if (appId == null || api == null || api.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }

        Integer qpsLimit = plan.getQpsLimit();
        Integer dailyQuota = plan.getDailyQuota();
        Integer monthlyQuota = plan.getMonthlyQuota();
        // 在访问 Redis 前拦截异常配置，避免 null 变成字符串 "null" 让 Lua 无法解析。
        if (qpsLimit == null || qpsLimit <= 0
                || dailyQuota == null || dailyQuota <= 0
                || monthlyQuota == null || monthlyQuota <= 0) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND, "配额套餐配置不完整");
        }

        // 日期、毫秒和 TTL 使用同一时钟，避免跨秒或跨日时三者不一致。
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        ZoneId zone = clock.getZone();

        // key 格式与旧版完全一致，滚动发布期间新旧实例可共享同一份状态。
        String rateKey = RedisConstants.RATE_LIMIT_KEY + appId + ":" + api.getId();
        String dayKey = RedisConstants.QUOTA_DAY_KEY + appId + ":" + api.getId()
                + ":" + today.format(DAY_FORMAT);
        String monthKey = RedisConstants.QUOTA_MONTH_KEY + appId + ":" + api.getId()
                + ":" + today.format(MONTH_FORMAT);

        // 旧计数器保留到周期结束后 60 秒，再自动清理，避免历史 key 长期占用 Redis。
        long dayTtl = Duration.between(now, today.plusDays(1).atStartOfDay(zone)).getSeconds() + 60;
        long monthTtl = Duration.between(now,
                today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone)).getSeconds() + 60;

        // 一次脚本完成全部判断与写入；返回码见脚本头部注释。
        Long result = redisTemplate.execute(
                consumeScript,
                Arrays.asList(rateKey, dayKey, monthKey),
                String.valueOf(qpsLimit),
                String.valueOf(qpsLimit),
                String.valueOf(clock.millis()),
                "1",
                String.valueOf(dailyQuota),
                String.valueOf(monthlyQuota),
                String.valueOf(dayTtl),
                String.valueOf(monthTtl));

        if (result == null) {
            log.error("限流与配额脚本无返回，按系统错误拒绝 appId={} apiId={}", appId, api.getId());
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
        if (result == 1L) {
            return;
        }
        if (result == 0L) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
        if (result == -1L) {
            log.debug("日配额用尽 appId={} apiId={}", appId, api.getId());
            throw new BizException(ErrorCode.QUOTA_EXCEEDED);
        }
        if (result == -2L) {
            log.debug("月配额用尽 appId={} apiId={}", appId, api.getId());
            throw new BizException(ErrorCode.QUOTA_EXCEEDED);
        }
        log.error("限流与配额脚本返回非法值 result={} appId={} apiId={}", result, appId, api.getId());
        throw new BizException(ErrorCode.SYSTEM_ERROR);
    }
}
