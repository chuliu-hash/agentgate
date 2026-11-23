package com.agentgate.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * 教学说明：JUnit 5 脱敏规则测试，检查敏感字段输出及结构；实际具体字段与场景以断言为准。
 * 文件路径：src/test/java/com/agentgate/utils/MaskUtilsTest.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

class MaskUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证 masksSensitiveFieldsRecursively 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
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

    /**
     * 验证 masksBankCardKeepingLastFour 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void masksBankCardKeepingLastFour() {
        String masked = MaskUtils.maskJson("{\"bankCard\":\"6222020202020202020\"}", objectMapper);
        assertEquals("{\"bankCard\":\"****2020\"}", masked);
    }

    /**
     * 验证 nonJsonContentIsReturnedAsIs 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void nonJsonContentIsReturnedAsIs() {
        assertEquals("plain text", MaskUtils.maskJson("plain text", objectMapper));
        assertEquals("", MaskUtils.maskJson("", objectMapper));
    }
}
