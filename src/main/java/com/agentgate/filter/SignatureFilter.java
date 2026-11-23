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
import com.agentgate.service.CallLogDispatchService;
import com.agentgate.service.QuotaPlanService;
import com.agentgate.service.RateLimitService;
import com.agentgate.service.SubscriptionService;
import com.agentgate.utils.CachedBodyHttpServletRequest;
import com.agentgate.utils.IpUtils;
import com.agentgate.utils.RedisConstants;
import com.agentgate.utils.SignatureUtils;
import com.agentgate.utils.TraceIdUtils;
import com.agentgate.utils.HttpStatusResolver;
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
/**
 * 教学说明：开放请求总入口：验证请求体大小、四个签名头、时间窗口、应用状态、IP、HMAC、Nonce、API 与订阅，扣减额度，写入请求属性；finally 收集成功和失败调用日志。辅助方法解析 API code、构造错误响应与状态映射。
 * 文件路径：src/main/java/com/agentgate/filter/SignatureFilter.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class SignatureFilter extends OncePerRequestFilter {

    /** 开放调用接口的统一路径前缀，过滤和 API 编码解析必须使用同一规则。 */
    public static final String OPEN_API_PREFIX = "/openapi/";

    public static final String HEADER_APP_KEY = "X-App-Key";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";

    public static final String ATTR_APP_ID = "agentgate.appId";
    public static final String ATTR_API = "agentgate.api";
    public static final String ATTR_TRACE_ID = "agentgate.traceId";

    private final AppService appService;
    private final ApiService apiService;
    private final SubscriptionService subscriptionService;
    private final QuotaPlanService quotaPlanService;
    private final RateLimitService rateLimitService;
    private final CallLogDispatchService callLogDispatchService;
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

    /**
     * 处理当前请求的过滤流程；主动调用 filterChain 才继续执行，异常与上下文清理由过滤器负责。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String openApiPrefix = request.getContextPath() + OPEN_API_PREFIX;

        //  URI 是否以 contextPath + + OPEN_API_PREFIX 开头
        if (!uri.startsWith(openApiPrefix)) {
            filterChain.doFilter(request, response);
            return;  //  必须retrun，不然会走下面的逻辑
        }

        long start = System.currentTimeMillis();
        // 包装响应以缓存状态码和响应体，便于记录大小并从下游 Result 中提取业务错误码。
        // 过滤流程结束时必须调用 copyBodyToResponse()，否则缓存内容不会真正发送给客户端。
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        String traceId = (String) request.getAttribute(TraceIdUtils.TRACE_ID);
        // 再次检查traceId
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
            // 缓存原始请求体，保证验签读取后 Controller 仍可再次读取同一字节内容。
            CachedBodyHttpServletRequest cachedRequest =
                    new CachedBodyHttpServletRequest(request, maxBodyBytes);
            // 校验条件成立时拒绝本次操作，抛出 BODY_TOO_LARGE；不再继续后续正常业务步骤。
            // 超过上限时拒绝请求，避免大请求占用过多内存。
            if (cachedRequest.isTooLarge()) {
                throw new BizException(ErrorCode.BODY_TOO_LARGE);
            }
            // 记录请求体大小(字节数)
            requestSize = cachedRequest.getCachedBody().length;

            // 生成应用标识 AK，用于开放调用时定位应用；它不替代 SK 的身份凭证作用。
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
            // 将客户端秒级时间戳转换为毫秒，与服务器当前时间统一比较。
            long requestTimeMs = ts * 1000L;
            // 校验条件成立时拒绝本次操作，抛出 TIMESTAMP_EXPIRED；不再继续后续正常业务步骤。
            if (Math.abs(System.currentTimeMillis() - requestTimeMs) > timestampWindowSeconds * 1000L) {
                throw new BizException(ErrorCode.TIMESTAMP_EXPIRED);
            }

            // 根据 AppKey 读取应用及解密后的 Secret，后续用于身份和签名校验。
            AppAuthInfo app = appService.getAuthInfo(appKey);
            // 校验条件成立时拒绝本次操作，抛出 SIGNATURE_INVALID；不再继续后续正常业务步骤。
            if (app == null) {
                throw new BizException(ErrorCode.SIGNATURE_INVALID);
            }
            appId = app.getId();
            // 校验条件成立时拒绝本次操作，抛出 APP_DISABLED；不再继续后续正常业务步骤。
            if (!AppStatus.ACTIVE.name().equals(app.getStatus())) {
                throw new BizException(ErrorCode.APP_DISABLED);
            }
            // 校验条件成立时拒绝本次操作，抛出 IP_NOT_ALLOWED；不再继续后续正常业务步骤。
            if (!IpUtils.isAllowed(clientIp, app.getIpWhitelist())) {
                throw new BizException(ErrorCode.IP_NOT_ALLOWED);
            }

            String path = uri.substring(request.getContextPath().length());
            // 对实际缓存字节做摘要，保证签名覆盖请求体，不能用重新序列化后的 JSON 替代。
            // 对原始请求体计算摘要，防止请求内容在验签后被替换。
            String bodyHash = SignatureUtils.sha256Hex(cachedRequest.getCachedBody());
            String canonical = SignatureUtils.canonicalString(
                    request.getMethod(), path,
                    SignatureUtils.parseQueryString(request.getQueryString()),
                    timestamp, nonce, bodyHash);
            // 使用服务端取回的 SK 计算预期签名，再与客户端提供的签名比较。
            String expected = SignatureUtils.hmacSha256Hex(app.getAppSecret(), canonical);
            // 校验条件成立时拒绝本次操作，抛出 SIGNATURE_INVALID；不再继续后续正常业务步骤。
            if (!SignatureUtils.constantTimeEquals(
                    expected, signature.trim().toLowerCase(java.util.Locale.ROOT))) {
                throw new BizException(ErrorCode.SIGNATURE_INVALID);
            }

            // 签名通过后再消费 nonce，避免无效签名占用 Redis 空间
            String nonceKey = RedisConstants.NONCE_KEY + appKey + ":" + nonce;
            // 教学说明：SETNX 与过期时间一次提交，避免先占位后设 TTL 中途失败；仅相同应用内的重复 Nonce 会被拒绝。
            Boolean firstUse = redisTemplate.opsForValue()
                    .setIfAbsent(nonceKey, "1", nonceTtlMinutes, TimeUnit.MINUTES);
            // 校验条件成立时拒绝本次操作，抛出 NONCE_REPLAYED；不再继续后续正常业务步骤。
            if (!Boolean.TRUE.equals(firstUse)) {
                throw new BizException(ErrorCode.NONCE_REPLAYED);
            }

            // 取开放路径第一段作为 API 编码，后续资源路径由代理转发。
            // 从路径提取 API 编码，确保调用目标与已发布 API 定义绑定。
            String apiCode = resolveApiCode(uri, request.getContextPath());
            ApiDefinition api = apiService.getPublishedByCode(apiCode);
            apiId = api.getId();

            Subscription subscription = subscriptionService.getActive(appId, apiId);
            // 校验条件成立时拒绝本次操作，抛出 NOT_SUBSCRIBED；不再继续后续正常业务步骤。
            if (subscription == null) {
                throw new BizException(ErrorCode.NOT_SUBSCRIBED);
            }
            // 根据有效订阅中的套餐 ID 读取限制，不能让调用者自行指定本次额度。
            QuotaPlan plan = quotaPlanService.getById(subscription.getQuotaPlanId());
            // 鉴权与订阅通过后，单次 Lua 原子扣减 QPS 与日/月配额；上游失败不会自动退还已扣额度。
            rateLimitService.checkAndConsume(appId, api, plan);

            // 把已验证的业务上下文传给 Controller，避免下游重复查询或信任客户端自报身份。
            request.setAttribute(ATTR_APP_ID, appId);
            request.setAttribute(ATTR_API, api);
            request.setAttribute(ATTR_TRACE_ID, traceId);

            // 向 MVC 传递可重复读取的请求和缓存响应；下游处理结束后会返回这里继续记录结果。
            filterChain.doFilter(cachedRequest, responseWrapper);

            statusCode = responseWrapper.getStatus();
            // 下游已返回错误 HTTP 状态，尝试从 Result 响应补充业务错误码。
            if (statusCode >= 400) {
                errorCode = extractErrorCode(responseWrapper);
            }
        } catch (BizException e) {
            errorCode = e.getCode();
            statusCode = HttpStatusResolver.resolve(e.getCode()).value();
            writeError(responseWrapper, statusCode, Result.fail(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("开放接口未处理异常 traceId={}", traceId, e);
            errorCode = ErrorCode.SYSTEM_ERROR.getCode();
            statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            writeError(responseWrapper, statusCode, Result.fail(ErrorCode.SYSTEM_ERROR));
        } finally {

            // 在复制响应前读取缓冲大小，记录实际返回给调用方的字节数。
            int responseSize = responseWrapper.getContentSize();

            // 把包装器缓存的字节复制到真实响应，否则客户端可能收不到响应体。
            responseWrapper.copyBodyToResponse();

            // 构造调用快照；早期校验失败时应用或 API 尚未识别，ID 可以为空。
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
            callLogDispatchService.asyncSave(message);
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
            // 将 JSON 解析为树以便按字段遍历；解析失败如何处理由当前 catch 分支决定。
            JsonNode node = objectMapper.readTree(content);
            if (node.has("success") && node.has("code")) {
                return node.get("code").asInt();
            }
        } catch (Exception ignored) {
            // 上游原始响应，不做解析
        }
        return null;
    }

    /**
     * 读取必填签名头，缺失或空白时抛出鉴权异常。
     */
    private String requireHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        // 校验条件成立时拒绝本次操作，抛出 MISSING_SIGNATURE_HEADER；不再继续后续正常业务步骤。
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.MISSING_SIGNATURE_HEADER, "缺少请求头 " + name);
        }
        return value;
    }

    /**
     * 提取 /openapi/ 后第一个路径段作为 API 编码，后续路径留给上游资源。
     */
    private String resolveApiCode(String uri, String contextPath) {
        String path = uri.substring(contextPath.length());
        String prefix = OPEN_API_PREFIX;
        // 校验条件成立时拒绝本次操作，抛出 NOT_FOUND；不再继续后续正常业务步骤。
        if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
            throw new BizException(ErrorCode.NOT_FOUND, "缺少 API 编码");
        }
        String rest = path.substring(prefix.length());
        int slash = rest.indexOf('/');
        return slash > 0 ? rest.substring(0, slash) : rest;
    }

    /**
     * 重置未提交响应的缓冲区，写入状态与 JSON 错误；已提交时不再覆盖。
     */
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

}
