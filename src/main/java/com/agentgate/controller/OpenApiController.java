package com.agentgate.controller;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.filter.SignatureFilter;
import com.agentgate.service.OpenApiProxyService;
import com.agentgate.service.ProxyResponse;
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
 * 开放接口入口。鉴权、限流、订阅校验已经在 SignatureFilter 完成，这里只负责转发。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OpenApiController {

    private final OpenApiProxyService openApiProxyService;

    @RequestMapping("/openapi/**")
    public ResponseEntity<byte[]> forward(HttpServletRequest request) {
        ApiDefinition api = (ApiDefinition) request.getAttribute(SignatureFilter.ATTR_API);
        String traceId = (String) request.getAttribute(SignatureFilter.ATTR_TRACE_ID);
        String clientIp = (String) request.getAttribute(SignatureFilter.ATTR_CLIENT_IP);

        ProxyResponse proxyResponse;
        try {
            proxyResponse = openApiProxyService.forward(request, api, traceId, clientIp);
        } catch (ResourceAccessException e) {
            log.warn("上游访问失败 traceId={} api={} error={}", traceId, api.getCode(), e.toString());
            boolean timeout = e.getCause() instanceof SocketTimeoutException;
            throw new BizException(timeout ? ErrorCode.UPSTREAM_TIMEOUT : ErrorCode.UPSTREAM_ERROR);
        } catch (RestClientException e) {
            log.warn("上游返回异常 traceId={} api={} error={}", traceId, api.getCode(), e.toString());
            throw new BizException(ErrorCode.UPSTREAM_ERROR);
        }

        HttpHeaders headers = new HttpHeaders();
        if (proxyResponse.getContentType() != null) {
            headers.set(HttpHeaders.CONTENT_TYPE, proxyResponse.getContentType());
        }
        return ResponseEntity.status(proxyResponse.getStatusCode())
                .headers(headers)
                .body(proxyResponse.getBody());
    }
}
