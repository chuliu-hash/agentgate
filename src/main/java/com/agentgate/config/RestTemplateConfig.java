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
public class RestTemplateConfig {

    @Value("${agentgate.openapi.connect-timeout-ms:3000}")
    private int connectTimeout;

    @Value("${agentgate.openapi.read-timeout-ms:5000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
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
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) {
                // no-op
            }
        });
        return restTemplate;
    }
}
