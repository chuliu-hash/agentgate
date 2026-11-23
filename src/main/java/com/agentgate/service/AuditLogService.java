package com.agentgate.service;

import com.agentgate.dto.PageResult;
import com.agentgate.entity.AuditLog;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/AuditLogService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface AuditLogService {

    /**
     * 持久化管理操作审计记录；审计写入的异常处理由调用切面决定。
     */
    void save(AuditLog auditLog);

    /**
     * 按筛选条件与页码查询记录；调用方传入的应用范围用于限制查询，页大小在业务层约束。
     */
    PageResult<AuditLog> page(long page, long size);
}
