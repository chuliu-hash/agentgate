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
/**
 * 教学说明：RabbitMQ 消费入口：调用事务持久化 Service，成功手动 ACK，失败 NACK 且不重新入原队列，消息进入配置的死信路由。
 * 文件路径：src/main/java/com/agentgate/listener/CallLogListener.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class CallLogListener {

    private final CallLogService callLogService;

    /**
     * 消费调用快照，落库成功后手动 ACK；异常时拒绝且不重回原队列，按队列配置进入死信链路。
     */
    @RabbitListener(queues = RabbitMQConfig.CALL_LOG_QUEUE)
    public void onMessage(CallLogMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            callLogService.saveDirect(message);
            // 教学说明：先成功落库再确认消息。false 表示仅确认这一条；未确认消息可能重新投递，数据库必须做幂等。
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("调用日志消费失败 traceId={}", message.getTraceId(), e);
            // 拒绝本条消息且不回原队列，交给已配置的死信路由；不会自动重试补偿。
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
