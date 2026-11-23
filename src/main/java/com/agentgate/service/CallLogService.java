package com.agentgate.service;

import com.agentgate.dto.CallLogMessage;
import com.agentgate.dto.CallLogQueryDTO;
import com.agentgate.dto.PageResult;
import com.agentgate.entity.CallLog;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/CallLogService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface CallLogService {

    /**
     * 按 traceId 幂等写入调用日志；仅首次插入时累加用量统计，两个写操作在同一事务内完成。
     * 必须由其他 Bean 调用以经过 Spring 代理；本实现类内部自调用不会开启事务。
     */
    void saveDirect(CallLogMessage message);

    /**
     * 按筛选条件与页码查询记录；调用方传入的应用范围用于限制查询，页大小在业务层约束。
     */
    PageResult<CallLog> page(CallLogQueryDTO query, Long restrictAppId);

    /**
     * 按 traceId 查询调用记录；存在应用限制时再次核对归属范围，防止日志越权。
     */
    CallLog getByTraceId(String traceId, Long restrictAppId);
}
