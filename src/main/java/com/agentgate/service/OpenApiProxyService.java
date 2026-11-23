package com.agentgate.service;

import com.agentgate.dto.ProxyResponse;
import com.agentgate.entity.ApiDefinition;

import javax.servlet.http.HttpServletRequest;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/OpenApiProxyService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface OpenApiProxyService {

    /**
     * 转发开放 API 请求并返回上游状态和响应体；身份、订阅及额度校验由前置签名过滤器完成。
     */
    ProxyResponse forward(HttpServletRequest request, ApiDefinition api, String traceId);
}
