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
/**
 * 教学说明：递归遍历 JSON 对象与数组，按字段名匹配并掩码电话、证件、邮箱、地址、银行卡及密钥字段；解析失败原样返回。
 * 文件路径：src/main/java/com/agentgate/utils/MaskUtils.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public final class MaskUtils {

    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "phone", "mobile", "telephone", "idcard", "idcardno", "idno",
            "email", "address", "bankcard", "cardno", "password", "secret",
            "token", "accesskey", "secretkey"));

    /**
     * 私有构造器禁止直接实例化，仅通过静态方法使用此工具类。
     */
    private MaskUtils() {
    }

    /**
     * 解析 JSON 并递归掩码已配置敏感字段；解析失败返回原内容，因此不是严格的防泄漏边界。
     */
    public static String maskJson(String json, ObjectMapper objectMapper) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        try {
            // 将 JSON 解析为树以便按字段遍历；解析失败如何处理由当前 catch 分支决定。
            // 转成 JSON 树后递归处理对象和数组中的敏感字段。
            JsonNode root = objectMapper.readTree(json);
            maskNode(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            // 非 JSON 或解析失败时原样返回，避免影响业务响应
            return json;
        }
    }

    /**
     * 递归遍历对象与数组，对命中敏感字段名的标量值执行掩码。
     */
    private static void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        // 对象节点按字段名判断是否敏感，并递归处理嵌套结构。
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

    /**
     * 依据字段种类选择保留前后缀或全部替换，未分类敏感字段默认全掩码。
     */
    private static String maskValue(String field, String value) {
        // 空值无需掩码；保持调用方传入的 JSON 值语义。
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

    /**
     * 保留指定长度的前后缀，中间替换为星号；原值太短时全部掩码。
     */
    private static String maskMiddle(String value, int prefix, int suffix) {
        if (value.length() <= prefix + suffix) {
            return "******";
        }
        return value.substring(0, prefix)
                + "****"
                + value.substring(value.length() - suffix);
    }
}
