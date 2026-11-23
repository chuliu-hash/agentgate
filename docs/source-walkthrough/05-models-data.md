# 实体、DTO、枚举与数据库访问

[返回源码索引](README.md) · [学习指南](../learning-guide.md)

## ApiDefinition.java

源码：[src/main/java/com/agentgate/entity/ApiDefinition.java](../../src/main/java/com/agentgate/entity/ApiDefinition.java)

数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private String code;
    private String name;
    private String method;
    private String upstreamUrl;
    private String version;
    private Integer sensitivityLevel;
    private String status;
    private Integer timeoutMs;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
```

### 完整代码

```java
package com.agentgate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_api")
public class ApiDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 对外暴露的稳定编码，如 weather */
    private String code;
    private String name;
    /** GET / POST / PUT / DELETE / ANY */
    private String method;
    /** 上游基础地址，如 http://127.0.0.1:8081/mock-api/weather */
    private String upstreamUrl;
    private String version;
    /** 1 普通，2 敏感（响应脱敏），3 高敏 */
    private Integer sensitivityLevel;
    private String status;
    private Integer timeoutMs;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

## App.java

源码：[src/main/java/com/agentgate/entity/App.java](../../src/main/java/com/agentgate/entity/App.java)

数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private String name;
    private String appKey;
    private String appSecretCipher;
    private String status;
    private String ipWhitelist;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
```

### 完整代码

```java
package com.agentgate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_app")
public class App implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String appKey;
    @JsonIgnore
    private String appSecretCipher;
    private String status;
    private String ipWhitelist;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

## AuditLog.java

源码：[src/main/java/com/agentgate/entity/AuditLog.java](../../src/main/java/com/agentgate/entity/AuditLog.java)

数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private String ip;
    private LocalDateTime createTime;
```

### 完整代码

```java
package com.agentgate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

## CallLog.java

源码：[src/main/java/com/agentgate/entity/CallLog.java](../../src/main/java/com/agentgate/entity/CallLog.java)

数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private String traceId;
    private Long appId;
    private Long apiId;
    private String method;
    private String path;
    private Integer statusCode;
    private Long latencyMs;
    private String clientIp;
    private Integer errorCode;
    private Integer requestSize;
    private Integer responseSize;
    private LocalDateTime createTime;
```

### 完整代码

```java
package com.agentgate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_call_log")
public class CallLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private Long appId;
    private Long apiId;
    private String method;
    private String path;
    private Integer statusCode;
    private Long latencyMs;
    private String clientIp;
    private Integer errorCode;
    private Integer requestSize;
    private Integer responseSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

## QuotaPlan.java

源码：[src/main/java/com/agentgate/entity/QuotaPlan.java](../../src/main/java/com/agentgate/entity/QuotaPlan.java)

数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private Integer qpsLimit;
    private Integer dailyQuota;
    private Integer monthlyQuota;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
```

### 完整代码

```java
package com.agentgate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_quota_plan")
public class QuotaPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer qpsLimit;
    private Integer dailyQuota;
    private Integer monthlyQuota;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

## Subscription.java

源码：[src/main/java/com/agentgate/entity/Subscription.java](../../src/main/java/com/agentgate/entity/Subscription.java)

数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long appId;
    private Long apiId;
    private Long quotaPlanId;
    private String status;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
```

### 完整代码

```java
package com.agentgate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_subscription")
public class Subscription implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long appId;
    private Long apiId;
    private Long quotaPlanId;
    private String status;
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

## UsageStat.java

源码：[src/main/java/com/agentgate/entity/UsageStat.java](../../src/main/java/com/agentgate/entity/UsageStat.java)

数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long appId;
    private Long apiId;
    private LocalDate statDate;
    private Long callCount;
    private Long errorCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
```

### 完整代码

```java
package com.agentgate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tb_usage_stat")
public class UsageStat implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long appId;
    private Long apiId;
    private LocalDate statDate;
    private Long callCount;
    private Long errorCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

## User.java

源码：[src/main/java/com/agentgate/entity/User.java](../../src/main/java/com/agentgate/entity/User.java)

数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String role;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
```

### 完整代码

```java
package com.agentgate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String nickname;
    private String role;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

## ApiCreateDTO.java

源码：[src/main/java/com/agentgate/dto/ApiCreateDTO.java](../../src/main/java/com/agentgate/dto/ApiCreateDTO.java)

API 登记/修改请求，包含 code、方法、上游、敏感级别、版本、超时等约束；合法字段仍需 Service 校验 URL 与唯一性。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private String code;
    private String name;
    private String method;
    private String upstreamUrl;
    private String version = "v1";
    private Integer sensitivityLevel = 1;
    private Integer timeoutMs = 5000;
    private String description;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ApiCreateDTO {

    @NotBlank(message = "API 编码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,64}$", message = "API 编码只能包含字母、数字、下划线和中划线，长度 2-64")
    private String code;

    @NotBlank(message = "API 名称不能为空")
    @Size(max = 128, message = "API 名称最长 128 个字符")
    private String name;

    @NotBlank(message = "请求方法不能为空")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|ANY)$", message = "请求方法只能是 GET/POST/PUT/DELETE/ANY")
    private String method;

    @NotBlank(message = "上游地址不能为空")
    @Size(max = 512, message = "上游地址最长 512 个字符")
    private String upstreamUrl;

    @Size(max = 32, message = "版本号最长 32 个字符")
    private String version = "v1";

    @NotNull(message = "敏感级别不能为空")
    @Min(value = 1, message = "敏感级别最小为 1")
    @Max(value = 3, message = "敏感级别最大为 3")
    private Integer sensitivityLevel = 1;

    @Min(value = 100, message = "超时时间最小 100ms")
    @Max(value = 60000, message = "超时时间最大 60000ms")
    private Integer timeoutMs = 5000;

    @Size(max = 512, message = "描述最长 512 个字符")
    private String description;
}
```

## AppAuthInfo.java

源码：[src/main/java/com/agentgate/dto/AppAuthInfo.java](../../src/main/java/com/agentgate/dto/AppAuthInfo.java)

开放接口鉴权缓存模型，含应用 ID、所有者、AK、明文 SK、状态与白名单；供 Filter 验签，不是公开返回模型。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private String appKey;
    private String appSecret;
    private String status;
    private String ipWhitelist;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 开放接口鉴权所需的内部信息，包含解密后的 secret，禁止直接返回给客户端。
 */
@Data
public class AppAuthInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String appKey;
    private String appSecret;
    private String status;
    private String ipWhitelist;
}
```

## AppCreateDTO.java

源码：[src/main/java/com/agentgate/dto/AppCreateDTO.java](../../src/main/java/com/agentgate/dto/AppCreateDTO.java)

应用创建输入：名称和 IP 白名单，字段校验约束见源码；所有者从当前登录上下文取得，不能由客户端指定。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private String name;
    private String ipWhitelist;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AppCreateDTO {

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 64, message = "应用名称最长 64 个字符")
    private String name;

    @Size(max = 512, message = "IP 白名单最长 512 个字符")
    private String ipWhitelist;
}
```

## ApproveDTO.java

源码：[src/main/java/com/agentgate/dto/ApproveDTO.java](../../src/main/java/com/agentgate/dto/ApproveDTO.java)

审批输入，approved 决定通过或拒绝，reason 承载拒绝原因；状态流转由业务实现控制。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private Boolean approved;
    private String reason;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ApproveDTO {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Size(max = 256, message = "拒绝原因最长 256 个字符")
    private String reason;
}
```

## AppSecretDTO.java

源码：[src/main/java/com/agentgate/dto/AppSecretDTO.java](../../src/main/java/com/agentgate/dto/AppSecretDTO.java)

创建或重置应用 Secret 的输出，包含应用 ID、AK 和明文 SK；普通应用列表不应用它长期暴露密钥。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private Long appId;
    private String appKey;
    private String appSecret;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppSecretDTO {

    private Long appId;
    private String appKey;
    /** 仅在创建或重置时返回一次 */
    private String appSecret;
}
```

## CallLogMessage.java

源码：[src/main/java/com/agentgate/dto/CallLogMessage.java](../../src/main/java/com/agentgate/dto/CallLogMessage.java)

从请求 Filter 发给 MQ 的调用快照：traceId、appId/apiId、方法路径、状态耗时、IP、错误码和大小；不是请求/响应正文。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private String traceId;
    private Long appId;
    private Long apiId;
    private String method;
    private String path;
    private Integer statusCode;
    private Long latencyMs;
    private String clientIp;
    private Integer errorCode;
    private Integer requestSize;
    private Integer responseSize;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CallLogMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;
    private Long appId;
    private Long apiId;
    private String method;
    private String path;
    private Integer statusCode;
    private Long latencyMs;
    private String clientIp;
    private Integer errorCode;
    private Integer requestSize;
    private Integer responseSize;
}
```

## CallLogQueryDTO.java

源码：[src/main/java/com/agentgate/dto/CallLogQueryDTO.java](../../src/main/java/com/agentgate/dto/CallLogQueryDTO.java)

分页日志查询条件：应用/API/traceId、起止时间与页码大小；时间格式通过 DateTimeFormat 解析，实际页大小在 Service 限制。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private Long appId;
    private Long apiId;
    private String traceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long page = 1;
    private long size = 10;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class CallLogQueryDTO {

    private Long appId;
    private Long apiId;
    private String traceId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private long page = 1;
    private long size = 10;
}
```

## DashboardSummaryDTO.java

源码：[src/main/java/com/agentgate/dto/DashboardSummaryDTO.java](../../src/main/java/com/agentgate/dto/DashboardSummaryDTO.java)

看板输出：当日调用数、成功数/成功率、平均耗时、活跃应用和热门 API 列表，计算逻辑在 DashboardServiceImpl。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private long todayCalls;
    private long successCalls;
    private double successRate;
    private double avgLatencyMs;
    private long activeApps;
    private List<Map<String, Object>> topApis;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardSummaryDTO {

    private long todayCalls;
    private long successCalls;
    private double successRate;
    private double avgLatencyMs;
    private long activeApps;
    private List<Map<String, Object>> topApis;
}
```

## IpWhitelistDTO.java

源码：[src/main/java/com/agentgate/dto/IpWhitelistDTO.java](../../src/main/java/com/agentgate/dto/IpWhitelistDTO.java)

白名单修改输入，限制字符串长度；具体匹配格式由 IpUtils 实现，注解不提供网络地址验证。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private String ipWhitelist;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class IpWhitelistDTO {

    @Size(max = 512, message = "IP 白名单最长 512 个字符")
    private String ipWhitelist;
}
```

## LoginFormDTO.java

源码：[src/main/java/com/agentgate/dto/LoginFormDTO.java](../../src/main/java/com/agentgate/dto/LoginFormDTO.java)

接收用户名和密码，声明非空校验；不是数据库实体，也不保存登录状态。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private String username;
    private String password;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginFormDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

## LoginResultDTO.java

源码：[src/main/java/com/agentgate/dto/LoginResultDTO.java](../../src/main/java/com/agentgate/dto/LoginResultDTO.java)

登录结果：Token 与 UserDTO；客户端应保存 Token 并放入后续 Authorization 请求头。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private String token;
    private UserDTO user;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultDTO {

    private String token;
    private UserDTO user;
}
```

## PageResult.java

源码：[src/main/java/com/agentgate/dto/PageResult.java](../../src/main/java/com/agentgate/dto/PageResult.java)

将 MyBatis IPage 或记录列表转换为 records/total/current/size，统一分页 JSON 形状。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private List<T> records;
    private long total;
    private long current;
    private long size;
    public static <T> PageResult<T> of(IPage<T> page) {
    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
```

### 完整代码

```java
package com.agentgate.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;
    private long total;
    private long current;
    private long size;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        return result;
    }
}
```

## QuotaPlanDTO.java

源码：[src/main/java/com/agentgate/dto/QuotaPlanDTO.java](../../src/main/java/com/agentgate/dto/QuotaPlanDTO.java)

套餐名称、QPS、日/月配额和说明，NotNull/Min 保证计数上限为正，名称长度受约束。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private String name;
    private Integer qpsLimit;
    private Integer dailyQuota;
    private Integer monthlyQuota;
    private String description;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class QuotaPlanDTO {

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 64, message = "套餐名称最长 64 个字符")
    private String name;

    @NotNull(message = "QPS 上限不能为空")
    @Min(value = 1, message = "QPS 上限至少为 1")
    private Integer qpsLimit;

    @NotNull(message = "日配额不能为空")
    @Min(value = 1, message = "日配额至少为 1")
    private Integer dailyQuota;

    @NotNull(message = "月配额不能为空")
    @Min(value = 1, message = "月配额至少为 1")
    private Integer monthlyQuota;

    @Size(max = 256, message = "描述最长 256 个字符")
    private String description;
}
```

## RegisterFormDTO.java

源码：[src/main/java/com/agentgate/dto/RegisterFormDTO.java](../../src/main/java/com/agentgate/dto/RegisterFormDTO.java)

接收注册用户名、密码、昵称，使用 NotBlank/Size 声明必填和长度约束；配合 Controller 的 Valid 生效。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private String username;
    private String password;
    private String nickname;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterFormDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    private String password;

    @Size(max = 32, message = "昵称最长 32 个字符")
    private String nickname;
}
```

## Result.java

源码：[src/main/java/com/agentgate/dto/Result.java](../../src/main/java/com/agentgate/dto/Result.java)

管理接口统一包装 success/code/message/data/timestamp；静态 ok/fail 创建结果，业务 code 不总是 HTTP 状态。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private boolean success;
    private int code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();
    public static <T> Result<T> ok() {
        return ok(null);
    public static <T> Result<T> ok(T data) {
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    public static <T> Result<T> fail(int code, String message) {
```

### 完整代码

```java
package com.agentgate.dto;

import com.agentgate.enums.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private int code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMessage(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
```

## SubscribeDTO.java

源码：[src/main/java/com/agentgate/dto/SubscribeDTO.java](../../src/main/java/com/agentgate/dto/SubscribeDTO.java)

订阅申请输入：appId、apiId、quotaPlanId，均非空；归属、存在性和发布状态必须额外查询判断。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private Long appId;
    private Long apiId;
    private Long quotaPlanId;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class SubscribeDTO {

    @NotNull(message = "应用 ID 不能为空")
    private Long appId;

    @NotNull(message = "API ID 不能为空")
    private Long apiId;

    @NotNull(message = "配额套餐 ID 不能为空")
    private Long quotaPlanId;
}
```

## UserDTO.java

源码：[src/main/java/com/agentgate/dto/UserDTO.java](../../src/main/java/com/agentgate/dto/UserDTO.java)

无密码的登录用户快照，用于返回客户端、Redis 会话恢复与 ThreadLocal 用户上下文。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String nickname;
    private String role;
```

### 完整代码

```java
package com.agentgate.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String role;
}
```

## ApiStatus.java

源码：[src/main/java/com/agentgate/enums/ApiStatus.java](../../src/main/java/com/agentgate/enums/ApiStatus.java)

DRAFT/PUBLISHED/OFFLINE 及 canTransitionTo 规则；业务实现还要用条件 SQL 防并发覆盖。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    public boolean canTransitionTo(ApiStatus target) {
```

### 完整代码

```java
package com.agentgate.enums;

public enum ApiStatus {
    DRAFT,
    PUBLISHED,
    OFFLINE;

    public boolean canTransitionTo(ApiStatus target) {
        if (this == target) {
            return false;
        }
        switch (this) {
            case DRAFT:
                return target == PUBLISHED;
            case PUBLISHED:
                return target == OFFLINE;
            case OFFLINE:
                return target == PUBLISHED;
            default:
                return false;
        }
    }
}
```

## AppStatus.java

源码：[src/main/java/com/agentgate/enums/AppStatus.java](../../src/main/java/com/agentgate/enums/AppStatus.java)

应用 ACTIVE/DISABLED 状态枚举，Filter 拒绝非 ACTIVE 应用；没有自动恢复接口。


### 完整代码

```java
package com.agentgate.enums;

public enum AppStatus {
    ACTIVE,
    DISABLED
}
```

## ErrorCode.java

源码：[src/main/java/com/agentgate/enums/ErrorCode.java](../../src/main/java/com/agentgate/enums/ErrorCode.java)

成功、通用 HTTP 风格、签名、上游与业务错误编号及中文信息；异常处理代码另行映射 HTTP 状态。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    private final int code;
    private final String message;
```

### 完整代码

```java
package com.agentgate.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    BODY_TOO_LARGE(413, "请求体过大"),
    RATE_LIMITED(429, "请求过于频繁"),
    QUOTA_EXCEEDED(4290, "调用配额已用尽"),
    SYSTEM_ERROR(500, "系统内部错误"),

    // 开放接口鉴权
    MISSING_SIGNATURE_HEADER(1001, "缺少签名请求头"),
    TIMESTAMP_EXPIRED(1002, "请求时间戳已过期"),
    NONCE_REPLAYED(1003, "重复的 nonce，拒绝重放"),
    SIGNATURE_INVALID(1004, "签名校验失败"),
    APP_DISABLED(1005, "应用已禁用"),
    IP_NOT_ALLOWED(1006, "客户端 IP 不在白名单"),
    API_OFFLINE(1007, "API 未上架或已下线"),
    NOT_SUBSCRIBED(1008, "应用未订阅该 API"),
    SSRF_BLOCKED(1009, "上游地址不安全"),

    // 上游
    UPSTREAM_TIMEOUT(2001, "上游服务超时"),
    UPSTREAM_ERROR(2002, "上游服务不可用"),

    // 业务
    USERNAME_EXISTS(3001, "用户名已存在"),
    USER_NOT_FOUND(3002, "用户不存在"),
    PASSWORD_ERROR(3003, "用户名或密码错误"),
    APP_NOT_FOUND(3004, "应用不存在"),
    API_NOT_FOUND(3005, "API 不存在"),
    API_CODE_EXISTS(3006, "API 编码已存在"),
    SUBSCRIPTION_EXISTS(3007, "订阅已存在或正在审批中"),
    SUBSCRIPTION_NOT_FOUND(3008, "订阅不存在"),
    PLAN_NOT_FOUND(3009, "配额套餐不存在"),
    STATE_TRANSITION_INVALID(3010, "状态流转非法"),
    APP_ACCESS_DENIED(3011, "无权操作该应用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

## SubscriptionStatus.java

源码：[src/main/java/com/agentgate/enums/SubscriptionStatus.java](../../src/main/java/com/agentgate/enums/SubscriptionStatus.java)

PENDING/ACTIVE/REJECTED 及允许流转规则；枚举允许不代表 Controller 已暴露所有转换入口。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    public boolean canTransitionTo(SubscriptionStatus target) {
```

### 完整代码

```java
package com.agentgate.enums;

public enum SubscriptionStatus {
    PENDING,
    ACTIVE,
    REJECTED;

    public boolean canTransitionTo(SubscriptionStatus target) {
        if (this == target) {
            return false;
        }
        switch (this) {
            case PENDING:
                return target == ACTIVE || target == REJECTED;
            case ACTIVE:
                return target == REJECTED;
            case REJECTED:
                return target == PENDING;
            default:
                return false;
        }
    }
}
```

## UserRole.java

源码：[src/main/java/com/agentgate/enums/UserRole.java](../../src/main/java/com/agentgate/enums/UserRole.java)

ADMIN 与 DEVELOPER 角色常量，登录会话缓存角色，LoginInterceptor 保护管理员路径。


### 完整代码

```java
package com.agentgate.enums;

public enum UserRole {
    ADMIN,
    DEVELOPER
}
```

## ApiDefinitionMapper.java

源码：[src/main/java/com/agentgate/mapper/ApiDefinitionMapper.java](../../src/main/java/com/agentgate/mapper/ApiDefinitionMapper.java)

继承 BaseMapper<ApiDefinition> 的数据库访问接口。selectById/selectList/insert/update 等通用 SQL 由 MyBatis-Plus 提供；空接口表示无自定义 SQL，不表示不执行数据库操作。


### 完整代码

```java
package com.agentgate.mapper;

import com.agentgate.entity.ApiDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface ApiDefinitionMapper extends BaseMapper<ApiDefinition> {
}
```

## AppMapper.java

源码：[src/main/java/com/agentgate/mapper/AppMapper.java](../../src/main/java/com/agentgate/mapper/AppMapper.java)

继承 BaseMapper<App> 的数据库访问接口。selectById/selectList/insert/update 等通用 SQL 由 MyBatis-Plus 提供；空接口表示无自定义 SQL，不表示不执行数据库操作。


### 完整代码

```java
package com.agentgate.mapper;

import com.agentgate.entity.App;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface AppMapper extends BaseMapper<App> {
}
```

## AuditLogMapper.java

源码：[src/main/java/com/agentgate/mapper/AuditLogMapper.java](../../src/main/java/com/agentgate/mapper/AuditLogMapper.java)

继承 BaseMapper<AuditLog> 的数据库访问接口。selectById/selectList/insert/update 等通用 SQL 由 MyBatis-Plus 提供；空接口表示无自定义 SQL，不表示不执行数据库操作。


### 完整代码

```java
package com.agentgate.mapper;

import com.agentgate.entity.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
```

## CallLogMapper.java

源码：[src/main/java/com/agentgate/mapper/CallLogMapper.java](../../src/main/java/com/agentgate/mapper/CallLogMapper.java)

自定义 INSERT IGNORE 保证 trace_id 重复时不再次落库；提供 count/avg/distinct/group by 聚合查询，用参数绑定和可选 appId 条件限制统计范围。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    int insertIgnore(@Param("log") CallLog log);
    long countSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);
    long countSuccessSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);
    Double averageLatencySince(@Param("start") LocalDateTime start, @Param("appId") Long appId);
    long countDistinctAppsSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);
    List<Map<String, Object>> topApisSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);
```

### 完整代码

```java
package com.agentgate.mapper;

import com.agentgate.entity.CallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CallLogMapper extends BaseMapper<CallLog> {

    /**
     * 依赖 trace_id 唯一索引做幂等：重复消费返回 0，不抛 DuplicateKeyException。
     */
    @Insert("INSERT IGNORE INTO tb_call_log(trace_id, app_id, api_id, method, path, status_code, " +
            "latency_ms, client_ip, error_code, request_size, response_size, create_time) " +
            "VALUES(#{log.traceId}, #{log.appId}, #{log.apiId}, #{log.method}, #{log.path}, " +
            "#{log.statusCode}, #{log.latencyMs}, #{log.clientIp}, #{log.errorCode}, " +
            "#{log.requestSize}, #{log.responseSize}, #{log.createTime})")
    int insertIgnore(@Param("log") CallLog log);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tb_call_log WHERE create_time &gt;= #{start}" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    long countSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tb_call_log WHERE create_time &gt;= #{start} " +
            "AND status_code &gt;= 200 AND status_code &lt; 300" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    long countSuccessSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT AVG(latency_ms) FROM tb_call_log WHERE create_time &gt;= #{start}" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    Double averageLatencySince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT COUNT(DISTINCT app_id) FROM tb_call_log WHERE create_time &gt;= #{start}" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    long countDistinctAppsSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT api_id AS apiId, COUNT(*) AS callCount FROM tb_call_log " +
            "WHERE create_time &gt;= #{start} AND api_id IS NOT NULL" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            " GROUP BY api_id ORDER BY callCount DESC LIMIT 5" +
            "</script>")
    List<Map<String, Object>> topApisSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);
}
```

## QuotaPlanMapper.java

源码：[src/main/java/com/agentgate/mapper/QuotaPlanMapper.java](../../src/main/java/com/agentgate/mapper/QuotaPlanMapper.java)

继承 BaseMapper<QuotaPlan> 的数据库访问接口。selectById/selectList/insert/update 等通用 SQL 由 MyBatis-Plus 提供；空接口表示无自定义 SQL，不表示不执行数据库操作。


### 完整代码

```java
package com.agentgate.mapper;

import com.agentgate.entity.QuotaPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface QuotaPlanMapper extends BaseMapper<QuotaPlan> {
}
```

## SubscriptionMapper.java

源码：[src/main/java/com/agentgate/mapper/SubscriptionMapper.java](../../src/main/java/com/agentgate/mapper/SubscriptionMapper.java)

继承 BaseMapper<Subscription> 的数据库访问接口。selectById/selectList/insert/update 等通用 SQL 由 MyBatis-Plus 提供；空接口表示无自定义 SQL，不表示不执行数据库操作。


### 完整代码

```java
package com.agentgate.mapper;

import com.agentgate.entity.Subscription;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface SubscriptionMapper extends BaseMapper<Subscription> {
}
```

## UsageStatMapper.java

源码：[src/main/java/com/agentgate/mapper/UsageStatMapper.java](../../src/main/java/com/agentgate/mapper/UsageStatMapper.java)

按 appId/apiId/statDate 联合唯一键 INSERT ... ON DUPLICATE KEY UPDATE，原子累加 call_count/error_count，避免 Java 读改写丢计数。

### 字段与方法声明索引

下列声明帮助定位职责：字段是对象保存的数据或注入依赖，方法是执行入口；多行参数和实际逻辑见完整源码。

```java
    int upsertIncrement(@Param("appId") Long appId,
```

### 完整代码

```java
package com.agentgate.mapper;

import com.agentgate.entity.UsageStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface UsageStatMapper extends BaseMapper<UsageStat> {

    @Insert("INSERT INTO tb_usage_stat(app_id, api_id, stat_date, call_count, error_count, create_time, update_time) " +
            "VALUES(#{appId}, #{apiId}, #{statDate}, 1, #{isError}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE call_count = call_count + 1, " +
            "error_count = error_count + #{isError}, update_time = NOW()")
    int upsertIncrement(@Param("appId") Long appId,
                        @Param("apiId") Long apiId,
                        @Param("statDate") LocalDate statDate,
                        @Param("isError") int isError);
}
```

## UserMapper.java

源码：[src/main/java/com/agentgate/mapper/UserMapper.java](../../src/main/java/com/agentgate/mapper/UserMapper.java)

继承 BaseMapper<User> 的数据库访问接口。selectById/selectList/insert/update 等通用 SQL 由 MyBatis-Plus 提供；空接口表示无自定义 SQL，不表示不执行数据库操作。


### 完整代码

```java
package com.agentgate.mapper;

import com.agentgate.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface UserMapper extends BaseMapper<User> {
}
```

