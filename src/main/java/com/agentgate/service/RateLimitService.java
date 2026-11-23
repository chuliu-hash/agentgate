package com.agentgate.service;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/RateLimitService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface RateLimitService {

    /**
     * 通过一次 Lua 原子校验并扣减 QPS 令牌与日/月配额；任一不足都不会消耗其余资源，
     * 拒绝时抛出对应业务异常。扣减成功后上游失败也不自动回退。
     */
    void checkAndConsume(Long appId, ApiDefinition api, QuotaPlan plan);
}
