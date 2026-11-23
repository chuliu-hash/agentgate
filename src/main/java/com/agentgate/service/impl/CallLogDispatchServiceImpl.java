package com.agentgate.service.impl;

import com.agentgate.config.RabbitMQConfig;
import com.agentgate.dto.CallLogMessage;
import com.agentgate.service.CallLogDispatchService;
import com.agentgate.service.CallLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 教学说明：convertAndSend 发送调用日志，异常时通过 CallLogService 的 Spring 代理同步落库兜底；发送成功不代表消费者已落库。
 * 文件路径：src/main/java/com/agentgate/service/impl/CallLogDispatchServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class CallLogDispatchServiceImpl implements CallLogDispatchService {

    private final RabbitTemplate rabbitTemplate;
    private final CallLogService callLogService;

    /**
     * 尝试向 MQ 发送调用快照；发送抛异常时同步落库兜底，兜底失败仅记日志。
     */
    @Override
    public void asyncSave(CallLogMessage message) {
        try {
            // 发送 JSON 调用消息到指定交换机与路由；发送返回不代表消费者已经落库。
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CALL_LOG_EXCHANGE,
                    RabbitMQConfig.CALL_LOG_ROUTING_KEY,
                    message);
        } catch (Exception e) {
            // MQ 不可用时同步兜底，日志链路不能影响主请求
            log.warn("调用日志发送 MQ 失败，改为同步落库 traceId={}", message.getTraceId(), e);
            try {
                // 通过独立 Bean 的 Spring 代理写入，保证调用日志和用量统计处于同一事务。
                callLogService.saveDirect(message);
            } catch (Exception fallbackError) {
                // 兜底也失败时只记录错误，绝不向调用方抛出
                log.error("调用日志同步兜底失败 traceId={}", message.getTraceId(), fallbackError);
            }
        }
    }
}
