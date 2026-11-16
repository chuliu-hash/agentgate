package com.agentgate.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void masksSensitiveFieldsRecursively() {
        String json = "{\"name\":\"张三\",\"phone\":\"13812345678\","
                + "\"items\":[{\"email\":\"buyer@example.com\"}],"
                + "\"profile\":{\"idCard\":\"110101199001011234\"}}";

        String masked = MaskUtils.maskJson(json, objectMapper);

        assertTrue(masked.contains("\"name\":\"张三\""));
        assertTrue(masked.contains("\"phone\":\"138****5678\""));
        assertTrue(masked.contains("\"email\":\"b***@example.com\""));
        assertTrue(masked.contains("\"idCard\":\"110101****1234\""));
    }

    @Test
    void masksBankCardKeepingLastFour() {
        String masked = MaskUtils.maskJson("{\"bankCard\":\"6222020202020202020\"}", objectMapper);
        assertEquals("{\"bankCard\":\"****2020\"}", masked);
    }

    @Test
    void nonJsonContentIsReturnedAsIs() {
        assertEquals("plain text", MaskUtils.maskJson("plain text", objectMapper));
        assertEquals("", MaskUtils.maskJson("", objectMapper));
    }
}
