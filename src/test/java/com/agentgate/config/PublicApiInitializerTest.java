package com.agentgate.config;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.mapper.ApiDefinitionMapper;
import com.agentgate.mapper.QuotaPlanMapper;
import com.agentgate.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublicApiInitializerTest {
    /** 不启动容器或访问外网，验证真实 API 的初始化参数和存在性分支。 */
    @Test
    void initializesPublicApisAndSkipsExistingRecords() {
        ApiDefinitionMapper mapper = mock(ApiDefinitionMapper.class);
        DataInitializer initializer = new DataInitializer(mock(UserMapper.class), mock(QuotaPlanMapper.class), mapper);
        // 模拟首次不存在；第二次执行时两个编码均存在。
        when(mapper.selectCount(any())).thenReturn(0L, 0L, 1L, 1L);
        ReflectionTestUtils.invokeMethod(initializer, "initPublicApis");
        ReflectionTestUtils.invokeMethod(initializer, "initPublicApis");
        ArgumentCaptor<ApiDefinition> captor = ArgumentCaptor.forClass(ApiDefinition.class);
        verify(mapper, times(2)).insert(captor.capture());
        List<ApiDefinition> rows = captor.getAllValues();
        assertEquals("weather_live", rows.get(0).getCode());
        assertEquals("https://api.open-meteo.com/v1/forecast", rows.get(0).getUpstreamUrl());
        assertEquals("exchange_rate", rows.get(1).getCode());
        assertEquals("https://api.frankfurter.dev/v1/latest", rows.get(1).getUpstreamUrl());
        for (ApiDefinition api : rows) {
            assertEquals("GET", api.getMethod());
            assertEquals("PUBLISHED", api.getStatus());
            assertEquals(Integer.valueOf(1), api.getSensitivityLevel());
        }
    }
}
