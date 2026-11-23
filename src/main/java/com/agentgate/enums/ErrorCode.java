package com.agentgate.enums;

import lombok.Getter;

@Getter
/**
 * 教学说明：成功、通用 HTTP 风格、签名、上游与业务错误编号及中文信息；异常处理代码另行映射 HTTP 状态。
 * 文件路径：src/main/java/com/agentgate/enums/ErrorCode.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    BODY_TOO_LARGE(413, "请求体过大"),
    RATE_LIMITED(429, "请求过于频繁"),
    QUOTA_EXCEEDED(4290, "调用配额已用尽"),
    SYSTEM_ERROR(500, "系统内部错误"),

    // 开放接口鉴权
    MISSING_SIGNATURE_HEADER(1001, "缺少签名请求头"),
    TIMESTAMP_EXPIRED(1002, "请求时间戳已过期"),
    NONCE_REPLAYED(1003, "重复的 nonce，拒绝重放"),
    SIGNATURE_INVALID(1004, "签名校验失败"),
    APP_DISABLED(1005, "应用已禁用"),
    IP_NOT_ALLOWED(1006, "客户端 IP 不在白名单"),
    API_OFFLINE(1007, "API 未上架或已下线"),
    NOT_SUBSCRIBED(1008, "应用未订阅该 API"),
    SSRF_BLOCKED(1009, "上游地址不安全"),

    // 上游
    UPSTREAM_TIMEOUT(2001, "上游服务超时"),
    UPSTREAM_ERROR(2002, "上游服务不可用"),

    // 业务
    USERNAME_EXISTS(3001, "用户名已存在"),
    USER_NOT_FOUND(3002, "用户不存在"),
    PASSWORD_ERROR(3003, "用户名或密码错误"),
    APP_NOT_FOUND(3004, "应用不存在"),
    API_NOT_FOUND(3005, "API 不存在"),
    API_CODE_EXISTS(3006, "API 编码已存在"),
    SUBSCRIPTION_EXISTS(3007, "订阅已存在或正在审批中"),
    SUBSCRIPTION_NOT_FOUND(3008, "订阅不存在"),
    PLAN_NOT_FOUND(3009, "配额套餐不存在"),
    STATE_TRANSITION_INVALID(3010, "状态流转非法"),
    APP_ACCESS_DENIED(3011, "无权操作该应用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
