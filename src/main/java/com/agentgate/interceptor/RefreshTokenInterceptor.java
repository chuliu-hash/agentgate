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
/**
 * 从 Authorization 中恢复 Redis 登录会话，并滑动续期有效 Token。
 * 缺少或无效 Token 时不在此处拒绝请求，由后续 LoginInterceptor 决定目标路径是否要求登录。
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    /** 尝试恢复当前请求用户；无 Token 或 Redis 中无会话时仍继续，由登录拦截器执行访问控制。 */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("authorization");
        // 公开请求可以没有 Token；不能仅因缺少凭证就在会话恢复阶段拒绝。
        if (!StringUtils.hasText(token)) {
            return true;
        }
        // 登录接口返回的 Token 可直接传入，也兼容 Authorization: Bearer <token> 格式。
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
        // 供 Controller、Service 和审计切面读取当前用户；请求结束后由 afterCompletion 清理。
        UserHolder.saveUser(user);
        // 设置或续期会话 TTL，避免未主动登出的会话永久有效。
        redisTemplate.expire(key, RedisConstants.LOGIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return true;
    }

    /**
     * 请求结束后清除 ThreadLocal 用户，避免线程池复用导致身份串用。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
