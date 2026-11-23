package com.agentgate.service;

import com.agentgate.dto.DashboardSummaryDTO;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/DashboardService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface DashboardService {

    /**
     * 聚合当日调用日志生成看板，appId 为空表示全局；成功按 2xx 统计，异步日志会导致短时滞后。
     */
    DashboardSummaryDTO summary(Long appId);
}
