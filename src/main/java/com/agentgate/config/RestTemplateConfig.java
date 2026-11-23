package com.agentgate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;

@Configuration
/**
 * 教学说明：构造同步 HTTP 客户端，设置全局连接/读取超时、禁重定向和 Proxy.NO_PROXY；关闭默认 4xx/5xx 抛异常行为以透传上游状态。
 * 文件路径：src/main/java/com/agentgate/config/RestTemplateConfig.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class RestTemplateConfig {

    @Value("${agentgate.openapi.connect-timeout-ms:3000}")
    private int connectTimeout;

    @Value("${agentgate.openapi.read-timeout-ms:5000}")
    private int readTimeout;

    /**
     * 注册同步 HTTP 客户端：设置全局超时、禁重定向和直连，允许透传上游错误状态。
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            /**
             * 准备连接后关闭自动重定向，降低跳转到危险地址的风险。
             */
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                // 禁止跟随重定向，降低 SSRF 绕过风险
                connection.setInstanceFollowRedirects(false);
            }
        };

        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        // 上游直连：避免继承操作系统/JVM 代理，导致 SSRF 校验的地址与实际出口不一致
        factory.setProxy(Proxy.NO_PROXY);
        RestTemplate restTemplate = new RestTemplate(factory);
        // 开放平台需要把上游的 4xx/5xx 原样透传，而不是抛异常
        // 教学说明：代理需要保留上游 HTTP 4xx/5xx，故关闭默认错误状态抛异常行为；网络连接异常仍会抛出。
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            /**
             * 不将上游 HTTP 4xx/5xx 视为客户端异常，以便代理透传状态。
             */
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            /**
             * 保留错误处理回调的空实现，上游 HTTP 错误状态由代理响应返回。
             */
            @Override
            public void handleError(ClientHttpResponse response) {
                // no-op
            }
        });

        return restTemplate;
    }
}
