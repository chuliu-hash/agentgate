package com.agentgate.service.impl;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.service.OpenApiProxyService;
import com.agentgate.service.ProxyResponse;
import com.agentgate.utils.CachedBodyHttpServletRequest;
import com.agentgate.utils.MaskUtils;
import com.agentgate.utils.SsrfUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OpenApiProxyServiceImpl implements OpenApiProxyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agentgate.ssrf.allow-loopback:false}")
    private boolean allowLoopback;

    @Value("${agentgate.openapi.max-body-bytes:1048576}")
    private int maxBodyBytes;

    @Override
    public ProxyResponse forward(HttpServletRequest request, ApiDefinition api, String traceId, String clientIp) {
        String method = request.getMethod();
        if (StringUtils.hasText(api.getMethod())
                && !"ANY".equalsIgnoreCase(api.getMethod())
                && !api.getMethod().equalsIgnoreCase(method)) {
            throw new BizException(ErrorCode.METHOD_NOT_ALLOWED,
                    "该 API 仅支持 " + api.getMethod());
        }
        // 每次转发前重新校验，降低 DNS 重绑定绕过风险
        SsrfUtils.validate(api.getUpstreamUrl(), allowLoopback);

        String targetUrl = buildTargetUrl(request, api);
        HttpMethod httpMethod = HttpMethod.resolve(method);
        if (httpMethod == null) {
            throw new BizException(ErrorCode.METHOD_NOT_ALLOWED);
        }

        HttpHeaders headers = new HttpHeaders();
        copyHeader(request, headers, HttpHeaders.CONTENT_TYPE);
        copyHeader(request, headers, HttpHeaders.ACCEPT);
        copyHeader(request, headers, HttpHeaders.USER_AGENT);
        headers.set("X-Trace-Id", traceId);

        byte[] body = readBody(request);
        HttpEntity<byte[]> entity = new HttpEntity<>(body.length == 0 ? null : body, headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(targetUrl, httpMethod, entity, byte[].class);
        byte[] responseBody = response.getBody() == null ? new byte[0] : response.getBody();
        String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

        if (api.getSensitivityLevel() != null && api.getSensitivityLevel() >= 2
                && contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) {
            Charset charset = resolveCharset(contentType);
            String json = new String(responseBody, charset);
            responseBody = MaskUtils.maskJson(json, objectMapper).getBytes(charset);
        }
        if (responseBody.length > maxBodyBytes) {
            responseBody = Arrays.copyOf(responseBody, maxBodyBytes);
        }

        ProxyResponse proxyResponse = new ProxyResponse();
        proxyResponse.setStatusCode(response.getStatusCodeValue());
        proxyResponse.setContentType(contentType);
        proxyResponse.setBody(responseBody);
        return proxyResponse;
    }

    private String buildTargetUrl(HttpServletRequest request, ApiDefinition api) {
        String upstream = api.getUpstreamUrl().trim();
        String prefix = request.getContextPath() + "/openapi/" + api.getCode();
        String requestUri = request.getRequestURI();
        String tail = requestUri.startsWith(prefix) ? requestUri.substring(prefix.length()) : "";
        if (StringUtils.hasText(tail)) {
            if (upstream.endsWith("/") && tail.startsWith("/")) {
                tail = tail.substring(1);
            } else if (!upstream.endsWith("/") && !tail.startsWith("/")) {
                tail = "/" + tail;
            }
            upstream = upstream + tail;
        }
        String query = request.getQueryString();
        if (StringUtils.hasText(query)) {
            upstream = upstream + (upstream.contains("?") ? "&" : "?") + query;
        }
        return upstream;
    }

    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (StringUtils.hasText(value)) {
            headers.set(name, value);
        }
    }

    private byte[] readBody(HttpServletRequest request) {
        if (request instanceof CachedBodyHttpServletRequest) {
            return ((CachedBodyHttpServletRequest) request).getCachedBody();
        }
        try {
            return StreamUtils.copyToByteArray(request.getInputStream());
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "读取请求体失败");
        }
    }

    private Charset resolveCharset(String contentType) {
        try {
            Charset charset = MediaType.parseMediaType(contentType).getCharset();
            return charset == null ? StandardCharsets.UTF_8 : charset;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
}
