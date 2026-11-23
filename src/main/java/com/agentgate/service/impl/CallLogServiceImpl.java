package com.agentgate.service.impl;

import com.agentgate.dto.CallLogMessage;
import com.agentgate.dto.CallLogQueryDTO;
import com.agentgate.dto.PageResult;
import com.agentgate.entity.CallLog;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.CallLogMapper;
import com.agentgate.mapper.UsageStatMapper;
import com.agentgate.service.CallLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
/**
 * 教学说明：事务写入调用日志与日用量统计，并提供范围受限的分页与 traceId 查询；MQ 发送职责已拆到 CallLogDispatchService。
 * 文件路径：src/main/java/com/agentgate/service/impl/CallLogServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class CallLogServiceImpl implements CallLogService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final CallLogMapper callLogMapper;
    private final UsageStatMapper usageStatMapper;

    /**
     * 按 traceId 幂等写入调用日志；仅首次插入时累加用量统计，两个写操作在同一事务内完成。
     * 必须由其他 Bean 调用以经过 Spring 代理；本类内部自调用不会开启事务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDirect(CallLogMessage message) {
        CallLog callLog = new CallLog();
        callLog.setTraceId(message.getTraceId());
        callLog.setAppId(message.getAppId());
        callLog.setApiId(message.getApiId());
        callLog.setMethod(message.getMethod());
        callLog.setPath(message.getPath());
        callLog.setStatusCode(message.getStatusCode());
        callLog.setLatencyMs(message.getLatencyMs());
        callLog.setClientIp(message.getClientIp());
        callLog.setErrorCode(message.getErrorCode());
        callLog.setRequestSize(message.getRequestSize());
        callLog.setResponseSize(message.getResponseSize());
        callLog.setCreateTime(LocalDateTime.now());

        // trace_id 唯一约束使重复投递幂等；重复消息不能再次增加统计。
        int inserted = callLogMapper.insertIgnore(callLog);
        if (inserted == 0) {
            return;
        }
        if (message.getAppId() != null && message.getApiId() != null) {
            int isError = message.getStatusCode() != null && message.getStatusCode() >= 400 ? 1 : 0;
            // 与日志插入处于同一事务，统计更新失败时日志插入也回滚。
            usageStatMapper.upsertIncrement(
                    message.getAppId(), message.getApiId(), LocalDate.now(), isError);
        }
    }

    /**
     * 按筛选条件与页码查询记录；调用方传入的应用范围用于限制查询，页大小在业务层约束。
     */
    @Override
    public PageResult<CallLog> page(CallLogQueryDTO query, Long restrictAppId) {
        long current = Math.max(1L, query.getPage());
        long size = Math.min(Math.max(1L, query.getSize()), MAX_PAGE_SIZE);
        Long appId = restrictAppId != null ? restrictAppId : query.getAppId();

        LambdaQueryWrapper<CallLog> wrapper = new LambdaQueryWrapper<CallLog>()
                .eq(appId != null, CallLog::getAppId, appId)
                .eq(query.getApiId() != null, CallLog::getApiId, query.getApiId())
                .eq(StringUtils.hasText(query.getTraceId()), CallLog::getTraceId, query.getTraceId())
                .ge(query.getStartTime() != null, CallLog::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, CallLog::getCreateTime, query.getEndTime())
                .orderByDesc(CallLog::getCreateTime);

        // 把筛选条件与分页参数交给 MyBatis-Plus，避免将全部记录加载后再分页。
        Page<CallLog> page = callLogMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page);
    }

    /**
     * 按 traceId 查询调用记录；存在应用限制时再次核对归属范围，防止日志越权。
     */
    @Override
    public CallLog getByTraceId(String traceId, Long restrictAppId) {
        CallLog callLog = callLogMapper.selectOne(
                new LambdaQueryWrapper<CallLog>()
                        .eq(CallLog::getTraceId, traceId)
                        .last("limit 1"));
        // 校验条件成立时拒绝本次操作，抛出 NOT_FOUND；不再继续后续正常业务步骤。
        if (callLog == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "调用日志不存在");
        }
        // 校验条件成立时拒绝本次操作，抛出 APP_ACCESS_DENIED；不再继续后续正常业务步骤。
        if (restrictAppId != null && !restrictAppId.equals(callLog.getAppId())) {
            throw new BizException(ErrorCode.APP_ACCESS_DENIED);
        }
        return callLog;
    }
}
