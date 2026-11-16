package com.agentgate.service.impl;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> tokenBucketScript;
    private DefaultRedisScript<Long> quotaScript;

    @PostConstruct
    public void init() {
        tokenBucketScript = new DefaultRedisScript<>();
        tokenBucketScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/token_bucket.lua")));
        tokenBucketScript.setResultType(Long.class);

        quotaScript = new DefaultRedisScript<>();
        quotaScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/quota.lua")));
        quotaScript.setResultType(Long.class);
    }

    @Override
    public void checkAndConsume(Long appId, ApiDefinition api, QuotaPlan plan) {
        if (plan == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
        }
        int qps = plan.getQpsLimit() == null ? 1 : plan.getQpsLimit();
        String rateKey = "ratelimit:" + appId + ":" + api.getId();
        Long allowed = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(rateKey),
                String.valueOf(qps),
                String.valueOf(qps),
                String.valueOf(System.currentTimeMillis()),
                "1");
        if (allowed == null || allowed == 0L) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }

        LocalDate today = LocalDate.now();
        String dayKey = "quota:day:" + appId + ":" + api.getId() + ":" + today.format(DAY_FORMAT);
        String monthKey = "quota:month:" + appId + ":" + api.getId() + ":" + today.format(MONTH_FORMAT);
        long dayTtl = Duration.between(LocalDateTime.now(), today.plusDays(1).atStartOfDay()).getSeconds() + 60;
        long monthTtl = Duration.between(LocalDateTime.now(),
                today.withDayOfMonth(1).plusMonths(1).atStartOfDay()).getSeconds() + 60;

        Long quotaAllowed = redisTemplate.execute(
                quotaScript,
                Arrays.asList(dayKey, monthKey),
                String.valueOf(plan.getDailyQuota()),
                String.valueOf(plan.getMonthlyQuota()),
                String.valueOf(dayTtl),
                String.valueOf(monthTtl));
        if (quotaAllowed == null || quotaAllowed == 0L) {
            throw new BizException(ErrorCode.QUOTA_EXCEEDED);
        }
    }
}
