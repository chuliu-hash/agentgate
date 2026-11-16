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
public class RabbitMQConfig {

    public static final String CALL_LOG_EXCHANGE = "call.log.exchange";
    public static final String CALL_LOG_QUEUE = "call.log.queue";
    public static final String CALL_LOG_ROUTING_KEY = "call.log";

    public static final String CALL_LOG_DLX_EXCHANGE = "call.log.dlx.exchange";
    public static final String CALL_LOG_DLQ = "call.log.dlq";
    public static final String CALL_LOG_DLQ_ROUTING_KEY = "call.log.dlq";

    @Bean
    public DirectExchange callLogExchange() {
        return new DirectExchange(CALL_LOG_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange callLogDlxExchange() {
        return new DirectExchange(CALL_LOG_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue callLogQueue() {
        return QueueBuilder.durable(CALL_LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", CALL_LOG_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CALL_LOG_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue callLogDlq() {
        return QueueBuilder.durable(CALL_LOG_DLQ).build();
    }

    @Bean
    public Binding callLogBinding() {
        return BindingBuilder.bind(callLogQueue()).to(callLogExchange()).with(CALL_LOG_ROUTING_KEY);
    }

    @Bean
    public Binding callLogDlqBinding() {
        return BindingBuilder.bind(callLogDlq()).to(callLogDlxExchange()).with(CALL_LOG_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
