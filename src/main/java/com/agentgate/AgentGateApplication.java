package com.agentgate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableRabbit
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.agentgate.mapper")
@SpringBootApplication
/**
 * 教学说明：应用启动入口：main 调用 SpringApplication.run，扫描组件与 Mapper，建立 Web 应用上下文。
 * 文件路径：src/main/java/com/agentgate/AgentGateApplication.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AgentGateApplication {

    /**
     * 启动 Spring Boot 容器，完成组件扫描、自动配置与 Web 服务注册。
     */
    public static void main(String[] args) {
        SpringApplication.run(AgentGateApplication.class, args);
    }
}
