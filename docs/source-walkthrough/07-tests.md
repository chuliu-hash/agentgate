# 测试代码

[返回源码索引](README.md) · [学习指南](../learning-guide.md)

## StateMachineTest.java

源码：[src/test/java/com/agentgate/enums/StateMachineTest.java](../../src/test/java/com/agentgate/enums/StateMachineTest.java)

JUnit 5 API 与订阅状态转换测试，验证枚举允许/拒绝的边；不替代并发数据库状态更新测试。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    void apiStateTransitions() {
    void subscriptionStateTransitions() {
```

### 完整代码

```java
package com.agentgate.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateMachineTest {

    @Test
    void apiStateTransitions() {
        assertTrue(ApiStatus.DRAFT.canTransitionTo(ApiStatus.PUBLISHED));
        assertTrue(ApiStatus.PUBLISHED.canTransitionTo(ApiStatus.OFFLINE));
        assertTrue(ApiStatus.OFFLINE.canTransitionTo(ApiStatus.PUBLISHED));

        assertFalse(ApiStatus.DRAFT.canTransitionTo(ApiStatus.OFFLINE));
        assertFalse(ApiStatus.PUBLISHED.canTransitionTo(ApiStatus.DRAFT));
        assertFalse(ApiStatus.PUBLISHED.canTransitionTo(ApiStatus.PUBLISHED));
    }

    @Test
    void subscriptionStateTransitions() {
        assertTrue(SubscriptionStatus.PENDING.canTransitionTo(SubscriptionStatus.ACTIVE));
        assertTrue(SubscriptionStatus.PENDING.canTransitionTo(SubscriptionStatus.REJECTED));
        assertTrue(SubscriptionStatus.REJECTED.canTransitionTo(SubscriptionStatus.PENDING));

        assertFalse(SubscriptionStatus.ACTIVE.canTransitionTo(SubscriptionStatus.PENDING));
        assertFalse(SubscriptionStatus.PENDING.canTransitionTo(SubscriptionStatus.PENDING));
    }
}
```

## AesUtilsTest.java

源码：[src/test/java/com/agentgate/utils/AesUtilsTest.java](../../src/test/java/com/agentgate/utils/AesUtilsTest.java)

JUnit 5 加密工具回归测试；对照每个 Test 的输入、调用与断言理解实际覆盖范围，不等同密钥管理安全审计。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final String SECRET = "AgentGate@2026SecretKey32Bytes!!";
    void encryptAndDecryptRoundTrip() {
    void randomIvProducesDifferentCipherText() {
    void tamperedCipherTextFails() {
    void wrongKeyFails() {
```

### 完整代码

```java
package com.agentgate.utils;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesUtilsTest {

    private static final String SECRET = "AgentGate@2026SecretKey32Bytes!!";

    @Test
    void encryptAndDecryptRoundTrip() {
        String plain = "app-secret-0123456789abcdef";
        String cipher = AesUtils.encrypt(plain, SECRET);

        assertNotEquals(plain, cipher);
        assertEquals(plain, AesUtils.decrypt(cipher, SECRET));
    }

    @Test
    void randomIvProducesDifferentCipherText() {
        String plain = "same-plain-text";
        assertNotEquals(AesUtils.encrypt(plain, SECRET), AesUtils.encrypt(plain, SECRET));
    }

    @Test
    void tamperedCipherTextFails() {
        byte[] data = Base64.getDecoder().decode(AesUtils.encrypt("secret", SECRET));
        data[data.length - 1] ^= 0x01;

        assertThrows(IllegalStateException.class,
                () -> AesUtils.decrypt(Base64.getEncoder().encodeToString(data), SECRET));
    }

    @Test
    void wrongKeyFails() {
        String cipher = AesUtils.encrypt("secret", SECRET);
        assertThrows(IllegalStateException.class,
                () -> AesUtils.decrypt(cipher, "another-secret-key"));
    }
}
```

## MaskUtilsTest.java

源码：[src/test/java/com/agentgate/utils/MaskUtilsTest.java](../../src/test/java/com/agentgate/utils/MaskUtilsTest.java)

JUnit 5 脱敏规则测试，检查敏感字段输出及结构；实际具体字段与场景以断言为准。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final ObjectMapper objectMapper = new ObjectMapper();
    void masksSensitiveFieldsRecursively() {
    void masksBankCardKeepingLastFour() {
    void nonJsonContentIsReturnedAsIs() {
```

### 完整代码

```java
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
```

## SignatureUtilsTest.java

源码：[src/test/java/com/agentgate/utils/SignatureUtilsTest.java](../../src/test/java/com/agentgate/utils/SignatureUtilsTest.java)

JUnit 5 签名与 query 规范化测试，检查签名计算/排序编码行为，帮助理解客户端和服务端必须使用同一原始数据。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    void sha256OfEmptyBodyMatchesKnownValue() {
    void hmacSha256MatchesRfc4231StyleVector() {
    void canonicalStringSortsKeysAndValues() {
    void canonicalStringUrlEncodesChineseAndSpace() {
    void parseQueryStringDecodesAndKeepsMultiValues() {
    void parseQueryStringHandlesEmptyAndMalformedInput() {
    void constantTimeEqualsRejectsNullAndDifferentValues() {
```

### 完整代码

```java
package com.agentgate.utils;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureUtilsTest {

    @Test
    void sha256OfEmptyBodyMatchesKnownValue() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                SignatureUtils.sha256Hex(new byte[0]));
    }

    @Test
    void hmacSha256MatchesRfc4231StyleVector() {
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                SignatureUtils.hmacSha256Hex("key",
                        "The quick brown fox jumps over the lazy dog"));
    }

    @Test
    void canonicalStringSortsKeysAndValues() {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("b", new String[]{"2"});
        params.put("a", new String[]{"1", "0"});

        String canonical = SignatureUtils.canonicalString(
                "get", "/openapi/weather", params, "1700000000", "nonce-1", "hash");

        assertEquals("GET\n/openapi/weather\na=0&a=1&b=2\n1700000000\nnonce-1\nhash", canonical);
    }

    @Test
    void canonicalStringUrlEncodesChineseAndSpace() {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("city", new String[]{"北京 1"});

        String query = SignatureUtils.buildQueryString(params);

        assertEquals("city=%E5%8C%97%E4%BA%AC+1", query);
    }

    @Test
    void parseQueryStringDecodesAndKeepsMultiValues() {
        Map<String, String[]> params =
                SignatureUtils.parseQueryString("city=%E5%8C%97%E4%BA%AC+1&tag=a&tag=b&flag");

        assertEquals("city=%E5%8C%97%E4%BA%AC+1&flag=&tag=a&tag=b",
                SignatureUtils.buildQueryString(params));
    }

    @Test
    void parseQueryStringHandlesEmptyAndMalformedInput() {
        assertTrue(SignatureUtils.parseQueryString(null).isEmpty());
        assertTrue(SignatureUtils.parseQueryString("").isEmpty());
        Map<String, String[]> params = SignatureUtils.parseQueryString("bad=%ZZ");
        assertEquals("bad=%25ZZ", SignatureUtils.buildQueryString(params));
    }

    @Test
    void constantTimeEqualsRejectsNullAndDifferentValues() {
        assertTrue(SignatureUtils.constantTimeEquals("abc", "abc"));
        assertFalse(SignatureUtils.constantTimeEquals("abc", "abd"));
        assertFalse(SignatureUtils.constantTimeEquals(null, "abc"));
        assertFalse(SignatureUtils.constantTimeEquals("abc", null));
    }
}
```

## SsrfUtilsTest.java

源码：[src/test/java/com/agentgate/utils/SsrfUtilsTest.java](../../src/test/java/com/agentgate/utils/SsrfUtilsTest.java)

JUnit 5 SSRF 地址规则测试，校验危险地址拒绝与允许场景；不是对所有网络绕过的完整证明。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    void loopbackIsBlockedUnlessExplicitlyAllowed() throws Exception {
    void privateAndMetadataAddressesAreBlocked() throws Exception {
    void publicAddressIsAllowed() throws Exception {
    void ipv6UniqueLocalIsBlocked() throws Exception {
    void rejectsUnsupportedSchemeAndUserInfo() {
```

### 完整代码

```java
package com.agentgate.utils;

import com.agentgate.exception.BizException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsrfUtilsTest {

    @Test
    void loopbackIsBlockedUnlessExplicitlyAllowed() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        assertTrue(SsrfUtils.isBlocked(loopback, false));
        assertFalse(SsrfUtils.isBlocked(loopback, true));

        assertThrows(BizException.class,
                () -> SsrfUtils.validate("http://127.0.0.1:8081/mock-api/weather", false));
        assertDoesNotThrow(
                () -> SsrfUtils.validate("http://127.0.0.1:8081/mock-api/weather", true));
    }

    @Test
    void privateAndMetadataAddressesAreBlocked() throws Exception {
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("10.0.0.1"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("192.168.1.10"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("172.16.0.1"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("169.254.169.254"), false));
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByName("100.64.0.1"), false));
    }

    @Test
    void publicAddressIsAllowed() throws Exception {
        assertFalse(SsrfUtils.isBlocked(InetAddress.getByName("8.8.8.8"), false));
        assertFalse(SsrfUtils.isBlocked(InetAddress.getByName("223.5.5.5"), false));
    }

    @Test
    void ipv6UniqueLocalIsBlocked() throws Exception {
        byte[] address = new byte[16];
        address[0] = (byte) 0xFC;
        address[15] = 0x01;
        assertTrue(SsrfUtils.isBlocked(InetAddress.getByAddress(address), false));
    }

    @Test
    void rejectsUnsupportedSchemeAndUserInfo() {
        assertThrows(BizException.class, () -> SsrfUtils.validate("file:///etc/passwd", true));
        assertThrows(BizException.class, () -> SsrfUtils.validate("ftp://example.com/file", true));
        assertThrows(BizException.class,
                () -> SsrfUtils.validate("http://user:pass@example.com/path", true));
    }
}
```

