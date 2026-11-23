# 启动入口与配置

[返回源码索引](README.md) · [学习指南](../learning-guide.md)

## AgentGateApplication.java

源码：[src/main/java/com/agentgate/AgentGateApplication.java](../../src/main/java/com/agentgate/AgentGateApplication.java)

应用启动入口：main 调用 SpringApplication.run，扫描组件与 Mapper，建立 Web 应用上下文。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    public static void main(String[] args) {
```

### 完整代码

```java
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
```

## DataInitializer.java

源码：[src/main/java/com/agentgate/config/DataInitializer.java](../../src/main/java/com/agentgate/config/DataInitializer.java)

ApplicationRunner 启动回调：按存在性补齐管理员、三档套餐和 weather/order/user 演示 API；需先执行建表 SQL，不更新已存在记录。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final UserMapper userMapper;
    private final QuotaPlanMapper quotaPlanMapper;
    private final ApiDefinitionMapper apiDefinitionMapper;
    private String adminUsername;
    private String adminPassword;
    private int serverPort;
    public void run(ApplicationArguments args) {
    private void initAdmin() {
    private void initQuotaPlans() {
    private void insertPlanIfAbsent(String name, int qps, int daily, int monthly, String description) {
    private void initDemoApis() {
    private void insertApiIfAbsent(String code, String name, String method, String upstream,
```

### 完整代码

```java
package com.agentgate.config;

import cn.hutool.crypto.digest.BCrypt;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.entity.User;
import com.agentgate.enums.ApiStatus;
import com.agentgate.enums.UserRole;
import com.agentgate.mapper.ApiDefinitionMapper;
import com.agentgate.mapper.QuotaPlanMapper;
import com.agentgate.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐管理员、演示套餐与演示 API，重复启动幂等。
 * 建表脚本见 src/main/resources/db/agentgate.sql。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final QuotaPlanMapper quotaPlanMapper;
    private final ApiDefinitionMapper apiDefinitionMapper;

    @Value("${agentgate.admin.username:admin}")
    private String adminUsername;

    @Value("${agentgate.admin.password:admin123}")
    private String adminPassword;

    @Value("${server.port:8081}")
    private int serverPort;

    @Override
    public void run(ApplicationArguments args) {
        initAdmin();
        initQuotaPlans();
        initDemoApis();
    }

    private void initAdmin() {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, adminUsername));
        if (count != null && count > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(BCrypt.hashpw(adminPassword, BCrypt.gensalt()));
        admin.setNickname("平台管理员");
        admin.setRole(UserRole.ADMIN.name());
        admin.setStatus("ACTIVE");
        userMapper.insert(admin);
        log.info("已初始化管理员账号 {}", adminUsername);
    }

    private void initQuotaPlans() {
        insertPlanIfAbsent("试用版", 2, 1000, 10000, "适合接入联调与演示");
        insertPlanIfAbsent("标准版", 10, 10000, 100000, "适合中小规模业务");
        insertPlanIfAbsent("企业版", 50, 100000, 1000000, "适合高并发生产场景");
    }

    private void insertPlanIfAbsent(String name, int qps, int daily, int monthly, String description) {
        Long count = quotaPlanMapper.selectCount(
                new LambdaQueryWrapper<QuotaPlan>().eq(QuotaPlan::getName, name));
        if (count != null && count > 0) {
            return;
        }
        QuotaPlan plan = new QuotaPlan();
        plan.setName(name);
        plan.setQpsLimit(qps);
        plan.setDailyQuota(daily);
        plan.setMonthlyQuota(monthly);
        plan.setDescription(description);
        quotaPlanMapper.insert(plan);
        log.info("已初始化配额套餐 {}", name);
    }

    private void initDemoApis() {
        String base = "http://127.0.0.1:" + serverPort + "/mock-api";
        insertApiIfAbsent("weather", "天气查询", "GET", base + "/weather", 1,
                "演示用上游：返回城市天气");
        insertApiIfAbsent("order", "订单查询", "GET", base + "/order", 2,
                "演示敏感级别 2：响应中的手机号/邮箱/地址会被脱敏");
        insertApiIfAbsent("user", "用户信息查询", "GET", base + "/user", 3,
                "演示敏感级别 3：身份证/银行卡等字段会被脱敏");
    }

    private void insertApiIfAbsent(String code, String name, String method, String upstream,
                                   int sensitivityLevel, String description) {
        Long count = apiDefinitionMapper.selectCount(
                new LambdaQueryWrapper<ApiDefinition>().eq(ApiDefinition::getCode, code));
        if (count != null && count > 0) {
            return;
        }
        ApiDefinition api = new ApiDefinition();
        api.setCode(code);
        api.setName(name);
        api.setMethod(method);
        api.setUpstreamUrl(upstream);
        api.setVersion("v1");
        api.setSensitivityLevel(sensitivityLevel);
        api.setStatus(ApiStatus.PUBLISHED.name());
        api.setTimeoutMs(5000);
        api.setDescription(description);
        apiDefinitionMapper.insert(api);
        log.info("已初始化演示 API {}", code);
    }
}
```

## MvcConfig.java

源码：[src/main/java/com/agentgate/config/MvcConfig.java](../../src/main/java/com/agentgate/config/MvcConfig.java)

注册 MVC 拦截器：先恢复登录态，再执行登录与管理员校验；排除公开端点。路径与顺序直接决定哪些接口受到保护。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final RefreshTokenInterceptor refreshTokenInterceptor;
    private final LoginInterceptor loginInterceptor;
    public void addInterceptors(InterceptorRegistry registry) {
```

### 完整代码

```java
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
```

## MybatisConfig.java

源码：[src/main/java/com/agentgate/config/MybatisConfig.java](../../src/main/java/com/agentgate/config/MybatisConfig.java)

注册 MySQL 分页插件和 MetaObjectHandler；在插入/更新时填充实体声明的时间字段。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            public void insertFill(MetaObject metaObject) {
            public void updateFill(MetaObject metaObject) {
```

### 完整代码

```java
package com.agentgate.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

## RabbitMQConfig.java

源码：[src/main/java/com/agentgate/config/RabbitMQConfig.java](../../src/main/java/com/agentgate/config/RabbitMQConfig.java)

声明正常与死信 DirectExchange、持久队列、routing key 和绑定；注册 JSON 消息转换器。队列是待处理消息存储，交换机负责路由。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    public static final String CALL_LOG_EXCHANGE = "call.log.exchange";
    public static final String CALL_LOG_QUEUE = "call.log.queue";
    public static final String CALL_LOG_ROUTING_KEY = "call.log";
    public static final String CALL_LOG_DLX_EXCHANGE = "call.log.dlx.exchange";
    public static final String CALL_LOG_DLQ = "call.log.dlq";
    public static final String CALL_LOG_DLQ_ROUTING_KEY = "call.log.dlq";
    public DirectExchange callLogExchange() {
        return new DirectExchange(CALL_LOG_EXCHANGE, true, false);
    public DirectExchange callLogDlxExchange() {
        return new DirectExchange(CALL_LOG_DLX_EXCHANGE, true, false);
    public Queue callLogQueue() {
    public Queue callLogDlq() {
    public Binding callLogBinding() {
    public Binding callLogDlqBinding() {
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
```

### 完整代码

```java
package com.agentgate.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CALL_LOG_EXCHANGE = "call.log.exchange";
    public static final String CALL_LOG_QUEUE = "call.log.queue";
    public static final String CALL_LOG_ROUTING_KEY = "call.log";

    public static final String CALL_LOG_DLX_EXCHANGE = "call.log.dlx.exchange";
    public static final String CALL_LOG_DLQ = "call.log.dlq";
    public static final String CALL_LOG_DLQ_ROUTING_KEY = "call.log.dlq";

    @Bean
    public DirectExchange callLogExchange() {
        return new DirectExchange(CALL_LOG_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange callLogDlxExchange() {
        return new DirectExchange(CALL_LOG_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue callLogQueue() {
        return QueueBuilder.durable(CALL_LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", CALL_LOG_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CALL_LOG_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue callLogDlq() {
        return QueueBuilder.durable(CALL_LOG_DLQ).build();
    }

    @Bean
    public Binding callLogBinding() {
        return BindingBuilder.bind(callLogQueue()).to(callLogExchange()).with(CALL_LOG_ROUTING_KEY);
    }

    @Bean
    public Binding callLogDlqBinding() {
        return BindingBuilder.bind(callLogDlq()).to(callLogDlxExchange()).with(CALL_LOG_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

## RedissonConfig.java

源码：[src/main/java/com/agentgate/config/RedissonConfig.java](../../src/main/java/com/agentgate/config/RedissonConfig.java)

从 Redis host/port/password 构造单机 RedissonClient，设置连接池并在销毁时 shutdown；主要供缓存重建锁使用。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private String host;
    private int port;
    private String password;
    public RedissonClient redissonClient() {
```

### 完整代码

```java
package com.agentgate.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setConnectionPoolSize(16)
                .setConnectionMinimumIdleSize(4);
        if (StringUtils.hasText(password)) {
            server.setPassword(password);
        }
        return Redisson.create(config);
    }
}
```

## RestTemplateConfig.java

源码：[src/main/java/com/agentgate/config/RestTemplateConfig.java](../../src/main/java/com/agentgate/config/RestTemplateConfig.java)

构造同步 HTTP 客户端，设置全局连接/读取超时、禁重定向和 Proxy.NO_PROXY；关闭默认 4xx/5xx 抛异常行为以透传上游状态。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private int connectTimeout;
    private int readTimeout;
    public RestTemplate restTemplate() {
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            public boolean hasError(ClientHttpResponse response) {
            public void handleError(ClientHttpResponse response) {
```

### 完整代码

```java
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
```

## WebExceptionAdvice.java

源码：[src/main/java/com/agentgate/config/WebExceptionAdvice.java](../../src/main/java/com/agentgate/config/WebExceptionAdvice.java)

捕获 MVC 业务异常、参数校验异常与未知异常，转换为 HTTP 状态及 Result；不替代 Filter 自己的异常处理。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
    public ResponseEntity<Result<Void>> handleValidException(Exception e) {
    public ResponseEntity<Result<Void>> handleException(Exception e) {
    private HttpStatus resolveStatus(int code) {
```

### 完整代码

```java
package com.agentgate.config;

import com.agentgate.dto.Result;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        HttpStatus status = resolveStatus(e.getCode());
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidException(Exception e) {
        String message = "参数错误";
        if (e instanceof MethodArgumentNotValidException) {
            FieldError fieldError = ((MethodArgumentNotValidException) e).getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        } else if (e instanceof BindException) {
            FieldError fieldError = ((BindException) e).getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        }
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SYSTEM_ERROR));
    }

    private HttpStatus resolveStatus(int code) {
        switch (code) {
            case 401:
                return HttpStatus.UNAUTHORIZED;
            case 403:
                return HttpStatus.FORBIDDEN;
            case 404:
                return HttpStatus.NOT_FOUND;
            case 413:
                return HttpStatus.PAYLOAD_TOO_LARGE;
            case 429:
            case 4290:
                return HttpStatus.TOO_MANY_REQUESTS;
            case 405:
                return HttpStatus.METHOD_NOT_ALLOWED;
            case 2001:
                return HttpStatus.GATEWAY_TIMEOUT;
            case 2002:
                return HttpStatus.BAD_GATEWAY;
            case 500:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case 1001:
            case 1002:
            case 1003:
            case 1004:
                return HttpStatus.UNAUTHORIZED;
            case 1005:
            case 1006:
            case 1007:
            case 1008:
            case 1009:
                return HttpStatus.FORBIDDEN;
            default:
                return HttpStatus.BAD_REQUEST;
        }
    }
}
```

