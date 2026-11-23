package com.agentgate.config;

import cn.hutool.crypto.digest.BCrypt;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.entity.User;
import com.agentgate.enums.ApiStatus;
import com.agentgate.enums.UserRole;
import com.agentgate.enums.UserStatus;
import com.agentgate.mapper.ApiDefinitionMapper;
import com.agentgate.mapper.QuotaPlanMapper;
import com.agentgate.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐管理员、演示套餐与演示 API，重复启动幂等。
 * 建表脚本见 src/main/resources/db/agentgate.sql。
 */
@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 教学说明：ApplicationRunner 启动回调：按存在性补齐管理员、三档套餐和 weather/order/user 演示 API；需先执行建表 SQL，不更新已存在记录。
 * 文件路径：src/main/java/com/agentgate/config/DataInitializer.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final QuotaPlanMapper quotaPlanMapper;
    private final ApiDefinitionMapper apiDefinitionMapper;

    @Value("${agentgate.admin.username:admin}")
    private String adminUsername;

    @Value("${agentgate.admin.password:admin123}")
    private String adminPassword;

    @Value("${server.port:8081}")
    private int serverPort;

    /**
     * 启动阶段依次补齐管理员、套餐和示例 API；数据库表需预先创建。
     */
    @Override
    public void run(ApplicationArguments args) {
        initAdmin();
        initQuotaPlans();
        initDemoApis();
        initPublicApis();
    }

    /**
     * 仅在管理员用户名不存在时创建账号，已有记录不会因配置改变而自动重设密码。
     */
    private void initAdmin() {
        // 按用户名统计已有账号；这是友好提示，不能代替数据库唯一索引的并发约束。
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, adminUsername));
        if (count != null && count > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(BCrypt.hashpw(adminPassword, BCrypt.gensalt()));
        admin.setNickname("平台管理员");
        admin.setRole(UserRole.ADMIN.name());
        admin.setStatus(UserStatus.ACTIVE.name());
        // 执行插入并由数据库约束最终校验唯一性；实体主键按表配置回填。
        userMapper.insert(admin);
        log.info("已初始化管理员账号 {}", adminUsername);
    }

    /**
     * 调用存在性检查补齐预设配额套餐；不覆盖已存在套餐。
     */
    private void initQuotaPlans() {
        insertPlanIfAbsent("试用版", 2, 1000, 10000, "适合接入联调与演示");
        insertPlanIfAbsent("标准版", 10, 10000, 100000, "适合中小规模业务");
        insertPlanIfAbsent("企业版", 50, 100000, 1000000, "适合高并发生产场景");
    }

    /**
     * 按套餐名称检查后插入初始套餐；不是更新已有套餐。
     */
    private void insertPlanIfAbsent(String name, int qps, int daily, int monthly, String description) {
        Long count = quotaPlanMapper.selectCount(
                new LambdaQueryWrapper<QuotaPlan>().eq(QuotaPlan::getName, name));
        if (count != null && count > 0) {
            return;
        }
        QuotaPlan plan = new QuotaPlan();
        plan.setName(name);
        plan.setQpsLimit(qps);
        plan.setDailyQuota(daily);
        plan.setMonthlyQuota(monthly);
        plan.setDescription(description);
        // 执行插入并由数据库约束最终校验唯一性；实体主键按表配置回填。
        quotaPlanMapper.insert(plan);
        log.info("已初始化配额套餐 {}", name);
    }

    /**
     * 补齐指向本机 Mock 服务的示例 API，仅用于联调。
     */
    private void initDemoApis() {
        String base = "http://127.0.0.1:" + serverPort + "/mock-api";
        insertApiIfAbsent("weather", "天气查询", "GET", base + "/weather", 1,
                "演示用上游：返回城市天气");
        insertApiIfAbsent("order", "订单查询", "GET", base + "/order", 2,
                "演示敏感级别 2：响应中的手机号/邮箱/地址会被脱敏");
        insertApiIfAbsent("user", "用户信息查询", "GET", base + "/user", 3,
                "演示敏感级别 3：身份证/银行卡等字段会被脱敏");
    }

    /**
     * 补齐无需第三方密钥的真实 API；只登记配置，不在启动时请求公网。
     * 同编码已存在时保留管理员配置，调用仍需申请订阅并通过审批。
     */
    private void initPublicApis() {
        // 天气免费服务限非商业用途；调用方提供经纬度和所需天气指标。
        insertApiIfAbsent("weather_live", "天气查询（Open-Meteo）", "GET",
                "https://api.open-meteo.com/v1/forecast", 1,
                "来源 Open-Meteo，免费层限非商业用途；参数 latitude、longitude、current=temperature_2m");
        // Frankfurter 返回带日期的参考汇率，不是实时交易报价。
        insertApiIfAbsent("exchange_rate", "汇率查询（Frankfurter）", "GET",
                "https://api.frankfurter.dev/v1/latest", 1,
                "来源 Frankfurter，参考汇率；参数 base=USD、symbols=CNY,EUR");
    }

    /**
     * 按编码检查后插入示例 API；已有记录保留原配置。
     */
    private void insertApiIfAbsent(String code, String name, String method, String upstream,
                                   int sensitivityLevel, String description) {
        Long count = apiDefinitionMapper.selectCount(
                new LambdaQueryWrapper<ApiDefinition>().eq(ApiDefinition::getCode, code));
        if (count != null && count > 0) {
            return;
        }
        ApiDefinition api = new ApiDefinition();
        api.setCode(code);
        api.setName(name);
        api.setMethod(method);
        api.setUpstreamUrl(upstream);
        api.setVersion("v1");
        api.setSensitivityLevel(sensitivityLevel);
        api.setStatus(ApiStatus.PUBLISHED.name());
        api.setTimeoutMs(5000);
        api.setDescription(description);
        // 执行插入并由数据库约束最终校验唯一性；实体主键按表配置回填。
        apiDefinitionMapper.insert(api);
        log.info("已初始化演示 API {}", code);
    }
}
