package com.agentgate.service.impl;

import com.agentgate.dto.CallLogMessage;
import com.agentgate.entity.CallLog;
import com.agentgate.mapper.CallLogMapper;
import com.agentgate.mapper.UsageStatMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证调用日志落库的字段复制、traceId 幂等与用量统计累加边界。
 */
@ExtendWith(MockitoExtension.class)
class CallLogServiceImplTest {

    @Mock
    private CallLogMapper callLogMapper;

    @Mock
    private UsageStatMapper usageStatMapper;

    @InjectMocks
    private CallLogServiceImpl service;

    private CallLogMessage message;

    @BeforeEach
    void setUp() {
        message = new CallLogMessage();
        message.setTraceId("trace-1");
        message.setAppId(7L);
        message.setApiId(9L);
        message.setMethod("GET");
        message.setPath("/openapi/weather");
        message.setStatusCode(200);
        message.setLatencyMs(12L);
        message.setClientIp("127.0.0.1");
        message.setRequestSize(0);
        message.setResponseSize(32);
    }

    @Test
    void firstInsert_copiesFieldsAndUpdatesUsageStat() {
        when(callLogMapper.insertIgnore(any(CallLog.class))).thenReturn(1);

        service.saveDirect(message);

        ArgumentCaptor<CallLog> captor = ArgumentCaptor.forClass(CallLog.class);
        verify(callLogMapper).insertIgnore(captor.capture());
        CallLog saved = captor.getValue();
        assertEquals("trace-1", saved.getTraceId());
        assertEquals(7L, saved.getAppId());
        assertEquals(9L, saved.getApiId());
        assertEquals("GET", saved.getMethod());
        assertEquals("/openapi/weather", saved.getPath());
        assertEquals(200, saved.getStatusCode());
        assertEquals(12L, saved.getLatencyMs());
        assertEquals(32, saved.getResponseSize());

        verify(usageStatMapper).upsertIncrement(eq(7L), eq(9L), any(LocalDate.class), eq(0));
    }

    @Test
    void duplicateInsert_skipsUsageStat() {
        when(callLogMapper.insertIgnore(any(CallLog.class))).thenReturn(0);

        service.saveDirect(message);

        verifyNoInteractions(usageStatMapper);
    }

    @Test
    void errorStatus_incrementsErrorCount() {
        message.setStatusCode(502);
        when(callLogMapper.insertIgnore(any(CallLog.class))).thenReturn(1);

        service.saveDirect(message);

        verify(usageStatMapper).upsertIncrement(eq(7L), eq(9L), any(LocalDate.class), eq(1));
    }

    @Test
    void missingAppOrApi_stillWritesLogButSkipsUsageStat() {
        message.setApiId(null);
        when(callLogMapper.insertIgnore(any(CallLog.class))).thenReturn(1);

        service.saveDirect(message);

        verify(callLogMapper).insertIgnore(any(CallLog.class));
        verifyNoInteractions(usageStatMapper);
    }

    @Test
    void mapperFailure_propagatesForTransactionRollback() {
        when(callLogMapper.insertIgnore(any(CallLog.class)))
                .thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> service.saveDirect(message));
        verifyNoInteractions(usageStatMapper);
    }
}
