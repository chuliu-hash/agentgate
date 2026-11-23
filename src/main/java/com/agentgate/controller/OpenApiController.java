package com.agentgate.controller;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.filter.SignatureFilter;
import com.agentgate.service.OpenApiProxyService;
import com.agentgate.dto.ProxyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.servlet.http.HttpServletRequest;
import java.net.SocketTimeoutException;

/**
 * 开放接口入口。签名过滤器完成应用鉴权、订阅校验和额度扣减后，由此调用代理服务。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OpenApiController {

    private final OpenApiProxyService openApiProxyService;

    /**
     * 使用过滤器提供的调用上下文转发请求，再将代理结果转换为客户端 HTTP 响应。
     */
    @RequestMapping("/openapi/**")
    public ResponseEntity<byte[]> forward(HttpServletRequest request) {
        // 从同一次 Servlet 请求的属性读取过滤器写入的 API 定义和追踪 ID。
        ApiDefinition api = (ApiDefinition) request.getAttribute(SignatureFilter.ATTR_API);
        String traceId = (String) request.getAttribute(SignatureFilter.ATTR_TRACE_ID);

        ProxyResponse proxyResponse;
        try {
            proxyResponse = openApiProxyService.forward(request, api, traceId);
        } catch (ResourceAccessException e) {
            log.warn("上游访问失败 traceId={} api={} error={}", traceId, api.getCode(), e.toString());
            // 区分超时和其他访问失败，交给全局异常处理器映射为 504 或 502。
            boolean timeout = e.getCause() instanceof SocketTimeoutException;
            throw new BizException(timeout ? ErrorCode.UPSTREAM_TIMEOUT : ErrorCode.UPSTREAM_ERROR);
        } catch (RestClientException e) {
            // 其他 HTTP 客户端异常统一转换为上游访问错误。
            log.warn("上游返回异常 traceId={} api={} error={}", traceId, api.getCode(), e.toString());
            throw new BizException(ErrorCode.UPSTREAM_ERROR);
        }

        // 保留上游状态和内容类型，直接返回代理服务处理后的字节。
        HttpHeaders headers = new HttpHeaders();
        if (proxyResponse.getContentType() != null) {
            headers.set(HttpHeaders.CONTENT_TYPE, proxyResponse.getContentType());
        }
        return ResponseEntity.status(proxyResponse.getStatusCode())
                .headers(headers)
                .body(proxyResponse.getBody());
    }
}
