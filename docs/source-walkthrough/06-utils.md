# 公共工具

[返回源码索引](README.md) · [学习指南](../learning-guide.md)

## AesUtils.java

源码：[src/main/java/com/agentgate/utils/AesUtils.java](../../src/main/java/com/agentgate/utils/AesUtils.java)

AES/GCM/NoPadding 可逆加密：随机 12 字节 IV，SHA-256 派生密钥，Base64 存 IV+密文+Tag；用于数据库 AppSecret 保护。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private AesUtils() {
    public static String encrypt(String plainText, String secret) {
            throw new IllegalStateException("AES encrypt failed", e);
    public static String decrypt(String base64CipherText, String secret) {
            return new String(plain, StandardCharsets.UTF_8);
            throw new IllegalStateException("AES decrypt failed", e);
    private static SecretKeySpec key(String secret) throws Exception {
        return new SecretKeySpec(key, "AES");
```

### 完整代码

```java
package com.agentgate.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM，密钥由配置中的 secret 经 SHA-256 派生。
 * 密文格式：Base64(IV(12B) + CipherText + Tag)。
 */
public final class AesUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AesUtils() {
    }

    public static String encrypt(String plainText, String secret) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key(secret), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String base64CipherText, String secret) {
        try {
            byte[] data = Base64.getDecoder().decode(base64CipherText);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key(secret), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] plain = cipher.doFinal(data, IV_LENGTH, data.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES decrypt failed", e);
        }
    }

    private static SecretKeySpec key(String secret) throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}
```

## CacheClient.java

源码：[src/main/java/com/agentgate/utils/CacheClient.java](../../src/main/java/com/agentgate/utils/CacheClient.java)

封装 JSON 缓存和 getOrLoad：空值缓存、随机 TTL、Redisson 抢锁与二次查缓存；锁失败仍可能回源，不是绝对串行重建。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    public <T> T get(String key, Class<T> type) {
    public void set(String key, Object value, long ttlMinutes) {
            throw new IllegalStateException("缓存写入失败", e);
    public void setNull(String key) {
    public void delete(String key) {
    public <T> T getOrLoad(String key, Class<T> type, long ttlMinutes, Supplier<T> loader) {
```

### 完整代码

```java
package com.agentgate.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheClient {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    public <T> T get(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，删除脏数据 key={}", key, e);
            redisTemplate.delete(key);
            return null;
        }
    }

    public void set(String key, Object value, long ttlMinutes) {
        try {
            String json = objectMapper.writeValueAsString(value);
            // 随机抖动，避免同一批 key 同时失效
            long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 120);
            redisTemplate.opsForValue().set(key, json, ttlMinutes * 60 + jitterSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("缓存写入失败", e);
        }
    }

    public void setNull(String key) {
        redisTemplate.opsForValue().set(key, RedisConstants.CACHE_NULL_VALUE, 1, TimeUnit.MINUTES);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Cache Aside + 空值缓存 + Redisson 互斥重建。
     */
    public <T> T getOrLoad(String key, Class<T> type, long ttlMinutes, Supplier<T> loader) {
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            // 空值缓存命中
            return null;
        }
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_CACHE_KEY + key);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 10, TimeUnit.SECONDS);
            if (locked) {
                cached = get(key, type);
                if (cached != null) {
                    return cached;
                }
                if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    return null;
                }
                T value = loader.get();
                if (value == null) {
                    setNull(key);
                    return null;
                }
                set(key, value, ttlMinutes);
                return value;
            }
            // 未抢到锁：等待一小段时间后重试缓存，仍没有再回源
            Thread.sleep(100);
            cached = get(key, type);
            return cached != null ? cached : loader.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

## CachedBodyHttpServletRequest.java

源码：[src/main/java/com/agentgate/utils/CachedBodyHttpServletRequest.java](../../src/main/java/com/agentgate/utils/CachedBodyHttpServletRequest.java)

包装 Servlet 请求并缓存有大小限制的 body；覆盖输入流/Reader 使验签后代理仍能读取；超限通过标志交由 Filter 拒绝。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final byte[] cachedBody;
    private final boolean tooLarge;
    public CachedBodyHttpServletRequest(HttpServletRequest request, int maxBytes) throws IOException {
    public byte[] getCachedBody() {
    public boolean isTooLarge() {
    public ServletInputStream getInputStream() {
        return new ServletInputStream() {
            public boolean isFinished() {
            public boolean isReady() {
            public void setReadListener(ReadListener readListener) {
            public int read() {
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
```

### 完整代码

```java
package com.agentgate.utils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 缓存请求体，使签名校验读取 body 后，下游 Controller 仍能再次读取。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;
    private final boolean tooLarge;

    public CachedBodyHttpServletRequest(HttpServletRequest request, int maxBytes) throws IOException {
        super(request);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        boolean overflow = false;
        try (InputStream input = request.getInputStream()) {
            int len;
            while ((len = input.read(buffer)) != -1) {
                total += len;
                if (total > maxBytes) {
                    overflow = true;
                    break;
                }
                output.write(buffer, 0, len);
            }
        }
        this.cachedBody = output.toByteArray();
        this.tooLarge = overflow;
    }

    public byte[] getCachedBody() {
        return cachedBody;
    }

    public boolean isTooLarge() {
        return tooLarge;
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream input = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // no-op
            }

            @Override
            public int read() {
                return input.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        Charset charset = StandardCharsets.UTF_8;
        String encoding = getCharacterEncoding();
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            } catch (Exception ignored) {
                // 使用 UTF-8
            }
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
```

## IpUtils.java

源码：[src/main/java/com/agentgate/utils/IpUtils.java](../../src/main/java/com/agentgate/utils/IpUtils.java)

取得客户端 IP 并匹配白名单；默认使用远端地址，仅信任代理配置打开时使用转发头；支持精确 IP 与 *，不支持 CIDR。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private IpUtils() {
    public static String getClientIp(HttpServletRequest request) {
        return getClientIp(request, false);
    public static String getClientIp(HttpServletRequest request, boolean trustProxy) {
            return normalize(request.getRemoteAddr());
            return normalize(ip.trim());
            return normalize(ip.trim());
        return normalize(request.getRemoteAddr());
    public static boolean isAllowed(String clientIp, String whitelist) {
    private static String normalize(String ip) {
```

### 完整代码

```java
package com.agentgate.utils;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public final class IpUtils {

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        return getClientIp(request, false);
    }

    /**
     * @param trustProxy 只有部署在可信反向代理之后才可开启；否则客户端可伪造 X-Forwarded-For 绕过 IP 白名单
     */
    public static String getClientIp(HttpServletRequest request, boolean trustProxy) {
        if (!trustProxy) {
            return normalize(request.getRemoteAddr());
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            ip = index > 0 ? ip.substring(0, index) : ip;
            return normalize(ip.trim());
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return normalize(ip.trim());
        }
        return normalize(request.getRemoteAddr());
    }

    public static boolean isAllowed(String clientIp, String whitelist) {
        if (!StringUtils.hasText(whitelist)) {
            return true;
        }
        String[] items = whitelist.split(",");
        for (String item : items) {
            String allowed = item.trim();
            if ("*".equals(allowed)) {
                return true;
            }
            if (allowed.equals(clientIp)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
```

## MaskUtils.java

源码：[src/main/java/com/agentgate/utils/MaskUtils.java](../../src/main/java/com/agentgate/utils/MaskUtils.java)

递归遍历 JSON 对象与数组，按字段名匹配并掩码电话、证件、邮箱、地址、银行卡及密钥字段；解析失败原样返回。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
    private MaskUtils() {
    public static String maskJson(String json, ObjectMapper objectMapper) {
    private static void maskNode(JsonNode node) {
    private static String maskValue(String field, String value) {
            return maskMiddle(value, 3, 4);
            return maskMiddle(value, 6, 4);
    private static String maskMiddle(String value, int prefix, int suffix) {
```

### 完整代码

```java
package com.agentgate.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MaskUtils {

    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "phone", "mobile", "telephone", "idcard", "idcardno", "idno",
            "email", "address", "bankcard", "cardno", "password", "secret",
            "token", "accesskey", "secretkey"));

    private MaskUtils() {
    }

    public static String maskJson(String json, ObjectMapper objectMapper) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            maskNode(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            // 非 JSON 或解析失败时原样返回，避免影响业务响应
            return json;
        }
    }

    private static void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String field = entry.getKey().toLowerCase(Locale.ROOT);
                JsonNode value = entry.getValue();
                if (SENSITIVE_FIELDS.contains(field) && value.isValueNode()) {
                    objectNode.put(entry.getKey(), maskValue(field, value.asText()));
                } else {
                    maskNode(value);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskNode(child);
            }
        }
    }

    private static String maskValue(String field, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if ("phone".equals(field) || "mobile".equals(field) || "telephone".equals(field)) {
            return maskMiddle(value, 3, 4);
        }
        if ("idcard".equals(field) || "idcardno".equals(field) || "idno".equals(field)) {
            return maskMiddle(value, 6, 4);
        }
        if ("email".equals(field)) {
            int at = value.indexOf('@');
            if (at <= 0) {
                return "******";
            }
            return value.charAt(0) + "***" + value.substring(at);
        }
        if ("address".equals(field)) {
            return value.length() <= 6 ? "******" : value.substring(0, 6) + "****";
        }
        if ("bankcard".equals(field) || "cardno".equals(field)) {
            return value.length() <= 4 ? "****" : "****" + value.substring(value.length() - 4);
        }
        return "******";
    }

    private static String maskMiddle(String value, int prefix, int suffix) {
        if (value.length() <= prefix + suffix) {
            return "******";
        }
        return value.substring(0, prefix)
                + "****"
                + value.substring(value.length() - suffix);
    }
}
```

## RedisConstants.java

源码：[src/main/java/com/agentgate/utils/RedisConstants.java](../../src/main/java/com/agentgate/utils/RedisConstants.java)

集中管理登录、Nonce、鉴权缓存、订阅缓存及锁 key 前缀和有效期；改前缀会影响已有缓存与登录态。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private RedisConstants() {
    public static final String LOGIN_TOKEN_KEY = "login:token:";
    public static final long LOGIN_TOKEN_TTL_MINUTES = 120L;
    public static final String CACHE_APP_KEY = "cache:app:";
    public static final long CACHE_APP_TTL_MINUTES = 30L;
    public static final String CACHE_API_KEY = "cache:api:";
    public static final long CACHE_API_TTL_MINUTES = 10L;
    public static final String CACHE_SUB_KEY = "cache:sub:";
    public static final long CACHE_SUB_TTL_MINUTES = 10L;
    public static final String CACHE_NULL_VALUE = "";
    public static final String NONCE_KEY = "nonce:";
    public static final String RATE_LIMIT_KEY = "ratelimit:";
    public static final String QUOTA_DAY_KEY = "quota:day:";
    public static final String QUOTA_MONTH_KEY = "quota:month:";
    public static final String LOCK_CACHE_KEY = "lock:cache:";
```

### 完整代码

```java
package com.agentgate.utils;

public final class RedisConstants {

    private RedisConstants() {
    }

    public static final String LOGIN_TOKEN_KEY = "login:token:";
    public static final long LOGIN_TOKEN_TTL_MINUTES = 120L;

    public static final String CACHE_APP_KEY = "cache:app:";
    public static final long CACHE_APP_TTL_MINUTES = 30L;

    public static final String CACHE_API_KEY = "cache:api:";
    public static final long CACHE_API_TTL_MINUTES = 10L;

    public static final String CACHE_SUB_KEY = "cache:sub:";
    public static final long CACHE_SUB_TTL_MINUTES = 10L;

    public static final String CACHE_NULL_VALUE = "";

    public static final String NONCE_KEY = "nonce:";
    public static final String RATE_LIMIT_KEY = "ratelimit:";
    public static final String QUOTA_DAY_KEY = "quota:day:";
    public static final String QUOTA_MONTH_KEY = "quota:month:";
    public static final String LOCK_CACHE_KEY = "lock:cache:";
}
```

## SignatureUtils.java

源码：[src/main/java/com/agentgate/utils/SignatureUtils.java](../../src/main/java/com/agentgate/utils/SignatureUtils.java)

构建规范签名串，解析并排序/编码查询参数，计算 body SHA-256、HMAC-SHA256、十六进制和常量时间比较；客户端必须采用相同规则。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private SignatureUtils() {
    public static String sha256Hex(byte[] data) {
            return toHex(digest.digest(data));
            throw new IllegalStateException("SHA-256 not supported", e);
    public static String hmacSha256Hex(String secret, String data) {
            return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
            throw new IllegalStateException("HMAC-SHA256 failed", e);
    public static String canonicalString(String method,
    public static Map<String, String[]> parseQueryString(String queryString) {
    public static String buildQueryString(Map<String, String[]> params) {
    private static void append(StringBuilder sb, String key, String value) {
    private static String urlEncode(String value) {
            throw new IllegalStateException(e);
    private static String urlDecode(String value) {
            throw new IllegalStateException(e);
    public static boolean constantTimeEquals(String expected, String actual) {
    private static String toHex(byte[] bytes) {
        return new String(chars);
```

### 完整代码

```java
package com.agentgate.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SignatureUtils {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private SignatureUtils() {
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    public static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }

    /**
     * 规范字符串：
     * METHOD\nPATH\n排序后的Query\nTimestamp\nNonce\nSHA256(body)
     */
    public static String canonicalString(String method,
                                         String path,
                                         Map<String, String[]> params,
                                         String timestamp,
                                         String nonce,
                                         String bodyHash) {
        return method.toUpperCase() + "\n"
                + path + "\n"
                + buildQueryString(params) + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + bodyHash;
    }

    /**
     * 只解析 URL 中的查询串，不读取表单体。
     * 这样 QUERY 与 BODY 各司其职：表单字段由 body hash 覆盖，也避免请求体被缓存后
     * 再依赖容器解析参数导致的差异。
     */
    public static Map<String, String[]> parseQueryString(String queryString) {
        Map<String, List<String>> temp = new LinkedHashMap<>();
        if (queryString != null && !queryString.isEmpty()) {
            for (String pair : queryString.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int index = pair.indexOf('=');
                String key = index >= 0 ? pair.substring(0, index) : pair;
                String value = index >= 0 ? pair.substring(index + 1) : "";
                temp.computeIfAbsent(urlDecode(key), k -> new ArrayList<>()).add(urlDecode(value));
            }
        }
        Map<String, String[]> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : temp.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toArray(new String[0]));
        }
        return result;
    }

    public static String buildQueryString(Map<String, String[]> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            String[] values = params.get(key);
            if (values == null || values.length == 0) {
                append(sb, key, "");
                continue;
            }
            String[] sortedValues = values.clone();
            Arrays.sort(sortedValues);
            for (String value : sortedValues) {
                append(sb, key, value == null ? "" : value);
            }
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String key, String value) {
        if (sb.length() > 0) {
            sb.append('&');
        }
        sb.append(urlEncode(key)).append('=').append(urlEncode(value));
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            // 非法的百分号编码按原样参与签名，最终会因签名不匹配被拒绝
            return value;
        }
    }

    public static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            chars[i * 2] = HEX[b >>> 4];
            chars[i * 2 + 1] = HEX[b & 0x0F];
        }
        return new String(chars);
    }
}
```

## SsrfUtils.java

源码：[src/main/java/com/agentgate/utils/SsrfUtils.java](../../src/main/java/com/agentgate/utils/SsrfUtils.java)

验证 URL 协议、userinfo、主机解析地址并拒绝危险网络；允许 loopback 是演示开关；校验不等于固定实际连接 IP。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private SsrfUtils() {
    public static void validate(String url, boolean allowLoopback) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址为空");
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址格式非法");
            throw new BizException(ErrorCode.SSRF_BLOCKED, "仅允许 http/https 上游");
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址不允许包含用户信息");
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址缺少主机名");
                    throw new BizException(ErrorCode.SSRF_BLOCKED,
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游域名解析失败");
    static boolean isBlocked(InetAddress address, boolean allowLoopback) {
```

### 完整代码

```java
package com.agentgate.utils;

import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;

import java.net.InetAddress;
import java.net.URI;

public final class SsrfUtils {

    private SsrfUtils() {
    }

    public static void validate(String url, boolean allowLoopback) {
        if (url == null || url.trim().isEmpty()) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址为空");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址格式非法");
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "仅允许 http/https 上游");
        }
        if (uri.getUserInfo() != null) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址不允许包含用户信息");
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游地址缺少主机名");
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlocked(address, allowLoopback)) {
                    throw new BizException(ErrorCode.SSRF_BLOCKED,
                            "上游地址指向受限网络: " + address.getHostAddress());
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SSRF_BLOCKED, "上游域名解析失败");
        }
    }

    static boolean isBlocked(InetAddress address, boolean allowLoopback) {
        if (address.isLoopbackAddress()) {
            return !allowLoopback;
        }
        if (address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            // 0.0.0.0/8
            if (first == 0) {
                return true;
            }
            // 100.64.0.0/10 运营商级 NAT
            if (first == 100 && second >= 64 && second <= 127) {
                return true;
            }
            // 169.254.0.0/16 链路本地（含云元数据 169.254.169.254）
            return first == 169 && second == 254;
        }
        if (bytes.length == 16) {
            // fc00::/7 唯一本地地址
            return (bytes[0] & 0xFE) == 0xFC;
        }
        return false;
    }
}
```

## TraceIdUtils.java

源码：[src/main/java/com/agentgate/utils/TraceIdUtils.java](../../src/main/java/com/agentgate/utils/TraceIdUtils.java)

生成追踪 ID 并对 MDC 执行 put/clear；与 Filter、响应头、消息 traceId 配合定位请求。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    public static final String TRACE_ID = "traceId";
    private TraceIdUtils() {
    public static String generate() {
    public static void put(String traceId) {
    public static void clear() {
```

### 完整代码

```java
package com.agentgate.utils;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdUtils {

    public static final String TRACE_ID = "traceId";

    private TraceIdUtils() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void put(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}
```

## UserHolder.java

源码：[src/main/java/com/agentgate/utils/UserHolder.java](../../src/main/java/com/agentgate/utils/UserHolder.java)

静态 ThreadLocal 保存当前线程 UserDTO，提供取用户/ID 和 remove；请求结束必须清理，不能当作跨线程身份存储。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final ThreadLocal<UserDTO> USER_THREAD_LOCAL = new ThreadLocal<>();
    private UserHolder() {
    public static void saveUser(UserDTO user) {
    public static UserDTO getUser() {
    public static Long getUserId() {
    public static void removeUser() {
```

### 完整代码

```java
package com.agentgate.utils;

import com.agentgate.dto.UserDTO;

public final class UserHolder {

    private static final ThreadLocal<UserDTO> USER_THREAD_LOCAL = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void saveUser(UserDTO user) {
        USER_THREAD_LOCAL.set(user);
    }

    public static UserDTO getUser() {
        return USER_THREAD_LOCAL.get();
    }

    public static Long getUserId() {
        UserDTO user = USER_THREAD_LOCAL.get();
        return user == null ? null : user.getId();
    }

    public static void removeUser() {
        USER_THREAD_LOCAL.remove();
    }
}
```

