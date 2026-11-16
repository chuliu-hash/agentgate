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
