# HTTP Controller

[返回源码索引](README.md) · [学习指南](../learning-guide.md)

## AdminApiController.java

源码：[src/main/java/com/agentgate/controller/AdminApiController.java](../../src/main/java/com/agentgate/controller/AdminApiController.java)

管理员 API 管理入口：列表、新增、修改、上架、下线；写操作带 AuditLog 注解，由切面记录成功或失败。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final ApiService apiService;
    public Result<List<ApiDefinition>> list() {
    public Result<ApiDefinition> create(@Valid @RequestBody ApiCreateDTO dto) {
    public Result<ApiDefinition> update(@PathVariable Long id,
    public Result<Void> publish(@PathVariable Long id) {
    public Result<Void> offline(@PathVariable Long id) {
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.annotation.AuditLog;
import com.agentgate.dto.ApiCreateDTO;
import com.agentgate.dto.Result;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/apis")
@RequiredArgsConstructor
public class AdminApiController {

    private final ApiService apiService;

    @GetMapping
    public Result<List<ApiDefinition>> list() {
        return Result.ok(apiService.listAll());
    }

    @PostMapping
    @AuditLog(action = "CREATE_API", resourceType = "API")
    public Result<ApiDefinition> create(@Valid @RequestBody ApiCreateDTO dto) {
        return Result.ok(apiService.create(dto));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_API", resourceType = "API")
    public Result<ApiDefinition> update(@PathVariable Long id,
                                        @Valid @RequestBody ApiCreateDTO dto) {
        return Result.ok(apiService.update(id, dto));
    }

    @PostMapping("/{id}/publish")
    @AuditLog(action = "PUBLISH_API", resourceType = "API")
    public Result<Void> publish(@PathVariable Long id) {
        apiService.publish(id);
        return Result.ok();
    }

    @PostMapping("/{id}/offline")
    @AuditLog(action = "OFFLINE_API", resourceType = "API")
    public Result<Void> offline(@PathVariable Long id) {
        apiService.offline(id);
        return Result.ok();
    }
}
```

## AdminAuditLogController.java

源码：[src/main/java/com/agentgate/controller/AdminAuditLogController.java](../../src/main/java/com/agentgate/controller/AdminAuditLogController.java)

分页查看管理操作审计，调用 AuditLogService；不同于开放接口调用日志。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final AuditLogService auditLogService;
    public Result<PageResult<AuditLog>> page(@RequestParam(defaultValue = "1") long page,
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.dto.PageResult;
import com.agentgate.dto.Result;
import com.agentgate.entity.AuditLog;
import com.agentgate.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public Result<PageResult<AuditLog>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(auditLogService.page(page, size));
    }
}
```

## AdminCallLogController.java

源码：[src/main/java/com/agentgate/controller/AdminCallLogController.java](../../src/main/java/com/agentgate/controller/AdminCallLogController.java)

管理员全局调用日志入口：分页和 traceId 查询时传 null 范围，表示不限制单个应用；依赖拦截器保证 ADMIN。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final CallLogService callLogService;
    public Result<PageResult<CallLog>> page(CallLogQueryDTO query) {
    public Result<CallLog> get(@PathVariable String traceId) {
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.dto.CallLogQueryDTO;
import com.agentgate.dto.PageResult;
import com.agentgate.dto.Result;
import com.agentgate.entity.CallLog;
import com.agentgate.service.CallLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/call-logs")
@RequiredArgsConstructor
public class AdminCallLogController {

    private final CallLogService callLogService;

    @GetMapping
    public Result<PageResult<CallLog>> page(CallLogQueryDTO query) {
        return Result.ok(callLogService.page(query, null));
    }

    @GetMapping("/{traceId}")
    public Result<CallLog> get(@PathVariable String traceId) {
        return Result.ok(callLogService.getByTraceId(traceId, null));
    }
}
```

## AdminDashboardController.java

源码：[src/main/java/com/agentgate/controller/AdminDashboardController.java](../../src/main/java/com/agentgate/controller/AdminDashboardController.java)

管理员全局看板，summary(null) 表示不按应用过滤。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final DashboardService dashboardService;
    public Result<DashboardSummaryDTO> summary() {
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.dto.DashboardSummaryDTO;
import com.agentgate.dto.Result;
import com.agentgate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardSummaryDTO> summary() {
        return Result.ok(dashboardService.summary(null));
    }
}
```

## AdminQuotaPlanController.java

源码：[src/main/java/com/agentgate/controller/AdminQuotaPlanController.java](../../src/main/java/com/agentgate/controller/AdminQuotaPlanController.java)

管理员套餐管理入口：列表、创建、更新；Valid 校验 DTO，AuditLog 记录写操作。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final QuotaPlanService quotaPlanService;
    public Result<List<QuotaPlan>> list() {
    public Result<QuotaPlan> create(@Valid @RequestBody QuotaPlanDTO dto) {
    public Result<QuotaPlan> update(@PathVariable Long id,
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.annotation.AuditLog;
import com.agentgate.dto.QuotaPlanDTO;
import com.agentgate.dto.Result;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.service.QuotaPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/quota-plans")
@RequiredArgsConstructor
public class AdminQuotaPlanController {

    private final QuotaPlanService quotaPlanService;

    @GetMapping
    public Result<List<QuotaPlan>> list() {
        return Result.ok(quotaPlanService.listAll());
    }

    @PostMapping
    @AuditLog(action = "CREATE_QUOTA_PLAN", resourceType = "QUOTA_PLAN")
    public Result<QuotaPlan> create(@Valid @RequestBody QuotaPlanDTO dto) {
        return Result.ok(quotaPlanService.create(dto));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_QUOTA_PLAN", resourceType = "QUOTA_PLAN")
    public Result<QuotaPlan> update(@PathVariable Long id,
                                    @Valid @RequestBody QuotaPlanDTO dto) {
        return Result.ok(quotaPlanService.update(id, dto));
    }
}
```

## AdminSubscriptionController.java

源码：[src/main/java/com/agentgate/controller/AdminSubscriptionController.java](../../src/main/java/com/agentgate/controller/AdminSubscriptionController.java)

管理员订阅列表及审批入口；传 PENDING 时查待审批，其他值走全部列表；ApproveDTO 表达同意与拒绝原因。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final SubscriptionService subscriptionService;
    public Result<List<Subscription>> list(
    public Result<Void> approve(@PathVariable Long id,
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.annotation.AuditLog;
import com.agentgate.dto.ApproveDTO;
import com.agentgate.dto.Result;
import com.agentgate.entity.Subscription;
import com.agentgate.enums.SubscriptionStatus;
import com.agentgate.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public Result<List<Subscription>> list(
            @RequestParam(required = false) String status) {
        if (SubscriptionStatus.PENDING.name().equals(status)) {
            return Result.ok(subscriptionService.listPending());
        }
        return Result.ok(subscriptionService.listAll());
    }

    @PostMapping("/{id}/approve")
    @AuditLog(action = "APPROVE_SUBSCRIPTION", resourceType = "SUBSCRIPTION")
    public Result<Void> approve(@PathVariable Long id,
                                @Valid @RequestBody ApproveDTO dto) {
        subscriptionService.approve(id, dto.getApproved(), dto.getReason());
        return Result.ok();
    }
}
```

## DeveloperApiController.java

源码：[src/main/java/com/agentgate/controller/DeveloperApiController.java](../../src/main/java/com/agentgate/controller/DeveloperApiController.java)

开发者 API 列表入口，仅返回已上架 API；调用 ApiService.listPublished。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final ApiService apiService;
    public Result<List<ApiDefinition>> listPublished() {
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.dto.Result;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/developer/apis")
@RequiredArgsConstructor
public class DeveloperApiController {

    private final ApiService apiService;

    @GetMapping
    public Result<List<ApiDefinition>> listPublished() {
        return Result.ok(apiService.listPublished());
    }
}
```

## DeveloperAppController.java

源码：[src/main/java/com/agentgate/controller/DeveloperAppController.java](../../src/main/java/com/agentgate/controller/DeveloperAppController.java)

开发者应用入口：创建、列出本人应用、重置 Secret、修改白名单、禁用。ID 路径参数不能代替归属校验，归属在 AppService 实现。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final AppService appService;
    public Result<AppSecretDTO> create(@Valid @RequestBody AppCreateDTO dto) {
    public Result<List<App>> list() {
    public Result<AppSecretDTO> resetSecret(@PathVariable Long id) {
    public Result<Void> updateIpWhitelist(@PathVariable Long id,
    public Result<Void> disable(@PathVariable Long id) {
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.dto.AppCreateDTO;
import com.agentgate.dto.AppSecretDTO;
import com.agentgate.dto.IpWhitelistDTO;
import com.agentgate.dto.Result;
import com.agentgate.entity.App;
import com.agentgate.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/developer/apps")
@RequiredArgsConstructor
public class DeveloperAppController {

    private final AppService appService;

    @PostMapping
    public Result<AppSecretDTO> create(@Valid @RequestBody AppCreateDTO dto) {
        return Result.ok(appService.create(dto));
    }

    @GetMapping
    public Result<List<App>> list() {
        return Result.ok(appService.listMine());
    }

    @PostMapping("/{id}/reset-secret")
    public Result<AppSecretDTO> resetSecret(@PathVariable Long id) {
        return Result.ok(appService.resetSecret(id));
    }

    @PutMapping("/{id}/ip-whitelist")
    public Result<Void> updateIpWhitelist(@PathVariable Long id,
                                          @Valid @RequestBody IpWhitelistDTO dto) {
        appService.updateIpWhitelist(id, dto.getIpWhitelist());
        return Result.ok();
    }

    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        appService.disable(id);
        return Result.ok();
    }
}
```

## DeveloperCallLogController.java

源码：[src/main/java/com/agentgate/controller/DeveloperCallLogController.java](../../src/main/java/com/agentgate/controller/DeveloperCallLogController.java)

查询前通过 getOwnedApp 校验 appId 属于当前用户，再把 appId 作为日志查询范围，避免越权访问。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final CallLogService callLogService;
    private final AppService appService;
    public Result<PageResult<CallLog>> page(CallLogQueryDTO query,
    public Result<CallLog> get(@PathVariable String traceId, @RequestParam Long appId) {
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.dto.CallLogQueryDTO;
import com.agentgate.dto.PageResult;
import com.agentgate.dto.Result;
import com.agentgate.entity.CallLog;
import com.agentgate.service.AppService;
import com.agentgate.service.CallLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开发者只能查询自己应用产生的调用日志。
 */
@RestController
@RequestMapping("/developer/call-logs")
@RequiredArgsConstructor
public class DeveloperCallLogController {

    private final CallLogService callLogService;
    private final AppService appService;

    @GetMapping
    public Result<PageResult<CallLog>> page(CallLogQueryDTO query,
                                            @RequestParam Long appId) {
        appService.getOwnedApp(appId);
        return Result.ok(callLogService.page(query, appId));
    }

    @GetMapping("/{traceId}")
    public Result<CallLog> get(@PathVariable String traceId, @RequestParam Long appId) {
        appService.getOwnedApp(appId);
        return Result.ok(callLogService.getByTraceId(traceId, appId));
    }
}
```

## DeveloperDashboardController.java

源码：[src/main/java/com/agentgate/controller/DeveloperDashboardController.java](../../src/main/java/com/agentgate/controller/DeveloperDashboardController.java)

先验证应用归属，再获取该应用看板；不能信任客户端随意传来的 appId。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final DashboardService dashboardService;
    private final AppService appService;
    public Result<DashboardSummaryDTO> summary(@RequestParam Long appId) {
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.dto.DashboardSummaryDTO;
import com.agentgate.dto.Result;
import com.agentgate.service.AppService;
import com.agentgate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/developer/dashboard")
@RequiredArgsConstructor
public class DeveloperDashboardController {

    private final DashboardService dashboardService;
    private final AppService appService;

    @GetMapping
    public Result<DashboardSummaryDTO> summary(@RequestParam Long appId) {
        appService.getOwnedApp(appId);
        return Result.ok(dashboardService.summary(appId));
    }
}
```

## DeveloperSubscriptionController.java

源码：[src/main/java/com/agentgate/controller/DeveloperSubscriptionController.java](../../src/main/java/com/agentgate/controller/DeveloperSubscriptionController.java)

接收订阅申请及 appId 列表查询，委托 SubscriptionService 校验应用所有者并处理审批状态。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final SubscriptionService subscriptionService;
    public Result<Subscription> apply(@Valid @RequestBody SubscribeDTO dto) {
    public Result<List<Subscription>> list(@RequestParam Long appId) {
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.dto.Result;
import com.agentgate.dto.SubscribeDTO;
import com.agentgate.entity.Subscription;
import com.agentgate.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/developer/subscriptions")
@RequiredArgsConstructor
public class DeveloperSubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public Result<Subscription> apply(@Valid @RequestBody SubscribeDTO dto) {
        return Result.ok(subscriptionService.apply(dto));
    }

    @GetMapping
    public Result<List<Subscription>> list(@RequestParam Long appId) {
        return Result.ok(subscriptionService.listMine(appId));
    }
}
```

## MockApiController.java

源码：[src/main/java/com/agentgate/controller/MockApiController.java](../../src/main/java/com/agentgate/controller/MockApiController.java)

本机模拟上游：weather 返回天气，order/user 返回含敏感字段的演示数据；无登录鉴权，仅用于本地演示，不是真实第三方服务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    public Map<String, Object> weather(@RequestParam(defaultValue = "beijing") String city) {
    public Map<String, Object> order(@PathVariable String id) {
    public Map<String, Object> user(@PathVariable String id) {
```

### 完整代码

```java
package com.agentgate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 演示用上游服务：供 /openapi/** 转发，不参与鉴权。
 */
@RestController
@RequestMapping("/mock-api")
public class MockApiController {

    @GetMapping("/weather")
    public Map<String, Object> weather(@RequestParam(defaultValue = "beijing") String city) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("city", city);
        data.put("weather", "sunny");
        data.put("temperature", 26);
        data.put("humidity", 45);
        data.put("provider", "mock-upstream");
        return data;
    }

    @GetMapping("/order/{id}")
    public Map<String, Object> order(@PathVariable String id) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", id);
        data.put("status", "PAID");
        data.put("amount", 199.00);
        data.put("phone", "13812345678");
        data.put("email", "buyer@example.com");
        data.put("address", "北京市朝阳区某某街道 100 号");
        return data;
    }

    @GetMapping("/user/{id}")
    public Map<String, Object> user(@PathVariable String id) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", id);
        data.put("name", "张三");
        data.put("mobile", "13900001111");
        data.put("idCard", "110101199001011234");
        data.put("email", "zhangsan@example.com");
        data.put("bankCard", "6222020202020202020");
        data.put("address", "上海市浦东新区某某路 88 号");
        return data;
    }
}
```

## OpenApiController.java

源码：[src/main/java/com/agentgate/controller/OpenApiController.java](../../src/main/java/com/agentgate/controller/OpenApiController.java)

读取 Filter 已设置的 API、traceId、IP 属性，调用代理服务；将连接或超时异常转业务错误；返回上游状态、类型及字节响应。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final OpenApiProxyService openApiProxyService;
    public ResponseEntity<byte[]> forward(HttpServletRequest request) {
            throw new BizException(timeout ? ErrorCode.UPSTREAM_TIMEOUT : ErrorCode.UPSTREAM_ERROR);
            throw new BizException(ErrorCode.UPSTREAM_ERROR);
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.filter.SignatureFilter;
import com.agentgate.service.OpenApiProxyService;
import com.agentgate.dto.ProxyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.servlet.http.HttpServletRequest;
import java.net.SocketTimeoutException;

/**
 * 开放接口入口。鉴权、限流、订阅校验已经在 SignatureFilter 完成，这里只负责转发。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OpenApiController {

    private final OpenApiProxyService openApiProxyService;

    @RequestMapping("/openapi/**")
    public ResponseEntity<byte[]> forward(HttpServletRequest request) {
        ApiDefinition api = (ApiDefinition) request.getAttribute(SignatureFilter.ATTR_API);
        String traceId = (String) request.getAttribute(SignatureFilter.ATTR_TRACE_ID);

        ProxyResponse proxyResponse;
        try {
            proxyResponse = openApiProxyService.forward(request, api, traceId);
        } catch (ResourceAccessException e) {
            log.warn("上游访问失败 traceId={} api={} error={}", traceId, api.getCode(), e.toString());
            boolean timeout = e.getCause() instanceof SocketTimeoutException;
            throw new BizException(timeout ? ErrorCode.UPSTREAM_TIMEOUT : ErrorCode.UPSTREAM_ERROR);
        } catch (RestClientException e) {
            log.warn("上游返回异常 traceId={} api={} error={}", traceId, api.getCode(), e.toString());
            throw new BizException(ErrorCode.UPSTREAM_ERROR);
        }

        HttpHeaders headers = new HttpHeaders();
        if (proxyResponse.getContentType() != null) {
            headers.set(HttpHeaders.CONTENT_TYPE, proxyResponse.getContentType());
        }
        return ResponseEntity.status(proxyResponse.getStatusCode())
                .headers(headers)
                .body(proxyResponse.getBody());
    }
}
```

## UserController.java

源码：[src/main/java/com/agentgate/controller/UserController.java](../../src/main/java/com/agentgate/controller/UserController.java)

用户 HTTP 入口：接收注册/登录 DTO，触发校验，调用 UserService；提供登出及当前用户查询。业务规则在实现类。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final UserService userService;
    public Result<Void> register(@Valid @RequestBody RegisterFormDTO dto) {
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginFormDTO dto) {
    public Result<Void> logout(@RequestHeader(value = "authorization", required = false) String token) {
    public Result<UserDTO> me() {
```

### 完整代码

```java
package com.agentgate.controller;

import com.agentgate.dto.LoginFormDTO;
import com.agentgate.dto.LoginResultDTO;
import com.agentgate.dto.RegisterFormDTO;
import com.agentgate.dto.Result;
import com.agentgate.dto.UserDTO;
import com.agentgate.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterFormDTO dto) {
        userService.register(dto);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginFormDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "authorization", required = false) String token) {
        userService.logout(token);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserDTO> me() {
        return Result.ok(userService.me());
    }
}
```

