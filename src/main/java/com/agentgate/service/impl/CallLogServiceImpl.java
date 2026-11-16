package com.agentgate.service.impl;

import com.agentgate.config.RabbitMQConfig;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallLogServiceImpl implements CallLogService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final CallLogMapper callLogMapper;
    private final UsageStatMapper usageStatMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void asyncSave(CallLogMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CALL_LOG_EXCHANGE,
                    RabbitMQConfig.CALL_LOG_ROUTING_KEY,
                    message);
        } catch (Exception e) {
            // MQ 不可用时同步兜底，日志链路不能影响主请求
            log.warn("调用日志发送 MQ 失败，改为同步落库 traceId={}", message.getTraceId(), e);
            try {
                saveDirect(message);
            } catch (Exception fallbackError) {
                // 兜底也失败时只记录错误，绝不向调用方抛出
                log.error("调用日志同步兜底失败 traceId={}", message.getTraceId(), fallbackError);
            }
        }
    }

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

        int inserted = callLogMapper.insertIgnore(callLog);
        if (inserted == 0) {
            // 重复消息：trace_id 已存在，直接忽略，避免统计重复累加
            return;
        }
        if (message.getAppId() != null && message.getApiId() != null) {
            int isError = message.getStatusCode() != null && message.getStatusCode() >= 400 ? 1 : 0;
            usageStatMapper.upsertIncrement(
                    message.getAppId(), message.getApiId(), LocalDate.now(), isError);
        }
    }

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

        Page<CallLog> page = callLogMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page);
    }

    @Override
    public CallLog getByTraceId(String traceId, Long restrictAppId) {
        CallLog callLog = callLogMapper.selectOne(
                new LambdaQueryWrapper<CallLog>()
                        .eq(CallLog::getTraceId, traceId)
                        .last("limit 1"));
        if (callLog == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "调用日志不存在");
        }
        if (restrictAppId != null && !restrictAppId.equals(callLog.getAppId())) {
            throw new BizException(ErrorCode.APP_ACCESS_DENIED);
        }
        return callLog;
    }
}
