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
 * 记录带 {@link AuditLog} 的管理操作结果；审计写入失败只记日志，不改变原业务结果。
 * 参数摘要会序列化后截断长度，但不会自动脱敏，调用方不得把密码或密钥放入被审计参数。
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

    /** 执行业务方法并记录成功或失败；业务异常原样抛回调用方。 */
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

    /** 从当前请求上下文构造审计记录；第一个数字参数作为资源 ID，写入异常不会影响业务。 */
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

    /** 序列化参数并截断存储长度；截断不等于脱敏，敏感参数必须在进入切面前排除。 */
    private String buildDetail(Object[] args, boolean success, String errorMessage) {
        StringBuilder detail = new StringBuilder();
        detail.append("success=").append(success);
        if (errorMessage != null) {
            detail.append(", error=").append(errorMessage);
        }
        try {
            // 保存操作参数以便追溯；参数中不得包含密码或密钥，长度截断不会脱敏。
            String argsJson = objectMapper.writeValueAsString(args);
            detail.append(", args=").append(argsJson);
        } catch (Exception ignored) {
            detail.append(", args=<unserializable>");
        }
        String text = detail.toString();
        return text.length() <= MAX_DETAIL_LENGTH ? text : text.substring(0, MAX_DETAIL_LENGTH);
    }

    /**
     * 从 Spring 请求上下文取得当前 HTTP 请求，无请求上下文时返回空值。
     */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        return null;
    }
}
