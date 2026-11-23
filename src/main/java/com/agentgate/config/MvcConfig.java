package com.agentgate.config;

import com.agentgate.interceptor.LoginInterceptor;
import com.agentgate.interceptor.RefreshTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
/**
 * 教学说明：注册 MVC 拦截器：先恢复登录态，再执行登录与管理员校验；排除公开端点。路径与顺序直接决定哪些接口受到保护。
 * 文件路径：src/main/java/com/agentgate/config/MvcConfig.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class MvcConfig implements WebMvcConfigurer {

    private final RefreshTokenInterceptor refreshTokenInterceptor;
    private final LoginInterceptor loginInterceptor;

    /**
     * 先注册登录态恢复，再注册登录与角色校验；公开路径排除强制登录。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(refreshTokenInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/openapi/**",
                        "/mock-api/**",
                        "/actuator/**",
                        "/error",
                        "/favicon.ico")
                .order(1);
    }
}
