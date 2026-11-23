package com.agentgate.service.impl;

import com.agentgate.config.RabbitMQConfig;
import com.agentgate.dto.CallLogMessage;
import com.agentgate.service.CallLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 验证发送服务只负责投递与兜底：发送成功不落库；发送失败委托 CallLogService；兜底失败不向主流程抛出。
 */
@ExtendWith(MockitoExtension.class)
class CallLogDispatchServiceImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private CallLogService callLogService;

    @InjectMocks
    private CallLogDispatchServiceImpl service;

    private final CallLogMessage message = new CallLogMessage();

    @Test
    void mqSendSuccess_doesNotFallback() {
        service.asyncSave(message);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.CALL_LOG_EXCHANGE, RabbitMQConfig.CALL_LOG_ROUTING_KEY, message);
        verifyNoInteractions(callLogService);
    }

    @Test
    void mqSendFailure_fallsBackToSaveDirect() {
        doThrow(new AmqpException("mq down")).when(rabbitTemplate).convertAndSend(
                RabbitMQConfig.CALL_LOG_EXCHANGE, RabbitMQConfig.CALL_LOG_ROUTING_KEY, message);

        service.asyncSave(message);

        verify(callLogService).saveDirect(message);
    }

    @Test
    void fallbackFailure_isSwallowed() {
        doThrow(new AmqpException("mq down")).when(rabbitTemplate).convertAndSend(
                RabbitMQConfig.CALL_LOG_EXCHANGE, RabbitMQConfig.CALL_LOG_ROUTING_KEY, message);
        doThrow(new IllegalStateException("db down")).when(callLogService).saveDirect(message);

        assertDoesNotThrow(() -> service.asyncSave(message));
        verify(callLogService).saveDirect(message);
    }
}
