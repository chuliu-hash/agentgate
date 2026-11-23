package com.agentgate.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * 教学说明：JUnit 5 签名与 query 规范化测试，检查签名计算/排序编码行为，帮助理解客户端和服务端必须使用同一原始数据。
 * 文件路径：src/test/java/com/agentgate/utils/SignatureUtilsTest.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

class SignatureUtilsTest {

    /**
     * 验证 sha256OfEmptyBodyMatchesKnownValue 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void sha256OfEmptyBodyMatchesKnownValue() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                SignatureUtils.sha256Hex(new byte[0]));
    }

    /**
     * 验证 hmacSha256MatchesRfc4231StyleVector 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void hmacSha256MatchesRfc4231StyleVector() {
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                SignatureUtils.hmacSha256Hex("key",
                        "The quick brown fox jumps over the lazy dog"));
    }

    /**
     * 验证 canonicalStringSortsKeysAndValues 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void canonicalStringSortsKeysAndValues() {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("b", new String[]{"2"});
        params.put("a", new String[]{"1", "0"});

        String canonical = SignatureUtils.canonicalString(
                "get", "/openapi/weather", params, "1700000000", "nonce-1", "hash");

        assertEquals("GET\n/openapi/weather\na=0&a=1&b=2\n1700000000\nnonce-1\nhash", canonical);
    }

    /**
     * 验证 canonicalStringUrlEncodesChineseAndSpace 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void canonicalStringUrlEncodesChineseAndSpace() {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("city", new String[]{"北京 1"});

        String query = SignatureUtils.buildQueryString(params);

        assertEquals("city=%E5%8C%97%E4%BA%AC+1", query);
    }

    /**
     * 验证 parseQueryStringDecodesAndKeepsMultiValues 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void parseQueryStringDecodesAndKeepsMultiValues() {
        Map<String, String[]> params =
                SignatureUtils.parseQueryString("city=%E5%8C%97%E4%BA%AC+1&tag=a&tag=b&flag");

        assertEquals("city=%E5%8C%97%E4%BA%AC+1&flag=&tag=a&tag=b",
                SignatureUtils.buildQueryString(params));
    }

    /**
     * 验证 parseQueryStringHandlesEmptyAndMalformedInput 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void parseQueryStringHandlesEmptyAndMalformedInput() {
        assertTrue(SignatureUtils.parseQueryString(null).isEmpty());
        assertTrue(SignatureUtils.parseQueryString("").isEmpty());
        Map<String, String[]> params = SignatureUtils.parseQueryString("bad=%ZZ");
        assertEquals("bad=%25ZZ", SignatureUtils.buildQueryString(params));
    }

    /**
     * 验证 constantTimeEqualsRejectsNullAndDifferentValues 场景的输入与预期输出；具体覆盖边界以本方法的断言为准。
     */
    @Test
    void constantTimeEqualsRejectsNullAndDifferentValues() {
        assertTrue(SignatureUtils.constantTimeEquals("abc", "abc"));
        assertFalse(SignatureUtils.constantTimeEquals("abc", "abd"));
        assertFalse(SignatureUtils.constantTimeEquals(null, "abc"));
        assertFalse(SignatureUtils.constantTimeEquals("abc", null));
    }
}
