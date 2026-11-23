package com.agentgate.dto;

import lombok.Data;

import java.io.Serializable;

@Data
/**
 * 教学说明：从请求 Filter 发给 MQ 的调用快照：traceId、appId/apiId、方法路径、状态耗时、IP、错误码和大小；不是请求/响应正文。
 * 文件路径：src/main/java/com/agentgate/dto/CallLogMessage.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class CallLogMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;
    private Long appId;
    private Long apiId;
    private String method;
    private String path;
    private Integer statusCode;
    private Long latencyMs;
    private String clientIp;
    private Integer errorCode;
    private Integer requestSize;
    private Integer responseSize;
}
