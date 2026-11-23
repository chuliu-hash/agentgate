package com.agentgate.dto;

import lombok.Data;

@Data
/**
 * 教学说明：代理结果载体：保存上游 HTTP 状态、Content-Type、原始响应字节；用于 Service 与 Controller 传递，不套管理接口 Result。
 * 文件路径：src/main/java/com/agentgate/dto/ProxyResponse.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class ProxyResponse {

    private int statusCode;
    private String contentType;
    private byte[] body;
}
