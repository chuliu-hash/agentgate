package com.agentgate.dto;

import lombok.Data;

import java.io.Serializable;

@Data
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
