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
/**
 * 校验受保护 MVC 请求的登录用户，并对 /admin/ 路径额外校验 ADMIN 角色。
 * 用户上下文由先执行的 RefreshTokenInterceptor 从 Redis 恢复；此拦截器只负责拒绝未登录或越权请求。
 */
public class LoginInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    /** Controller 执行前检查用户上下文和管理员角色；拒绝时写 JSON 401/403 并终止 MVC 分发。 */
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

    /**
     * 写入登录校验失败的 HTTP 状态和 JSON 结果。
     */
    private void write(HttpServletResponse response, int status, Result<?> result) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
