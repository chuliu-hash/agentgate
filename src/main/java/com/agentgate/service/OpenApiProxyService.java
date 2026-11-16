package com.agentgate.service;

import com.agentgate.entity.ApiDefinition;

import javax.servlet.http.HttpServletRequest;

public interface OpenApiProxyService {

    ProxyResponse forward(HttpServletRequest request, ApiDefinition api, String traceId, String clientIp);
}
