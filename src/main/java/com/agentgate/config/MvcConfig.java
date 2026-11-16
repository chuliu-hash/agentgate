package com.agentgate.config;

import com.agentgate.interceptor.LoginInterceptor;
import com.agentgate.interceptor.RefreshTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

    private final RefreshTokenInterceptor refreshTokenInterceptor;
    private final LoginInterceptor loginInterceptor;

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
