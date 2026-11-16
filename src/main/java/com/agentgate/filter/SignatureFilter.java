package com.agentgate.filter;

import com.agentgate.dto.AppAuthInfo;
import com.agentgate.dto.CallLogMessage;
import com.agentgate.dto.Result;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.entity.Subscription;
import com.agentgate.enums.AppStatus;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.service.ApiService;
import com.agentgate.service.AppService;
import com.agentgate.service.CallLogService;
import com.agentgate.service.QuotaPlanService;
import com.agentgate.service.RateLimitService;
import com.agentgate.service.SubscriptionService;
import com.agentgate.utils.CachedBodyHttpServletRequest;
import com.agentgate.utils.IpUtils;
import com.agentgate.utils.RedisConstants;
import com.agentgate.utils.SignatureUtils;
import com.agentgate.utils.TraceIdUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 开放接口鉴权过滤器：签名、时间戳、Nonce、应用状态、IP 白名单、API/订阅、限流配额。
 * 同时统一记录调用日志（成功与失败都记）。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class SignatureFilter extends OncePerRequestFilter {

    public static final String HEADER_APP_KEY = "X-App-Key";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";

    public static final String ATTR_APP_ID = "agentgate.appId";
    public static final String ATTR_API = "agentgate.api";
    public static final String ATTR_TRACE_ID = "agentgate.traceId";
    public static final String ATTR_CLIENT_IP = "agentgate.clientIp";

    private final AppService appService;
    private final ApiService apiService;
    private final SubscriptionService subscriptionService;
    private final QuotaPlanService quotaPlanService;
    private final RateLimitService rateLimitService;
    private final CallLogService callLogService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agentgate.openapi.max-body-bytes:1048576}")
    private int maxBodyBytes;

    @Value("${agentgate.openapi.timestamp-window-seconds:300}")
    private long timestampWindowSeconds;

    @Value("${agentgate.openapi.nonce-ttl-minutes:5}")
    private long nonceTtlMinutes;

    @Value("${agentgate.security.trust-proxy:false}")
    private boolean trustProxy;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String openApiPrefix = request.getContextPath() + "/openapi/";
        if (!uri.startsWith(openApiPrefix)) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        String traceId = (String) request.getAttribute(TraceIdUtils.TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = TraceIdUtils.generate();
            responseWrapper.setHeader(TraceIdFilter.TRACE_ID_HEADER, traceId);
        }
        String clientIp = IpUtils.getClientIp(request, trustProxy);

        Long appId = null;
        Long apiId = null;
        Integer errorCode = null;
        int statusCode = HttpServletResponse.SC_OK;
        int requestSize = 0;

        try {
            CachedBodyHttpServletRequest cachedRequest =
                    new CachedBodyHttpServletRequest(request, maxBodyBytes);
            if (cachedRequest.isTooLarge()) {
                throw new BizException(ErrorCode.BODY_TOO_LARGE);
            }
            requestSize = cachedRequest.getCachedBody().length;

            String appKey = requireHeader(request, HEADER_APP_KEY);
            String timestamp = requireHeader(request, HEADER_TIMESTAMP);
            String nonce = requireHeader(request, HEADER_NONCE);
            String signature = requireHeader(request, HEADER_SIGNATURE);

            long ts;
            try {
                ts = Long.parseLong(timestamp);
            } catch (NumberFormatException e) {
                throw new BizException(ErrorCode.TIMESTAMP_EXPIRED, "时间戳格式非法");
            }
            long requestTimeMs = ts * 1000L;
            if (Math.abs(System.currentTimeMillis() - requestTimeMs) > timestampWindowSeconds * 1000L) {
                throw new BizException(ErrorCode.TIMESTAMP_EXPIRED);
            }

            AppAuthInfo app = appService.getAuthInfo(appKey);
            if (app == null) {
                throw new BizException(ErrorCode.SIGNATURE_INVALID);
            }
            appId = app.getId();
            if (!AppStatus.ACTIVE.name().equals(app.getStatus())) {
                throw new BizException(ErrorCode.APP_DISABLED);
            }
            if (!IpUtils.isAllowed(clientIp, app.getIpWhitelist())) {
                throw new BizException(ErrorCode.IP_NOT_ALLOWED);
            }

            String path = uri.substring(request.getContextPath().length());
            String bodyHash = SignatureUtils.sha256Hex(cachedRequest.getCachedBody());
            String canonical = SignatureUtils.canonicalString(
                    request.getMethod(), path,
                    SignatureUtils.parseQueryString(request.getQueryString()),
                    timestamp, nonce, bodyHash);
            String expected = SignatureUtils.hmacSha256Hex(app.getAppSecret(), canonical);
            if (!SignatureUtils.constantTimeEquals(
                    expected, signature.trim().toLowerCase(java.util.Locale.ROOT))) {
                throw new BizException(ErrorCode.SIGNATURE_INVALID);
            }

            // 签名通过后再消费 nonce，避免无效签名占用 Redis 空间
            String nonceKey = RedisConstants.NONCE_KEY + appKey + ":" + nonce;
            Boolean firstUse = redisTemplate.opsForValue()
                    .setIfAbsent(nonceKey, "1", nonceTtlMinutes, TimeUnit.MINUTES);
            if (!Boolean.TRUE.equals(firstUse)) {
                throw new BizException(ErrorCode.NONCE_REPLAYED);
            }

            String apiCode = resolveApiCode(uri, request.getContextPath());
            ApiDefinition api = apiService.getPublishedByCode(apiCode);
            apiId = api.getId();

            Subscription subscription = subscriptionService.getActive(appId, apiId);
            if (subscription == null) {
                throw new BizException(ErrorCode.NOT_SUBSCRIBED);
            }
            QuotaPlan plan = quotaPlanService.getById(subscription.getQuotaPlanId());
            rateLimitService.checkAndConsume(appId, api, plan);

            request.setAttribute(ATTR_APP_ID, appId);
            request.setAttribute(ATTR_API, api);
            request.setAttribute(ATTR_TRACE_ID, traceId);
            request.setAttribute(ATTR_CLIENT_IP, clientIp);

            filterChain.doFilter(cachedRequest, responseWrapper);
            statusCode = responseWrapper.getStatus();
            if (statusCode >= 400) {
                errorCode = extractErrorCode(responseWrapper);
            }
        } catch (BizException e) {
            errorCode = e.getCode();
            statusCode = resolveStatus(e.getCode());
            writeError(responseWrapper, statusCode, Result.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("开放接口未处理异常 traceId={}", traceId, e);
            errorCode = ErrorCode.SYSTEM_ERROR.getCode();
            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            writeError(responseWrapper, statusCode, Result.fail(ErrorCode.SYSTEM_ERROR));
        } finally {
            int responseSize = responseWrapper.getContentSize();
            responseWrapper.copyBodyToResponse();
            CallLogMessage message = new CallLogMessage();
            message.setTraceId(traceId);
            message.setAppId(appId);
            message.setApiId(apiId);
            message.setMethod(request.getMethod());
            message.setPath(uri);
            message.setStatusCode(statusCode);
            message.setLatencyMs(System.currentTimeMillis() - start);
            message.setClientIp(clientIp);
            message.setErrorCode(errorCode);
            message.setRequestSize(requestSize);
            message.setResponseSize(responseSize);
            callLogService.asyncSave(message);
        }
    }

    /**
     * Controller 层抛出的 BizException 由全局异常处理器转成 Result，这里从响应体补回错误码。
     */
    private Integer extractErrorCode(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(content);
            if (node.has("success") && node.has("code")) {
                return node.get("code").asInt();
            }
        } catch (Exception ignored) {
            // 上游原始响应，不做解析
        }
        return null;
    }

    private String requireHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.MISSING_SIGNATURE_HEADER, "缺少请求头 " + name);
        }
        return value;
    }

    private String resolveApiCode(String uri, String contextPath) {
        String path = uri.substring(contextPath.length());
        String prefix = "/openapi/";
        if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
            throw new BizException(ErrorCode.NOT_FOUND, "缺少 API 编码");
        }
        String rest = path.substring(prefix.length());
        int slash = rest.indexOf('/');
        return slash > 0 ? rest.substring(0, slash) : rest;
    }

    private void writeError(HttpServletResponse response, int status, Result<?> result) {
        if (response.isCommitted()) {
            return;
        }
        try {
            response.resetBuffer();
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(result));
        } catch (IOException e) {
            log.error("开放接口错误响应写入失败", e);
        }
    }

    private int resolveStatus(int code) {
        switch (code) {
            case 401:
                return HttpStatus.UNAUTHORIZED.value();
            case 403:
                return HttpStatus.FORBIDDEN.value();
            case 404:
                return HttpStatus.NOT_FOUND.value();
            case 405:
                return HttpStatus.METHOD_NOT_ALLOWED.value();
            case 413:
                return HttpStatus.PAYLOAD_TOO_LARGE.value();
            case 429:
            case 4290:
                return HttpStatus.TOO_MANY_REQUESTS.value();
            case 2001:
                return HttpStatus.GATEWAY_TIMEOUT.value();
            case 2002:
                return HttpStatus.BAD_GATEWAY.value();
            case 500:
                return HttpStatus.INTERNAL_SERVER_ERROR.value();
            // 开放接口鉴权类错误统一 401/403
            case 1001:
            case 1002:
            case 1003:
            case 1004:
                return HttpStatus.UNAUTHORIZED.value();
            case 1005:
            case 1006:
            case 1007:
            case 1008:
            case 1009:
                return HttpStatus.FORBIDDEN.value();
            default:
                return HttpStatus.BAD_REQUEST.value();
        }
    }
}
