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
