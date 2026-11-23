package com.agentgate.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * 教学说明：声明正常与死信 DirectExchange、持久队列、routing key 和绑定；注册 JSON 消息转换器。队列是待处理消息存储，交换机负责路由。
 * 文件路径：src/main/java/com/agentgate/config/RabbitMQConfig.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class RabbitMQConfig {

    public static final String CALL_LOG_EXCHANGE = "call.log.exchange";
    public static final String CALL_LOG_QUEUE = "call.log.queue";
    public static final String CALL_LOG_ROUTING_KEY = "call.log";

    public static final String CALL_LOG_DLX_EXCHANGE = "call.log.dlx.exchange";
    public static final String CALL_LOG_DLQ = "call.log.dlq";
    public static final String CALL_LOG_DLQ_ROUTING_KEY = "call.log.dlq";

    /**
     * 创建持久化正常调用日志交换机，按 routing key 路由消息。
     */
    @Bean
    public DirectExchange callLogExchange() {
        return new DirectExchange(CALL_LOG_EXCHANGE, true, false);
    }

    /**
     * 创建持久化死信交换机，接收拒绝且不重新入队的消息。
     */
    @Bean
    public DirectExchange callLogDlxExchange() {
        return new DirectExchange(CALL_LOG_DLX_EXCHANGE, true, false);
    }

    /**
     * 创建日志队列并指定死信交换机及路由键。
     */
    @Bean
    public Queue callLogQueue() {
        return QueueBuilder.durable(CALL_LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", CALL_LOG_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CALL_LOG_DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 创建死信队列，当前项目不自动消费补偿。
     */
    @Bean
    public Queue callLogDlq() {
        return QueueBuilder.durable(CALL_LOG_DLQ).build();
    }

    /**
     * 把日志队列绑定到正常交换机，使用指定 routing key。
     */
    @Bean
    public Binding callLogBinding() {
        return BindingBuilder.bind(callLogQueue()).to(callLogExchange()).with(CALL_LOG_ROUTING_KEY);
    }

    /**
     * 把死信队列绑定到死信交换机。
     */
    @Bean
    public Binding callLogDlqBinding() {
        return BindingBuilder.bind(callLogDlq()).to(callLogDlxExchange()).with(CALL_LOG_DLQ_ROUTING_KEY);
    }

    /**
     * 注册 JSON 消息转换器，用于 Java 调用快照与 MQ 消息体互转。
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
