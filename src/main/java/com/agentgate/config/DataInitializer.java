package com.agentgate.config;

import cn.hutool.crypto.digest.BCrypt;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.entity.User;
import com.agentgate.enums.ApiStatus;
import com.agentgate.enums.UserRole;
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

    @Override
    public void run(ApplicationArguments args) {
        initAdmin();
        initQuotaPlans();
        initDemoApis();
    }

    private void initAdmin() {
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
        admin.setStatus("ACTIVE");
        userMapper.insert(admin);
        log.info("已初始化管理员账号 {}", adminUsername);
    }

    private void initQuotaPlans() {
        insertPlanIfAbsent("试用版", 2, 1000, 10000, "适合接入联调与演示");
        insertPlanIfAbsent("标准版", 10, 10000, 100000, "适合中小规模业务");
        insertPlanIfAbsent("企业版", 50, 100000, 1000000, "适合高并发生产场景");
    }

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
        quotaPlanMapper.insert(plan);
        log.info("已初始化配额套餐 {}", name);
    }

    private void initDemoApis() {
        String base = "http://127.0.0.1:" + serverPort + "/mock-api";
        insertApiIfAbsent("weather", "天气查询", "GET", base + "/weather", 1,
                "演示用上游：返回城市天气");
        insertApiIfAbsent("order", "订单查询", "GET", base + "/order", 2,
                "演示敏感级别 2：响应中的手机号/邮箱/地址会被脱敏");
        insertApiIfAbsent("user", "用户信息查询", "GET", base + "/user", 3,
                "演示敏感级别 3：身份证/银行卡等字段会被脱敏");
    }

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
        apiDefinitionMapper.insert(api);
        log.info("已初始化演示 API {}", code);
    }
}
