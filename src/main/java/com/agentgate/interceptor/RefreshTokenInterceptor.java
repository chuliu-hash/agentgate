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
