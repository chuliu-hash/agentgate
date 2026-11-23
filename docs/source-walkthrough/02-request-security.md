# 请求过滤、认证与审计

[返回源码索引](README.md) · [学习指南](../learning-guide.md)

## SignatureFilter.java

源码：[src/main/java/com/agentgate/filter/SignatureFilter.java](../../src/main/java/com/agentgate/filter/SignatureFilter.java)

开放请求总入口：验证请求体大小、四个签名头、时间窗口、应用状态、IP、HMAC、Nonce、API 与订阅，扣减额度，写入请求属性；finally 收集成功和失败调用日志。辅助方法解析 API code、构造错误响应与状态映射。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
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
    private int maxBodyBytes;
    private long timestampWindowSeconds;
    private long nonceTtlMinutes;
    private boolean trustProxy;
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    new CachedBodyHttpServletRequest(request, maxBodyBytes);
                throw new BizException(ErrorCode.BODY_TOO_LARGE);
                throw new BizException(ErrorCode.TIMESTAMP_EXPIRED, "时间戳格式非法");
                throw new BizException(ErrorCode.TIMESTAMP_EXPIRED);
                throw new BizException(ErrorCode.SIGNATURE_INVALID);
                throw new BizException(ErrorCode.APP_DISABLED);
                throw new BizException(ErrorCode.IP_NOT_ALLOWED);
                throw new BizException(ErrorCode.SIGNATURE_INVALID);
                throw new BizException(ErrorCode.NONCE_REPLAYED);
                throw new BizException(ErrorCode.NOT_SUBSCRIBED);
    private Integer extractErrorCode(ContentCachingResponseWrapper response) {
    private String requireHeader(HttpServletRequest request, String name) {
            throw new BizException(ErrorCode.MISSING_SIGNATURE_HEADER, "缺少请求头 " + name);
    private String resolveApiCode(String uri, String contextPath) {
            throw new BizException(ErrorCode.NOT_FOUND, "缺少 API 编码");
    private void writeError(HttpServletResponse response, int status, Result<?> result) {
    private int resolveStatus(int code) {
```

### 完整代码

```java
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
```

## TraceIdFilter.java

源码：[src/main/java/com/agentgate/filter/TraceIdFilter.java](../../src/main/java/com/agentgate/filter/TraceIdFilter.java)

最高优先级过滤器：生成追踪 ID，写入 request、响应头和 MDC；finally 清除线程上下文。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
```

### 完整代码

```java
package com.agentgate.filter;

import com.agentgate.utils.TraceIdUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 为每个请求生成 traceId，写入 MDC（日志）与响应头，贯穿调用日志排查。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = TraceIdUtils.generate();
        request.setAttribute(TraceIdUtils.TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        TraceIdUtils.put(traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdUtils.clear();
        }
    }
}
```

## LoginInterceptor.java

源码：[src/main/java/com/agentgate/interceptor/LoginInterceptor.java](../../src/main/java/com/agentgate/interceptor/LoginInterceptor.java)

对受保护请求检查 ThreadLocal 用户，对 /admin/ 前缀额外要求 ADMIN；写 JSON 401/403 并返回 false 终止处理。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final ObjectMapper objectMapper;
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    private void write(HttpServletResponse response, int status, Result<?> result) throws Exception {
```

### 完整代码

```java
package com.agentgate.interceptor;

import com.agentgate.dto.Result;
import com.agentgate.dto.UserDTO;
import com.agentgate.enums.ErrorCode;
import com.agentgate.enums.UserRole;
import com.agentgate.utils.UserHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, Result.fail(ErrorCode.UNAUTHORIZED));
            return false;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin/") && !UserRole.ADMIN.name().equals(user.getRole())) {
            write(response, HttpServletResponse.SC_FORBIDDEN, Result.fail(ErrorCode.FORBIDDEN));
            return false;
        }
        return true;
    }

    private void write(HttpServletResponse response, int status, Result<?> result) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
```

## RefreshTokenInterceptor.java

源码：[src/main/java/com/agentgate/interceptor/RefreshTokenInterceptor.java](../../src/main/java/com/agentgate/interceptor/RefreshTokenInterceptor.java)

读取 Authorization，兼容 Bearer 前缀，查 Redis Hash 恢复 UserDTO 并续期；缺少 Token 不直接阻止请求；afterCompletion 清理 UserHolder。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final StringRedisTemplate redisTemplate;
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
```

### 完整代码

```java
package com.agentgate.interceptor;

import com.agentgate.dto.UserDTO;
import com.agentgate.utils.RedisConstants;
import com.agentgate.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("authorization");
        if (!StringUtils.hasText(token)) {
            return true;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String key = RedisConstants.LOGIN_TOKEN_KEY + token;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return true;
        }
        UserDTO user = new UserDTO();
        Object id = entries.get("id");
        if (id != null) {
            user.setId(Long.valueOf(id.toString()));
        }
        user.setUsername((String) entries.get("username"));
        user.setNickname((String) entries.get("nickname"));
        user.setRole((String) entries.get("role"));
        UserHolder.saveUser(user);
        redisTemplate.expire(key, RedisConstants.LOGIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
```

## AuditLog.java

源码：[src/main/java/com/agentgate/annotation/AuditLog.java](../../src/main/java/com/agentgate/annotation/AuditLog.java)



### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    String action();
    String resourceType() default "";
```

### 完整代码

```java
package com.agentgate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    String action();

    String resourceType() default "";
}
```

## AuditLogAspect.java

源码：[src/main/java/com/agentgate/aspect/AuditLogAspect.java](../../src/main/java/com/agentgate/aspect/AuditLogAspect.java)

Around 拦截带审计注解的方法，执行原业务并捕获结果；取 ThreadLocal 用户、参数与 IP 生成记录，摘要截断 1000 字符，审计失败不影响主业务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final int MAX_DETAIL_LENGTH = 1000;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private boolean trustProxy;
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    private void record(ProceedingJoinPoint joinPoint, AuditLog auditLog,
    private String buildDetail(Object[] args, boolean success, String errorMessage) {
    private HttpServletRequest currentRequest() {
```

### 完整代码

```java
package com.agentgate.aspect;

import com.agentgate.annotation.AuditLog;
import com.agentgate.dto.UserDTO;
import com.agentgate.service.AuditLogService;
import com.agentgate.utils.IpUtils;
import com.agentgate.utils.UserHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 管理端写操作审计：只追加，不影响业务主流程。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private static final int MAX_DETAIL_LENGTH = 1000;

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Value("${agentgate.security.trust-proxy:false}")
    private boolean trustProxy;

    @Around("@annotation(com.agentgate.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        AuditLog auditLog = ((MethodSignature) joinPoint.getSignature()).getMethod()
                .getAnnotation(AuditLog.class);
        try {
            Object result = joinPoint.proceed();
            record(joinPoint, auditLog, true, null);
            return result;
        } catch (Throwable throwable) {
            record(joinPoint, auditLog, false, throwable.getMessage());
            throw throwable;
        }
    }

    private void record(ProceedingJoinPoint joinPoint, AuditLog auditLog,
                        boolean success, String errorMessage) {
        try {
            UserDTO user = UserHolder.getUser();
            if (user == null) {
                return;
            }
            com.agentgate.entity.AuditLog entry = new com.agentgate.entity.AuditLog();
            entry.setUserId(user.getId());
            entry.setUsername(user.getUsername());
            entry.setAction(auditLog.action());
            entry.setResourceType(auditLog.resourceType());

            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof Number) {
                entry.setResourceId(String.valueOf(args[0]));
            }
            entry.setDetail(buildDetail(args, success, errorMessage));

            HttpServletRequest request = currentRequest();
            if (request != null) {
                entry.setIp(IpUtils.getClientIp(request, trustProxy));
            }
            auditLogService.save(entry);
        } catch (Exception e) {
            // 审计失败不能影响正常业务
            log.warn("审计日志写入失败 action={}", auditLog.action(), e);
        }
    }

    private String buildDetail(Object[] args, boolean success, String errorMessage) {
        StringBuilder detail = new StringBuilder();
        detail.append("success=").append(success);
        if (errorMessage != null) {
            detail.append(", error=").append(errorMessage);
        }
        try {
            String argsJson = objectMapper.writeValueAsString(args);
            detail.append(", args=").append(argsJson);
        } catch (Exception ignored) {
            detail.append(", args=<unserializable>");
        }
        String text = detail.toString();
        return text.length() <= MAX_DETAIL_LENGTH ? text : text.substring(0, MAX_DETAIL_LENGTH);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        return null;
    }
}
```

## BizException.java

源码：[src/main/java/com/agentgate/exception/BizException.java](../../src/main/java/com/agentgate/exception/BizException.java)

携带业务 code/message 的运行时异常；由 MVC Advice 或签名 Filter 转为客户端错误，不用于正常成功返回。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private final int code;
    public BizException(ErrorCode errorCode) {
    public BizException(ErrorCode errorCode, String message) {
    public BizException(int code, String message) {
```

### 完整代码

```java
package com.agentgate.exception;

import com.agentgate.enums.ErrorCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

