# 业务接口、实现与消息消费

[返回源码索引](README.md) · [学习指南](../learning-guide.md)

## ApiService.java

源码：[src/main/java/com/agentgate/service/ApiService.java](../../src/main/java/com/agentgate/service/ApiService.java)

业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    ApiDefinition create(ApiCreateDTO dto);
    ApiDefinition update(Long id, ApiCreateDTO dto);
    void publish(Long id);
    void offline(Long id);
    List<ApiDefinition> listAll();
    List<ApiDefinition> listPublished();
    ApiDefinition getById(Long id);
    ApiDefinition getByCode(String code);
    ApiDefinition getPublishedByCode(String code);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.ApiCreateDTO;
import com.agentgate.entity.ApiDefinition;

import java.util.List;

public interface ApiService {

    ApiDefinition create(ApiCreateDTO dto);

    ApiDefinition update(Long id, ApiCreateDTO dto);

    void publish(Long id);

    void offline(Long id);

    List<ApiDefinition> listAll();

    List<ApiDefinition> listPublished();

    ApiDefinition getById(Long id);

    ApiDefinition getByCode(String code);

    ApiDefinition getPublishedByCode(String code);
}
```

## AppService.java

源码：[src/main/java/com/agentgate/service/AppService.java](../../src/main/java/com/agentgate/service/AppService.java)

业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    AppSecretDTO create(AppCreateDTO dto);
    List<App> listMine();
    AppSecretDTO resetSecret(Long appId);
    void updateIpWhitelist(Long appId, String ipWhitelist);
    void disable(Long appId);
    App getById(Long appId);
    App getOwnedApp(Long appId);
    AppAuthInfo getAuthInfo(String appKey);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.AppAuthInfo;
import com.agentgate.dto.AppCreateDTO;
import com.agentgate.dto.AppSecretDTO;
import com.agentgate.entity.App;

import java.util.List;

public interface AppService {

    AppSecretDTO create(AppCreateDTO dto);

    List<App> listMine();

    AppSecretDTO resetSecret(Long appId);

    void updateIpWhitelist(Long appId, String ipWhitelist);

    void disable(Long appId);

    App getById(Long appId);

    /** 校验当前登录用户是否拥有该应用 */
    App getOwnedApp(Long appId);

    AppAuthInfo getAuthInfo(String appKey);
}
```

## AuditLogService.java

源码：[src/main/java/com/agentgate/service/AuditLogService.java](../../src/main/java/com/agentgate/service/AuditLogService.java)

业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    void save(AuditLog auditLog);
    PageResult<AuditLog> page(long page, long size);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.PageResult;
import com.agentgate.entity.AuditLog;

public interface AuditLogService {

    void save(AuditLog auditLog);

    PageResult<AuditLog> page(long page, long size);
}
```

## CallLogService.java

源码：[src/main/java/com/agentgate/service/CallLogService.java](../../src/main/java/com/agentgate/service/CallLogService.java)

调用日志持久化与查询契约：按 `traceId` 幂等写入日志并累计日用量，同时提供范围受限的分页与 `traceId` 查询。`saveDirect` 必须由其他 Bean 调用，才能经过 Spring 代理应用 `@Transactional`。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    void saveDirect(CallLogMessage message);
    PageResult<CallLog> page(CallLogQueryDTO query, Long restrictAppId);
    CallLog getByTraceId(String traceId, Long restrictAppId);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.CallLogMessage;
import com.agentgate.dto.CallLogQueryDTO;
import com.agentgate.dto.PageResult;
import com.agentgate.entity.CallLog;

public interface CallLogService {

    void saveDirect(CallLogMessage message);

    PageResult<CallLog> page(CallLogQueryDTO query, Long restrictAppId);

    CallLog getByTraceId(String traceId, Long restrictAppId);
}
```

## DashboardService.java

源码：[src/main/java/com/agentgate/service/DashboardService.java](../../src/main/java/com/agentgate/service/DashboardService.java)

业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    DashboardSummaryDTO summary(Long appId);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.DashboardSummaryDTO;

public interface DashboardService {

    DashboardSummaryDTO summary(Long appId);
}
```

## OpenApiProxyService.java

源码：[src/main/java/com/agentgate/service/OpenApiProxyService.java](../../src/main/java/com/agentgate/service/OpenApiProxyService.java)

业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    ProxyResponse forward(HttpServletRequest request, ApiDefinition api, String traceId);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.ProxyResponse;
import com.agentgate.entity.ApiDefinition;

import javax.servlet.http.HttpServletRequest;

public interface OpenApiProxyService {

    ProxyResponse forward(HttpServletRequest request, ApiDefinition api, String traceId);
}
```

## ProxyResponse.java

源码：[src/main/java/com/agentgate/dto/ProxyResponse.java](../../src/main/java/com/agentgate/dto/ProxyResponse.java)

代理结果载体：保存上游 HTTP 状态、Content-Type、原始响应字节；用于 Service 与 Controller 传递，不套管理接口 Result。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private int statusCode;
    private String contentType;
    private byte[] body;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

@Data
public class ProxyResponse {

    private int statusCode;
    private String contentType;
    private byte[] body;
}
```

## QuotaPlanService.java

源码：[src/main/java/com/agentgate/service/QuotaPlanService.java](../../src/main/java/com/agentgate/service/QuotaPlanService.java)

业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    QuotaPlan create(QuotaPlanDTO dto);
    QuotaPlan update(Long id, QuotaPlanDTO dto);
    List<QuotaPlan> listAll();
    QuotaPlan getById(Long id);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.QuotaPlanDTO;
import com.agentgate.entity.QuotaPlan;

import java.util.List;

public interface QuotaPlanService {

    QuotaPlan create(QuotaPlanDTO dto);

    QuotaPlan update(Long id, QuotaPlanDTO dto);

    List<QuotaPlan> listAll();

    QuotaPlan getById(Long id);
}
```

## RateLimitService.java

源码：[src/main/java/com/agentgate/service/RateLimitService.java](../../src/main/java/com/agentgate/service/RateLimitService.java)

业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    void checkAndConsume(Long appId, ApiDefinition api, QuotaPlan plan);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;

public interface RateLimitService {

    /**
     * QPS 与日/月配额都通过时返回 true，否则抛出对应业务异常。
     */
    void checkAndConsume(Long appId, ApiDefinition api, QuotaPlan plan);
}
```

## SubscriptionService.java

源码：[src/main/java/com/agentgate/service/SubscriptionService.java](../../src/main/java/com/agentgate/service/SubscriptionService.java)

业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    Subscription apply(SubscribeDTO dto);
    List<Subscription> listMine(Long appId);
    List<Subscription> listPending();
    List<Subscription> listAll();
    void approve(Long id, boolean approved, String reason);
    Subscription getActive(Long appId, Long apiId);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.SubscribeDTO;
import com.agentgate.entity.Subscription;

import java.util.List;

public interface SubscriptionService {

    Subscription apply(SubscribeDTO dto);

    List<Subscription> listMine(Long appId);

    List<Subscription> listPending();

    List<Subscription> listAll();

    void approve(Long id, boolean approved, String reason);

    Subscription getActive(Long appId, Long apiId);
}
```

## UserService.java

源码：[src/main/java/com/agentgate/service/UserService.java](../../src/main/java/com/agentgate/service/UserService.java)

业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    void register(RegisterFormDTO dto);
    LoginResultDTO login(LoginFormDTO dto);
    void logout(String token);
    UserDTO me();
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.LoginFormDTO;
import com.agentgate.dto.LoginResultDTO;
import com.agentgate.dto.RegisterFormDTO;
import com.agentgate.dto.UserDTO;

public interface UserService {

    void register(RegisterFormDTO dto);

    LoginResultDTO login(LoginFormDTO dto);

    void logout(String token);

    UserDTO me();
}
```

## ApiServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/ApiServiceImpl.java](../../src/main/java/com/agentgate/service/impl/ApiServiceImpl.java)

创建/修改时校验 code 和上游地址；发布/下架使用状态规则与旧状态条件更新；按 code 缓存查询；变更删除旧、新 code 缓存。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final ApiDefinitionMapper apiDefinitionMapper;
    private final CacheClient cacheClient;
    private boolean allowLoopback;
    public ApiDefinition create(ApiCreateDTO dto) {
            throw new BizException(ErrorCode.API_CODE_EXISTS);
            throw new BizException(ErrorCode.API_CODE_EXISTS);
    public ApiDefinition update(Long id, ApiCreateDTO dto) {
            throw new BizException(ErrorCode.API_CODE_EXISTS);
    public void publish(Long id) {
    public void offline(Long id) {
    public List<ApiDefinition> listAll() {
    public List<ApiDefinition> listPublished() {
    public ApiDefinition getById(Long id) {
            throw new BizException(ErrorCode.API_NOT_FOUND);
    public ApiDefinition getByCode(String code) {
    public ApiDefinition getPublishedByCode(String code) {
            throw new BizException(ErrorCode.API_NOT_FOUND);
            throw new BizException(ErrorCode.API_OFFLINE);
    private void transition(Long id, ApiStatus target) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID);
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID,
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "状态已被其他操作修改，请刷新后重试");
    private ApiDefinition findByCode(String code) {
    private void copy(ApiCreateDTO dto, ApiDefinition api) {
    private void deleteCache(String code) {
```

### 完整代码

```java
package com.agentgate.service.impl;

import com.agentgate.dto.ApiCreateDTO;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.enums.ApiStatus;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.ApiDefinitionMapper;
import com.agentgate.service.ApiService;
import com.agentgate.utils.CacheClient;
import com.agentgate.utils.RedisConstants;
import com.agentgate.utils.SsrfUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {

    private final ApiDefinitionMapper apiDefinitionMapper;
    private final CacheClient cacheClient;

    @Value("${agentgate.ssrf.allow-loopback:false}")
    private boolean allowLoopback;

    @Override
    public ApiDefinition create(ApiCreateDTO dto) {
        if (findByCode(dto.getCode()) != null) {
            throw new BizException(ErrorCode.API_CODE_EXISTS);
        }
        SsrfUtils.validate(dto.getUpstreamUrl(), allowLoopback);
        ApiDefinition api = new ApiDefinition();
        copy(dto, api);
        api.setStatus(ApiStatus.DRAFT.name());
        try {
            apiDefinitionMapper.insert(api);
        } catch (DuplicateKeyException e) {
            // 并发创建时 code 唯一索引兜底
            throw new BizException(ErrorCode.API_CODE_EXISTS);
        }
        return api;
    }

    @Override
    public ApiDefinition update(Long id, ApiCreateDTO dto) {
        ApiDefinition api = getById(id);
        ApiDefinition sameCode = findByCode(dto.getCode());
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new BizException(ErrorCode.API_CODE_EXISTS);
        }
        SsrfUtils.validate(dto.getUpstreamUrl(), allowLoopback);
        String oldCode = api.getCode();
        copy(dto, api);
        apiDefinitionMapper.updateById(api);
        deleteCache(oldCode);
        deleteCache(dto.getCode());
        return api;
    }

    @Override
    public void publish(Long id) {
        transition(id, ApiStatus.PUBLISHED);
    }

    @Override
    public void offline(Long id) {
        transition(id, ApiStatus.OFFLINE);
    }

    @Override
    public List<ApiDefinition> listAll() {
        return apiDefinitionMapper.selectList(
                new LambdaQueryWrapper<ApiDefinition>().orderByDesc(ApiDefinition::getCreateTime));
    }

    @Override
    public List<ApiDefinition> listPublished() {
        return apiDefinitionMapper.selectList(
                new LambdaQueryWrapper<ApiDefinition>()
                        .eq(ApiDefinition::getStatus, ApiStatus.PUBLISHED.name())
                        .orderByDesc(ApiDefinition::getCreateTime));
    }

    @Override
    public ApiDefinition getById(Long id) {
        ApiDefinition api = apiDefinitionMapper.selectById(id);
        if (api == null) {
            throw new BizException(ErrorCode.API_NOT_FOUND);
        }
        return api;
    }

    @Override
    public ApiDefinition getByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String key = RedisConstants.CACHE_API_KEY + code;
        return cacheClient.getOrLoad(key, ApiDefinition.class,
                RedisConstants.CACHE_API_TTL_MINUTES, () -> findByCode(code));
    }

    @Override
    public ApiDefinition getPublishedByCode(String code) {
        ApiDefinition api = getByCode(code);
        if (api == null) {
            throw new BizException(ErrorCode.API_NOT_FOUND);
        }
        if (!ApiStatus.PUBLISHED.name().equals(api.getStatus())) {
            throw new BizException(ErrorCode.API_OFFLINE);
        }
        return api;
    }

    private void transition(Long id, ApiStatus target) {
        ApiDefinition api = getById(id);
        ApiStatus current;
        try {
            current = ApiStatus.valueOf(api.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID);
        }
        if (!current.canTransitionTo(target)) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID,
                    "API 不能从 " + current + " 流转到 " + target);
        }
        int rows = apiDefinitionMapper.update(null, new LambdaUpdateWrapper<ApiDefinition>()
                .eq(ApiDefinition::getId, id)
                .eq(ApiDefinition::getStatus, current.name())
                .set(ApiDefinition::getStatus, target.name())
                .set(ApiDefinition::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "状态已被其他操作修改，请刷新后重试");
        }
        deleteCache(api.getCode());
    }

    private ApiDefinition findByCode(String code) {
        return apiDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ApiDefinition>()
                        .eq(ApiDefinition::getCode, code)
                        .last("limit 1"));
    }

    private void copy(ApiCreateDTO dto, ApiDefinition api) {
        api.setCode(dto.getCode());
        api.setName(dto.getName());
        api.setMethod(dto.getMethod());
        api.setUpstreamUrl(dto.getUpstreamUrl());
        api.setVersion(StringUtils.hasText(dto.getVersion()) ? dto.getVersion() : "v1");
        api.setSensitivityLevel(dto.getSensitivityLevel() == null ? 1 : dto.getSensitivityLevel());
        api.setTimeoutMs(dto.getTimeoutMs() == null ? 5000 : dto.getTimeoutMs());
        api.setDescription(dto.getDescription());
    }

    private void deleteCache(String code) {
        if (StringUtils.hasText(code)) {
            cacheClient.delete(RedisConstants.CACHE_API_KEY + code);
        }
    }
}
```

## AppServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/AppServiceImpl.java](../../src/main/java/com/agentgate/service/impl/AppServiceImpl.java)

生成 AK/SK、AES 加密保存 Secret；getOwnedApp 校验所有权；重置/禁用/白名单变更后删缓存；getAuthInfo 解密 SK 后缓存供验签使用。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final AppMapper appMapper;
    private final CacheClient cacheClient;
    private String secretKey;
    public AppSecretDTO create(AppCreateDTO dto) {
        return new AppSecretDTO(app.getId(), appKey, appSecret);
    public List<App> listMine() {
    public AppSecretDTO resetSecret(Long appId) {
        return new AppSecretDTO(app.getId(), app.getAppKey(), appSecret);
    public void updateIpWhitelist(Long appId, String ipWhitelist) {
    public void disable(Long appId) {
    public App getById(Long appId) {
            throw new BizException(ErrorCode.APP_NOT_FOUND);
    public AppAuthInfo getAuthInfo(String appKey) {
    public App getOwnedApp(Long appId) {
            throw new BizException(ErrorCode.APP_ACCESS_DENIED);
    private Long requireUserId() {
            throw new BizException(ErrorCode.UNAUTHORIZED);
    private void deleteCache(String appKey) {
```

### 完整代码

```java
package com.agentgate.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.agentgate.dto.AppAuthInfo;
import com.agentgate.dto.AppCreateDTO;
import com.agentgate.dto.AppSecretDTO;
import com.agentgate.entity.App;
import com.agentgate.enums.AppStatus;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.AppMapper;
import com.agentgate.service.AppService;
import com.agentgate.utils.AesUtils;
import com.agentgate.utils.CacheClient;
import com.agentgate.utils.RedisConstants;
import com.agentgate.utils.UserHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {

    private final AppMapper appMapper;
    private final CacheClient cacheClient;

    @Value("${agentgate.secret-key}")
    private String secretKey;

    @Override
    public AppSecretDTO create(AppCreateDTO dto) {
        Long userId = requireUserId();
        String appKey = "ak_" + RandomUtil.randomString(16);
        String appSecret = RandomUtil.randomString(32);

        App app = new App();
        app.setUserId(userId);
        app.setName(dto.getName());
        app.setAppKey(appKey);
        app.setAppSecretCipher(AesUtils.encrypt(appSecret, secretKey));
        app.setStatus(AppStatus.ACTIVE.name());
        app.setIpWhitelist(dto.getIpWhitelist());
        appMapper.insert(app);
        return new AppSecretDTO(app.getId(), appKey, appSecret);
    }

    @Override
    public List<App> listMine() {
        return appMapper.selectList(
                new LambdaQueryWrapper<App>()
                        .eq(App::getUserId, requireUserId())
                        .orderByDesc(App::getCreateTime));
    }

    @Override
    public AppSecretDTO resetSecret(Long appId) {
        App app = getOwnedApp(appId);
        String appSecret = RandomUtil.randomString(32);
        appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getAppSecretCipher, AesUtils.encrypt(appSecret, secretKey))
                .set(App::getUpdateTime, LocalDateTime.now()));
        deleteCache(app.getAppKey());
        return new AppSecretDTO(app.getId(), app.getAppKey(), appSecret);
    }

    @Override
    public void updateIpWhitelist(Long appId, String ipWhitelist) {
        App app = getOwnedApp(appId);
        appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getIpWhitelist, ipWhitelist)
                .set(App::getUpdateTime, LocalDateTime.now()));
        deleteCache(app.getAppKey());
    }

    @Override
    public void disable(Long appId) {
        App app = getOwnedApp(appId);
        appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getStatus, AppStatus.DISABLED.name())
                .set(App::getUpdateTime, LocalDateTime.now()));
        deleteCache(app.getAppKey());
    }

    @Override
    public App getById(Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new BizException(ErrorCode.APP_NOT_FOUND);
        }
        return app;
    }

    @Override
    public AppAuthInfo getAuthInfo(String appKey) {
        if (!StringUtils.hasText(appKey)) {
            return null;
        }
        String key = RedisConstants.CACHE_APP_KEY + appKey;
        return cacheClient.getOrLoad(key, AppAuthInfo.class,
                RedisConstants.CACHE_APP_TTL_MINUTES, () -> {
                    App app = appMapper.selectOne(
                            new LambdaQueryWrapper<App>()
                                    .eq(App::getAppKey, appKey)
                                    .last("limit 1"));
                    if (app == null) {
                        return null;
                    }
                    AppAuthInfo info = new AppAuthInfo();
                    info.setId(app.getId());
                    info.setUserId(app.getUserId());
                    info.setAppKey(app.getAppKey());
                    info.setStatus(app.getStatus());
                    info.setIpWhitelist(app.getIpWhitelist());
                    info.setAppSecret(AesUtils.decrypt(app.getAppSecretCipher(), secretKey));
                    return info;
                });
    }

    @Override
    public App getOwnedApp(Long appId) {
        App app = getById(appId);
        if (!app.getUserId().equals(requireUserId())) {
            throw new BizException(ErrorCode.APP_ACCESS_DENIED);
        }
        return app;
    }

    private Long requireUserId() {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private void deleteCache(String appKey) {
        cacheClient.delete(RedisConstants.CACHE_APP_KEY + appKey);
    }
}
```

## AuditLogServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/AuditLogServiceImpl.java](../../src/main/java/com/agentgate/service/impl/AuditLogServiceImpl.java)

保存管理操作审计并提供有上限的分页查询；与调用日志不同，没有 MQ 异步消费链路。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final AuditLogMapper auditLogMapper;
    public void save(AuditLog auditLog) {
    public PageResult<AuditLog> page(long page, long size) {
```

### 完整代码

```java
package com.agentgate.service.impl;

import com.agentgate.dto.PageResult;
import com.agentgate.entity.AuditLog;
import com.agentgate.mapper.AuditLogMapper;
import com.agentgate.service.AuditLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void save(AuditLog auditLog) {
        auditLogMapper.insert(auditLog);
    }

    @Override
    public PageResult<AuditLog> page(long page, long size) {
        long current = Math.max(1L, page);
        long pageSize = Math.min(Math.max(1L, size), 100L);
        Page<AuditLog> result = auditLogMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getCreateTime));
        return PageResult.of(result);
    }
}
```

## CallLogServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/CallLogServiceImpl.java](../../src/main/java/com/agentgate/service/impl/CallLogServiceImpl.java)

在事务内按 `traceId` 幂等写入调用日志并更新日用量统计；同时提供范围受限的分页与 `traceId` 查询。MQ 发送职责已拆分到 `CallLogDispatchService`。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long MAX_PAGE_SIZE = 100L;
    private final CallLogMapper callLogMapper;
    private final UsageStatMapper usageStatMapper;
    public void saveDirect(CallLogMessage message) {
    public PageResult<CallLog> page(CallLogQueryDTO query, Long restrictAppId) {
    public CallLog getByTraceId(String traceId, Long restrictAppId) {
            throw new BizException(ErrorCode.NOT_FOUND, "调用日志不存在");
            throw new BizException(ErrorCode.APP_ACCESS_DENIED);
```

### 完整代码

```java
package com.agentgate.service.impl;

import com.agentgate.dto.CallLogMessage;
import com.agentgate.dto.CallLogQueryDTO;
import com.agentgate.dto.PageResult;
import com.agentgate.entity.CallLog;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.CallLogMapper;
import com.agentgate.mapper.UsageStatMapper;
import com.agentgate.service.CallLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CallLogServiceImpl implements CallLogService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final CallLogMapper callLogMapper;
    private final UsageStatMapper usageStatMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDirect(CallLogMessage message) {
        CallLog callLog = new CallLog();
        callLog.setTraceId(message.getTraceId());
        callLog.setAppId(message.getAppId());
        callLog.setApiId(message.getApiId());
        callLog.setMethod(message.getMethod());
        callLog.setPath(message.getPath());
        callLog.setStatusCode(message.getStatusCode());
        callLog.setLatencyMs(message.getLatencyMs());
        callLog.setClientIp(message.getClientIp());
        callLog.setErrorCode(message.getErrorCode());
        callLog.setRequestSize(message.getRequestSize());
        callLog.setResponseSize(message.getResponseSize());
        callLog.setCreateTime(LocalDateTime.now());

        int inserted = callLogMapper.insertIgnore(callLog);
        if (inserted == 0) {
            return;
        }
        if (message.getAppId() != null && message.getApiId() != null) {
            int isError = message.getStatusCode() != null && message.getStatusCode() >= 400 ? 1 : 0;
            usageStatMapper.upsertIncrement(
                    message.getAppId(), message.getApiId(), LocalDate.now(), isError);
        }
    }

    @Override
    public PageResult<CallLog> page(CallLogQueryDTO query, Long restrictAppId) {
        long current = Math.max(1L, query.getPage());
        long size = Math.min(Math.max(1L, query.getSize()), MAX_PAGE_SIZE);
        Long appId = restrictAppId != null ? restrictAppId : query.getAppId();

        LambdaQueryWrapper<CallLog> wrapper = new LambdaQueryWrapper<CallLog>()
                .eq(appId != null, CallLog::getAppId, appId)
                .eq(query.getApiId() != null, CallLog::getApiId, query.getApiId())
                .eq(StringUtils.hasText(query.getTraceId()), CallLog::getTraceId, query.getTraceId())
                .ge(query.getStartTime() != null, CallLog::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, CallLog::getCreateTime, query.getEndTime())
                .orderByDesc(CallLog::getCreateTime);

        Page<CallLog> page = callLogMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page);
    }

    @Override
    public CallLog getByTraceId(String traceId, Long restrictAppId) {
        CallLog callLog = callLogMapper.selectOne(
                new LambdaQueryWrapper<CallLog>()
                        .eq(CallLog::getTraceId, traceId)
                        .last("limit 1"));
        if (callLog == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "调用日志不存在");
        }
        if (restrictAppId != null && !restrictAppId.equals(callLog.getAppId())) {
            throw new BizException(ErrorCode.APP_ACCESS_DENIED);
        }
        return callLog;
    }
}
```

## DashboardServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/DashboardServiceImpl.java](../../src/main/java/com/agentgate/service/impl/DashboardServiceImpl.java)

从调用日志 Mapper 聚合当日调用量、2xx 成功量、成功率、平均耗时、活跃应用和 Top5 API；appId 为空表示全局。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final CallLogMapper callLogMapper;
    private final ApiDefinitionMapper apiDefinitionMapper;
    public DashboardSummaryDTO summary(Long appId) {
    private List<Map<String, Object>> enrichTopApis(List<Map<String, Object>> rows) {
    private Long toLong(Object value) {
```

### 完整代码

```java
package com.agentgate.service.impl;

import com.agentgate.dto.DashboardSummaryDTO;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.mapper.ApiDefinitionMapper;
import com.agentgate.mapper.CallLogMapper;
import com.agentgate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final CallLogMapper callLogMapper;
    private final ApiDefinitionMapper apiDefinitionMapper;

    @Override
    public DashboardSummaryDTO summary(Long appId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        long todayCalls = callLogMapper.countSince(start, appId);
        long successCalls = callLogMapper.countSuccessSince(start, appId);
        Double avgLatency = callLogMapper.averageLatencySince(start, appId);

        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setTodayCalls(todayCalls);
        summary.setSuccessCalls(successCalls);
        summary.setSuccessRate(todayCalls == 0
                ? 0D
                : Math.round(successCalls * 10000D / todayCalls) / 100D);
        summary.setAvgLatencyMs(avgLatency == null ? 0D : Math.round(avgLatency * 100D) / 100D);
        summary.setActiveApps(callLogMapper.countDistinctAppsSince(start, appId));
        summary.setTopApis(enrichTopApis(callLogMapper.topApisSince(start, appId)));
        return summary;
    }

    private List<Map<String, Object>> enrichTopApis(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> apiIds = rows.stream()
                .map(row -> toLong(row.get("apiId")))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> names = new HashMap<>();
        if (!apiIds.isEmpty()) {
            for (ApiDefinition api : apiDefinitionMapper.selectBatchIds(apiIds)) {
                names.put(api.getId(), api.getName());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Long apiId = toLong(row.get("apiId"));
            Map<String, Object> item = new HashMap<>();
            item.put("apiId", apiId);
            item.put("apiName", apiId == null ? null : names.getOrDefault(apiId, "未知 API"));
            item.put("callCount", toLong(row.get("callCount")));
            result.add(item);
        }
        return result;
    }

    private Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}
```

## OpenApiProxyServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/OpenApiProxyServiceImpl.java](../../src/main/java/com/agentgate/service/impl/OpenApiProxyServiceImpl.java)

校验方法和上游地址，拼接路径后缀/query，复制有限请求头，读取缓存 body，同步转发；按敏感级别脱敏 JSON，再按上限截断响应。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private boolean allowLoopback;
    private int maxBodyBytes;
    public ProxyResponse forward(HttpServletRequest request, ApiDefinition api, String traceId) {
            throw new BizException(ErrorCode.METHOD_NOT_ALLOWED,
            throw new BizException(ErrorCode.METHOD_NOT_ALLOWED);
    private String buildTargetUrl(HttpServletRequest request, ApiDefinition api) {
    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String name) {
    private byte[] readBody(HttpServletRequest request) {
            throw new BizException(ErrorCode.PARAM_ERROR, "读取请求体失败");
    private Charset resolveCharset(String contentType) {
```

### 完整代码

```java
package com.agentgate.service.impl;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.service.OpenApiProxyService;
import com.agentgate.dto.ProxyResponse;
import com.agentgate.utils.CachedBodyHttpServletRequest;
import com.agentgate.utils.MaskUtils;
import com.agentgate.utils.SsrfUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OpenApiProxyServiceImpl implements OpenApiProxyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agentgate.ssrf.allow-loopback:false}")
    private boolean allowLoopback;

    @Value("${agentgate.openapi.max-body-bytes:1048576}")
    private int maxBodyBytes;

    @Override
    public ProxyResponse forward(HttpServletRequest request, ApiDefinition api, String traceId) {
        String method = request.getMethod();
        if (StringUtils.hasText(api.getMethod())
                && !"ANY".equalsIgnoreCase(api.getMethod())
                && !api.getMethod().equalsIgnoreCase(method)) {
            throw new BizException(ErrorCode.METHOD_NOT_ALLOWED,
                    "该 API 仅支持 " + api.getMethod());
        }
        // 每次转发前重新校验，降低 DNS 重绑定绕过风险
        SsrfUtils.validate(api.getUpstreamUrl(), allowLoopback);

        String targetUrl = buildTargetUrl(request, api);
        HttpMethod httpMethod = HttpMethod.resolve(method);
        if (httpMethod == null) {
            throw new BizException(ErrorCode.METHOD_NOT_ALLOWED);
        }

        HttpHeaders headers = new HttpHeaders();
        copyHeader(request, headers, HttpHeaders.CONTENT_TYPE);
        copyHeader(request, headers, HttpHeaders.ACCEPT);
        copyHeader(request, headers, HttpHeaders.USER_AGENT);
        headers.set("X-Trace-Id", traceId);

        byte[] body = readBody(request);
        HttpEntity<byte[]> entity = new HttpEntity<>(body.length == 0 ? null : body, headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(targetUrl, httpMethod, entity, byte[].class);
        byte[] responseBody = response.getBody() == null ? new byte[0] : response.getBody();
        String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

        if (api.getSensitivityLevel() != null && api.getSensitivityLevel() >= 2
                && contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) {
            Charset charset = resolveCharset(contentType);
            String json = new String(responseBody, charset);
            responseBody = MaskUtils.maskJson(json, objectMapper).getBytes(charset);
        }
        if (responseBody.length > maxBodyBytes) {
            responseBody = Arrays.copyOf(responseBody, maxBodyBytes);
        }

        ProxyResponse proxyResponse = new ProxyResponse();
        proxyResponse.setStatusCode(response.getStatusCodeValue());
        proxyResponse.setContentType(contentType);
        proxyResponse.setBody(responseBody);
        return proxyResponse;
    }

    private String buildTargetUrl(HttpServletRequest request, ApiDefinition api) {
        String upstream = api.getUpstreamUrl().trim();
        String prefix = request.getContextPath() + "/openapi/" + api.getCode();
        String requestUri = request.getRequestURI();
        String tail = requestUri.startsWith(prefix) ? requestUri.substring(prefix.length()) : "";
        if (StringUtils.hasText(tail)) {
            if (upstream.endsWith("/") && tail.startsWith("/")) {
                tail = tail.substring(1);
            } else if (!upstream.endsWith("/") && !tail.startsWith("/")) {
                tail = "/" + tail;
            }
            upstream = upstream + tail;
        }
        String query = request.getQueryString();
        if (StringUtils.hasText(query)) {
            upstream = upstream + (upstream.contains("?") ? "&" : "?") + query;
        }
        return upstream;
    }

    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (StringUtils.hasText(value)) {
            headers.set(name, value);
        }
    }

    private byte[] readBody(HttpServletRequest request) {
        if (request instanceof CachedBodyHttpServletRequest) {
            return ((CachedBodyHttpServletRequest) request).getCachedBody();
        }
        try {
            return StreamUtils.copyToByteArray(request.getInputStream());
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "读取请求体失败");
        }
    }

    private Charset resolveCharset(String contentType) {
        try {
            Charset charset = MediaType.parseMediaType(contentType).getCharset();
            return charset == null ? StandardCharsets.UTF_8 : charset;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
}
```

## QuotaPlanServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/QuotaPlanServiceImpl.java](../../src/main/java/com/agentgate/service/impl/QuotaPlanServiceImpl.java)

负责套餐创建、修改、列表及按 ID 查询，DTO 到实体赋值；缺少套餐抛业务异常，配额参数由 DTO 约束。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final QuotaPlanMapper quotaPlanMapper;
    public QuotaPlan create(QuotaPlanDTO dto) {
    public QuotaPlan update(Long id, QuotaPlanDTO dto) {
    public List<QuotaPlan> listAll() {
    public QuotaPlan getById(Long id) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
    private void copy(QuotaPlanDTO dto, QuotaPlan plan) {
```

### 完整代码

```java
package com.agentgate.service.impl;

import com.agentgate.dto.QuotaPlanDTO;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.QuotaPlanMapper;
import com.agentgate.service.QuotaPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuotaPlanServiceImpl implements QuotaPlanService {

    private final QuotaPlanMapper quotaPlanMapper;

    @Override
    public QuotaPlan create(QuotaPlanDTO dto) {
        QuotaPlan plan = new QuotaPlan();
        copy(dto, plan);
        quotaPlanMapper.insert(plan);
        return plan;
    }

    @Override
    public QuotaPlan update(Long id, QuotaPlanDTO dto) {
        QuotaPlan plan = getById(id);
        copy(dto, plan);
        quotaPlanMapper.updateById(plan);
        return plan;
    }

    @Override
    public List<QuotaPlan> listAll() {
        return quotaPlanMapper.selectList(
                new LambdaQueryWrapper<QuotaPlan>().orderByAsc(QuotaPlan::getId));
    }

    @Override
    public QuotaPlan getById(Long id) {
        QuotaPlan plan = quotaPlanMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
        }
        return plan;
    }

    private void copy(QuotaPlanDTO dto, QuotaPlan plan) {
        plan.setName(dto.getName());
        plan.setQpsLimit(dto.getQpsLimit());
        plan.setDailyQuota(dto.getDailyQuota());
        plan.setMonthlyQuota(dto.getMonthlyQuota());
        plan.setDescription(dto.getDescription());
    }
}
```

## RateLimitServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/RateLimitServiceImpl.java](../../src/main/java/com/agentgate/service/impl/RateLimitServiceImpl.java)

PostConstruct 加载单个 Lua 脚本；一次执行内原子完成令牌桶补充/扣减与日/月配额检查/双增；计算日/月末 TTL 并将脚本返回码转成业务异常。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String CONSUME_SCRIPT_PATH = "lua/rate_limit_consume.lua";
    private final StringRedisTemplate redisTemplate;
    private Clock clock = Clock.systemDefaultZone();
    private DefaultRedisScript<Long> consumeScript;
    public void init() {
    public void checkAndConsume(Long appId, ApiDefinition api, QuotaPlan plan) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
            throw new BizException(ErrorCode.PARAM_ERROR);
            throw new BizException(ErrorCode.SYSTEM_ERROR);
            throw new BizException(ErrorCode.RATE_LIMITED);
            throw new BizException(ErrorCode.QUOTA_EXCEEDED);
```

### 完整代码

```java
package com.agentgate.service.impl;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.service.RateLimitService;
import com.agentgate.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String CONSUME_SCRIPT_PATH = "lua/rate_limit_consume.lua";

    private final StringRedisTemplate redisTemplate;

    private Clock clock = Clock.systemDefaultZone();

    private DefaultRedisScript<Long> consumeScript;

    @PostConstruct
    public void init() {
        consumeScript = new DefaultRedisScript<>();
        consumeScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(CONSUME_SCRIPT_PATH)));
        consumeScript.setResultType(Long.class);
    }

    @Override
    public void checkAndConsume(Long appId, ApiDefinition api, QuotaPlan plan) {
        if (plan == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
        }
        if (appId == null || api == null || api.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }

        Integer qpsLimit = plan.getQpsLimit();
        Integer dailyQuota = plan.getDailyQuota();
        Integer monthlyQuota = plan.getMonthlyQuota();
        if (qpsLimit == null || qpsLimit <= 0
                || dailyQuota == null || dailyQuota <= 0
                || monthlyQuota == null || monthlyQuota <= 0) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND, "配额套餐配置不完整");
        }

        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        ZoneId zone = clock.getZone();

        String rateKey = RedisConstants.RATE_LIMIT_KEY + appId + ":" + api.getId();
        String dayKey = RedisConstants.QUOTA_DAY_KEY + appId + ":" + api.getId()
                + ":" + today.format(DAY_FORMAT);
        String monthKey = RedisConstants.QUOTA_MONTH_KEY + appId + ":" + api.getId()
                + ":" + today.format(MONTH_FORMAT);

        long dayTtl = Duration.between(now, today.plusDays(1).atStartOfDay(zone)).getSeconds() + 60;
        long monthTtl = Duration.between(now,
                today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone)).getSeconds() + 60;

        Long result = redisTemplate.execute(
                consumeScript,
                Arrays.asList(rateKey, dayKey, monthKey),
                String.valueOf(qpsLimit),
                String.valueOf(qpsLimit),
                String.valueOf(clock.millis()),
                "1",
                String.valueOf(dailyQuota),
                String.valueOf(monthlyQuota),
                String.valueOf(dayTtl),
                String.valueOf(monthTtl));

        if (result == null) {
            log.error("限流与配额脚本无返回，按系统错误拒绝 appId={} apiId={}", appId, api.getId());
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
        if (result == 1L) {
            return;
        }
        if (result == 0L) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
        if (result == -1L) {
            log.debug("日配额用尽 appId={} apiId={}", appId, api.getId());
            throw new BizException(ErrorCode.QUOTA_EXCEEDED);
        }
        if (result == -2L) {
            log.debug("月配额用尽 appId={} apiId={}", appId, api.getId());
            throw new BizException(ErrorCode.QUOTA_EXCEEDED);
        }
        log.error("限流与配额脚本返回非法值 result={} appId={} apiId={}", result, appId, api.getId());
        throw new BizException(ErrorCode.SYSTEM_ERROR);
    }
}
```

## SubscriptionServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/SubscriptionServiceImpl.java](../../src/main/java/com/agentgate/service/impl/SubscriptionServiceImpl.java)

申请核验应用归属、API 上架与套餐；联合唯一索引防重复；拒绝后可重申；审批条件更新防并发；getActive 缓存有效订阅，变更删缓存。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final SubscriptionMapper subscriptionMapper;
    private final AppService appService;
    private final ApiService apiService;
    private final QuotaPlanService quotaPlanService;
    private final CacheClient cacheClient;
    public Subscription apply(SubscribeDTO dto) {
            throw new BizException(ErrorCode.API_OFFLINE);
                throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
                throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
            return findOne(dto.getAppId(), dto.getApiId());
        throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
    public List<Subscription> listMine(Long appId) {
    public List<Subscription> listPending() {
    public List<Subscription> listAll() {
    public void approve(Long id, boolean approved, String reason) {
            throw new BizException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "只有待审批的订阅可以审批");
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "订阅状态已被其他操作修改");
    public Subscription getActive(Long appId, Long apiId) {
    private Subscription findOne(Long appId, Long apiId) {
    private void deleteCache(Long appId, Long apiId) {
```

### 完整代码

```java
package com.agentgate.service.impl;

import com.agentgate.dto.SubscribeDTO;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.App;
import com.agentgate.entity.Subscription;
import com.agentgate.enums.ApiStatus;
import com.agentgate.enums.ErrorCode;
import com.agentgate.enums.SubscriptionStatus;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.SubscriptionMapper;
import com.agentgate.service.ApiService;
import com.agentgate.service.AppService;
import com.agentgate.service.QuotaPlanService;
import com.agentgate.service.SubscriptionService;
import com.agentgate.utils.CacheClient;
import com.agentgate.utils.RedisConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final AppService appService;
    private final ApiService apiService;
    private final QuotaPlanService quotaPlanService;
    private final CacheClient cacheClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Subscription apply(SubscribeDTO dto) {
        App app = appService.getOwnedApp(dto.getAppId());
        ApiDefinition api = apiService.getById(dto.getApiId());
        if (!ApiStatus.PUBLISHED.name().equals(api.getStatus())) {
            throw new BizException(ErrorCode.API_OFFLINE);
        }
        quotaPlanService.getById(dto.getQuotaPlanId());

        Subscription existing = findOne(dto.getAppId(), dto.getApiId());
        if (existing == null) {
            Subscription subscription = new Subscription();
            subscription.setAppId(dto.getAppId());
            subscription.setApiId(dto.getApiId());
            subscription.setQuotaPlanId(dto.getQuotaPlanId());
            subscription.setStatus(SubscriptionStatus.PENDING.name());
            try {
                subscriptionMapper.insert(subscription);
            } catch (DuplicateKeyException e) {
                throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
            }
            deleteCache(dto.getAppId(), dto.getApiId());
            return subscription;
        }
        if (SubscriptionStatus.REJECTED.name().equals(existing.getStatus())) {
            int rows = subscriptionMapper.update(null, new LambdaUpdateWrapper<Subscription>()
                    .eq(Subscription::getId, existing.getId())
                    .eq(Subscription::getStatus, SubscriptionStatus.REJECTED.name())
                    .set(Subscription::getStatus, SubscriptionStatus.PENDING.name())
                    .set(Subscription::getQuotaPlanId, dto.getQuotaPlanId())
                    .set(Subscription::getRejectReason, null)
                    .set(Subscription::getUpdateTime, LocalDateTime.now()));
            if (rows == 0) {
                throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
            }
            deleteCache(dto.getAppId(), dto.getApiId());
            return findOne(dto.getAppId(), dto.getApiId());
        }
        throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
    }

    @Override
    public List<Subscription> listMine(Long appId) {
        App app = appService.getOwnedApp(appId);
        return subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getAppId, app.getId())
                        .orderByDesc(Subscription::getCreateTime));
    }

    @Override
    public List<Subscription> listPending() {
        return subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getStatus, SubscriptionStatus.PENDING.name())
                        .orderByAsc(Subscription::getCreateTime));
    }

    @Override
    public List<Subscription> listAll() {
        return subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .orderByDesc(Subscription::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, boolean approved, String reason) {
        Subscription subscription = subscriptionMapper.selectById(id);
        if (subscription == null) {
            throw new BizException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
        if (!SubscriptionStatus.PENDING.name().equals(subscription.getStatus())) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "只有待审批的订阅可以审批");
        }
        SubscriptionStatus target = approved ? SubscriptionStatus.ACTIVE : SubscriptionStatus.REJECTED;
        int rows = subscriptionMapper.update(null, new LambdaUpdateWrapper<Subscription>()
                .eq(Subscription::getId, id)
                .eq(Subscription::getStatus, SubscriptionStatus.PENDING.name())
                .set(Subscription::getStatus, target.name())
                .set(Subscription::getRejectReason, approved ? null : reason)
                .set(Subscription::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "订阅状态已被其他操作修改");
        }
        deleteCache(subscription.getAppId(), subscription.getApiId());
    }

    @Override
    public Subscription getActive(Long appId, Long apiId) {
        String key = RedisConstants.CACHE_SUB_KEY + appId + ":" + apiId;
        return cacheClient.getOrLoad(key, Subscription.class,
                RedisConstants.CACHE_SUB_TTL_MINUTES, () -> subscriptionMapper.selectOne(
                        new LambdaQueryWrapper<Subscription>()
                                .eq(Subscription::getAppId, appId)
                                .eq(Subscription::getApiId, apiId)
                                .eq(Subscription::getStatus, SubscriptionStatus.ACTIVE.name())
                                .last("limit 1")));
    }

    private Subscription findOne(Long appId, Long apiId) {
        return subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getAppId, appId)
                        .eq(Subscription::getApiId, apiId)
                        .last("limit 1"));
    }

    private void deleteCache(Long appId, Long apiId) {
        cacheClient.delete(RedisConstants.CACHE_SUB_KEY + appId + ":" + apiId);
    }
}
```

## UserServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/UserServiceImpl.java](../../src/main/java/com/agentgate/service/impl/UserServiceImpl.java)

注册查重并 BCrypt 存密码；登录校验密码和 ACTIVE 状态，生成 UUID Token 写 Redis Hash；登出删 key；me 从 UserHolder 取用户。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    public void register(RegisterFormDTO dto) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
            throw new BizException(ErrorCode.USERNAME_EXISTS);
    public LoginResultDTO login(LoginFormDTO dto) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        return new LoginResultDTO(token, userDTO);
    public void logout(String token) {
    public UserDTO me() {
            throw new BizException(ErrorCode.UNAUTHORIZED);
```

### 完整代码

```java
package com.agentgate.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.agentgate.dto.LoginFormDTO;
import com.agentgate.dto.LoginResultDTO;
import com.agentgate.dto.RegisterFormDTO;
import com.agentgate.dto.UserDTO;
import com.agentgate.entity.User;
import com.agentgate.enums.ErrorCode;
import com.agentgate.enums.UserRole;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.UserMapper;
import com.agentgate.service.UserService;
import com.agentgate.utils.RedisConstants;
import com.agentgate.utils.UserHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void register(RegisterFormDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRole(UserRole.DEVELOPER.name());
        user.setStatus("ACTIVE");
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时唯一索引兜底
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
    }

    @Override
    public LoginResultDTO login(LoginFormDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .last("limit 1"));
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String key = RedisConstants.LOGIN_TOKEN_KEY + token;
        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(user.getId()));
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("role", user.getRole());
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, RedisConstants.LOGIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setNickname(user.getNickname());
        userDTO.setRole(user.getRole());
        return new LoginResultDTO(token, userDTO);
    }

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            redisTemplate.delete(RedisConstants.LOGIN_TOKEN_KEY + token);
        }
        UserHolder.removeUser();
    }

    @Override
    public UserDTO me() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
```

## CallLogDispatchService.java

源码：[src/main/java/com/agentgate/service/CallLogDispatchService.java](../../src/main/java/com/agentgate/service/CallLogDispatchService.java)

调用日志发送契约：优先投递 MQ；发送抛异常时委托 `CallLogService.saveDirect` 同步落库兜底。发送成功不代表消费者已最终落库，本方法不向主流程抛异常。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    void asyncSave(CallLogMessage message);
```

### 完整代码

```java
package com.agentgate.service;

import com.agentgate.dto.CallLogMessage;

public interface CallLogDispatchService {

    void asyncSave(CallLogMessage message);
}
```

## CallLogDispatchServiceImpl.java

源码：[src/main/java/com/agentgate/service/impl/CallLogDispatchServiceImpl.java](../../src/main/java/com/agentgate/service/impl/CallLogDispatchServiceImpl.java)

发送服务实现：`convertAndSend` 投递调用快照；发送异常时跨 Bean 调用 `CallLogService.saveDirect`，使事务代理生效；兜底失败只记日志。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final RabbitTemplate rabbitTemplate;
    private final CallLogService callLogService;
    public void asyncSave(CallLogMessage message) {
```

### 完整代码

```java
package com.agentgate.service.impl;

import com.agentgate.config.RabbitMQConfig;
import com.agentgate.dto.CallLogMessage;
import com.agentgate.service.CallLogDispatchService;
import com.agentgate.service.CallLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallLogDispatchServiceImpl implements CallLogDispatchService {

    private final RabbitTemplate rabbitTemplate;
    private final CallLogService callLogService;

    @Override
    public void asyncSave(CallLogMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CALL_LOG_EXCHANGE,
                    RabbitMQConfig.CALL_LOG_ROUTING_KEY,
                    message);
        } catch (Exception e) {
            log.warn("调用日志发送 MQ 失败，改为同步落库 traceId={}", message.getTraceId(), e);
            try {
                callLogService.saveDirect(message);
            } catch (Exception fallbackError) {
                log.error("调用日志同步兜底失败 traceId={}", message.getTraceId(), fallbackError);
            }
        }
    }
}
```

## CallLogListener.java

源码：[src/main/java/com/agentgate/listener/CallLogListener.java](../../src/main/java/com/agentgate/listener/CallLogListener.java)

RabbitMQ 消费入口：跨 Bean 调用 `CallLogService.saveDirect`，事务提交成功后手动 ACK；失败 NACK 且不重新入原队列，消息进入配置的死信路由。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final CallLogService callLogService;
    public void onMessage(CallLogMessage message, Channel channel,
```

### 完整代码

```java
package com.agentgate.listener;

import com.agentgate.config.RabbitMQConfig;
import com.agentgate.dto.CallLogMessage;
import com.agentgate.service.CallLogService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 异步落库调用日志：手动 ACK，失败进入死信队列。
 * trace_id 唯一索引保证重复消息不会重复计数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CallLogListener {

    private final CallLogService callLogService;

    @RabbitListener(queues = RabbitMQConfig.CALL_LOG_QUEUE)
    public void onMessage(CallLogMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            callLogService.saveDirect(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("调用日志消费失败 traceId={}", message.getTraceId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

