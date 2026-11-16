package com.agentgate.listener;

import com.agentgate.config.RabbitMQConfig;
import com.agentgate.dto.CallLogMessage;
import com.agentgate.service.CallLogService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 异步落库调用日志：手动 ACK，失败进入死信队列。
 * trace_id 唯一索引保证重复消息不会重复计数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallLogListener {

    private final CallLogService callLogService;

    @RabbitListener(queues = RabbitMQConfig.CALL_LOG_QUEUE)
    public void onMessage(CallLogMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            callLogService.saveDirect(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("调用日志消费失败 traceId={}", message.getTraceId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
