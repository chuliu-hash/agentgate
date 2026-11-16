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
public class AgentGateApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentGateApplication.class, args);
    }
}
