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
