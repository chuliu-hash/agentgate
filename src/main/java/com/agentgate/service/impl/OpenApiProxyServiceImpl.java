package com.agentgate.service.impl;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.service.OpenApiProxyService;
import com.agentgate.dto.ProxyResponse;
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
/**
 * 教学说明：校验方法和上游地址，拼接路径后缀/query，复制有限请求头，读取缓存 body，同步转发；按敏感级别脱敏 JSON，再按上限截断响应。
 * 文件路径：src/main/java/com/agentgate/service/impl/OpenApiProxyServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class OpenApiProxyServiceImpl implements OpenApiProxyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agentgate.ssrf.allow-loopback:false}")
    private boolean allowLoopback;

    @Value("${agentgate.openapi.max-body-bytes:1048576}")
    private int maxBodyBytes;

    /**
     * 转发开放 API 请求并返回上游状态和响应体；身份、订阅及额度校验由前置签名过滤器完成。
     */
    @Override
    public ProxyResponse forward(HttpServletRequest request, ApiDefinition api, String traceId) {
        String method = request.getMethod();
        // 校验条件成立时拒绝本次操作，抛出 METHOD_NOT_ALLOWED；不再继续后续正常业务步骤。
        if (StringUtils.hasText(api.getMethod())
                && !"ANY".equalsIgnoreCase(api.getMethod())
                && !api.getMethod().equalsIgnoreCase(method)) {
            throw new BizException(ErrorCode.METHOD_NOT_ALLOWED,
                    "该 API 仅支持 " + api.getMethod());
        }
        // 每次转发前重新校验，降低 DNS 重绑定绕过风险
        SsrfUtils.validate(api.getUpstreamUrl(), allowLoopback);

        // 将登记的上游 URL 与请求资源后缀及查询串拼接，不包含 Nginx 的 /api 前缀。
        String targetUrl = buildTargetUrl(request, api);
        HttpMethod httpMethod = HttpMethod.resolve(method);
        // 校验条件成立时拒绝本次操作，抛出 METHOD_NOT_ALLOWED；不再继续后续正常业务步骤。
        if (httpMethod == null) {
            throw new BizException(ErrorCode.METHOD_NOT_ALLOWED);
        }

        // 只转发内容协商相关请求头和 User-Agent，使用网关的 traceId 关联上游日志。
        HttpHeaders headers = new HttpHeaders();
        copyHeader(request, headers, HttpHeaders.CONTENT_TYPE);
        copyHeader(request, headers, HttpHeaders.ACCEPT);
        copyHeader(request, headers, HttpHeaders.USER_AGENT);
        headers.set("X-Trace-Id", traceId);

        byte[] body = readBody(request);
        // 把选定请求头和原始请求字节组成上游请求，不透传所有客户端头。
        HttpEntity<byte[]> entity = new HttpEntity<>(body.length == 0 ? null : body, headers);

        // 教学说明：这是同步 HTTP 调用：当前请求线程等待上游响应，超时参数由 RestTemplateConfig 统一设置。
        ResponseEntity<byte[]> response = restTemplate.exchange(targetUrl, httpMethod, entity, byte[].class);
        // 上游无正文时统一使用空字节数组，方便后续处理和返回。
        byte[] responseBody = response.getBody() == null ? new byte[0] : response.getBody();
        String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

        // 只有敏感级别达阈值且声明为 JSON 的响应才进入字段掩码流程。
        if (api.getSensitivityLevel() != null && api.getSensitivityLevel() >= 2
                && contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) {
            Charset charset = resolveCharset(contentType);
            String json = new String(responseBody, charset);
            responseBody = MaskUtils.maskJson(json, objectMapper).getBytes(charset);
        }

        if (responseBody.length > maxBodyBytes) {
            // 当前实现截断已读取的响应字节；可能截断 JSON，也不等于流式内存限制。
            responseBody = Arrays.copyOf(responseBody, maxBodyBytes);
        }

        // 将上游状态、内容类型和处理后的正文交给 Controller 构建客户端响应。
        ProxyResponse proxyResponse = new ProxyResponse();
        proxyResponse.setStatusCode(response.getStatusCodeValue());
        proxyResponse.setContentType(contentType);
        proxyResponse.setBody(responseBody);
        return proxyResponse;
    }

    /**
     * 将开放路径的资源后缀及原始查询串追加到登记上游地址，不把 /openapi/API编码 当作上游路径。
     */
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

    /**
     * 仅复制指定的单个请求头；避免向上游无差别传递客户端身份和内部控制头。
     */
    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (StringUtils.hasText(value)) {
            headers.set(name, value);
        }
    }

    /**
     * 优先读取已缓存请求体，否则读取 Servlet 输入流；读取失败转为参数异常。
     */
    private byte[] readBody(HttpServletRequest request) {
        // 复用过滤器缓存的原始字节，使上游收到的请求体与验签使用的内容一致。
        if (request instanceof CachedBodyHttpServletRequest) {
            return ((CachedBodyHttpServletRequest) request).getCachedBody();
        }
        try {
            return StreamUtils.copyToByteArray(request.getInputStream());
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "读取请求体失败");
        }
    }

    /**
     * 解析 Content-Type 中的字符集；缺失或无法解析时回退 UTF-8。
     */
    private Charset resolveCharset(String contentType) {
        try {
            Charset charset = MediaType.parseMediaType(contentType).getCharset();
            return charset == null ? StandardCharsets.UTF_8 : charset;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
}
