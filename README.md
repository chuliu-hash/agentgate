# AgentGate — 智能体工具开放平台

AgentGate 是一个面向智能体/业务应用的 **工具（Open API）开放平台**：平台方把内部或第三方 HTTP 工具注册为开放 API，开发者创建应用并获得 AK/SK；应用申请订阅并通过审批后，使用签名调用 `/openapi/**`，平台统一完成身份认证、权限校验、限流配额、上游转发、敏感数据脱敏和调用审计。

它和作者的另一个项目「智能体安全护栏」形成互补：

> 护栏管理“模型输入与输出”，AgentGate 管理“工具调用与数据边界”。

本项目是 **单模块 Spring Boot 应用**，不使用微服务、Spring Cloud、Spring Security 或 JWT，便于在 JDK8 环境下学习、部署和面试讲解。

---

## 1. 功能概览

### 开发者

- 注册、登录、登出（Redis Token，登出立即失效）
- 创建应用，获得 AppKey / AppSecret（Secret 只返回一次）
- 重置 Secret、配置 IP 白名单、禁用应用
- 浏览已上架 API、申请订阅、查看订阅状态
- 查看自己应用的调用日志与用量看板

### 管理员

- API 注册、编辑、上架 / 下架（状态机 + 条件更新）
- 配额套餐维护（QPS、日配额、月配额）
- 订阅审批（通过 / 拒绝，拒绝后可重新申请）
- 全量调用日志查询、操作审计日志、平台看板

### 开放调用链路

```text
Client
  │  X-App-Key / X-Timestamp / X-Nonce / X-Signature
  ▼
TraceIdFilter        生成 traceId，写入 MDC 与响应头
  ▼
SignatureFilter      签名、时间戳、Nonce 防重放、应用状态、IP 白名单
  ▼                  API 上架状态、订阅状态、Redis Lua 限流与配额
OpenApiController    转发到上游（禁止重定向、SSRF 校验）
  ▼
Upstream             响应脱敏（sensitivity_level >= 2）
  ▼
RabbitMQ             异步调用日志 + 用量统计，失败同步兜底
```

---

## 2. 技术栈

| 分类 | 选型 |
|---|---|
| JDK | 8 |
| 框架 | Spring Boot 2.7.18 |
| 持久层 | MyBatis-Plus 3.5.7、MySQL 8 |
| 缓存 | Spring Data Redis + Lettuce、Redisson |
| 消息 | RabbitMQ（手动 ACK、死信队列） |
| 鉴权 | Redis Token + 拦截器 + ThreadLocal；开放接口 AK/SK + HMAC-SHA256 |
| 工具 | Hutool、Lombok、AspectJ、Bean Validation |
| 测试 | JUnit5 |

---

## 3. 快速启动

### 3.1 环境要求

- JDK 8
- Maven 3.6+
- MySQL 8.0
- Redis 6+
- RabbitMQ 3.x

### 3.2 初始化数据库

```bash
mysql -uroot -p123456 < src/main/resources/db/agentgate.sql
```

脚本只负责建库建表；管理员账号、配额套餐和演示 API（`weather` / `order` / `user`）由应用启动时的 `DataInitializer` 幂等补齐。

### 3.3 修改配置（可选）

`src/main/resources/application.yaml` 中的连接信息均支持环境变量覆盖：

```bash
export DB_USERNAME=root
export DB_PASSWORD=123456
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=123456
export RABBITMQ_HOST=localhost
export RABBITMQ_USERNAME=hmdp
export RABBITMQ_PASSWORD=hmdp

# 生产必须替换：AES 主密钥、管理员密码、关闭回环地址
export AGENTGATE_SECRET_KEY='replace-with-a-strong-secret'
export AGENTGATE_ADMIN_PASSWORD='replace-me'
export AGENTGATE_SSRF_ALLOW_LOOPBACK=false
# 只有部署在可信反向代理之后才设为 true
export AGENTGATE_TRUST_PROXY=false
```

### 3.4 启动

```bash
mvn spring-boot:run
```

默认端口 `8081`，健康检查：

```bash
curl http://127.0.0.1:8081/actuator/health
```

演示脚本（另开终端）：

```bash
bash scripts/demo.sh
```

> 如果 8081 被占用：`SERVER_PORT=8082 bash scripts/demo.sh`，脚本会把 `BASE_URL` 一并切到 8082。

### 3.5 默认账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |

开发者账号通过 `/user/register` 注册。生产环境务必通过环境变量修改管理员密码与 AES 密钥。

---

## 4. 目录结构

```text
src/main/java/com/agentgate
├── AgentGateApplication.java
├── annotation/        @AuditLog
├── aspect/            管理端操作审计 AOP
├── config/            MVC、MyBatis、Redis、RabbitMQ、RestTemplate、初始化
├── controller/        用户、开发者、管理员、开放接口、Mock 上游
├── dto/               请求/响应对象
├── entity/            8 张表的实体
├── enums/             错误码、状态机
├── exception/         BizException
├── filter/            TraceIdFilter、SignatureFilter
├── interceptor/       RefreshTokenInterceptor、LoginInterceptor
├── listener/          CallLogListener
├── mapper/            MyBatis-Plus Mapper
├── service/ + impl/   业务逻辑
└── utils/             签名、AES、SSRF、脱敏、缓存、IP、TraceId
```

---

## 5. 核心设计

### 5.1 登录态

- 密码使用 BCrypt 存储。
- 登录生成 UUID Token，写入 Redis Hash `login:token:{token}`，TTL 2 小时。
- `RefreshTokenInterceptor` 恢复 `UserHolder`（ThreadLocal）并续期；`LoginInterceptor` 校验登录与 `/admin/**` 角色。
- 登出删除 Redis Token，旧 Token 立即失效。

### 5.2 AK/SK 与签名

```text
canonical = METHOD + "\n" + PATH + "\n" + 排序后的 Query + "\n"
          + timestamp + "\n" + nonce + "\n" + SHA256(body)
signature = Hex(HMAC-SHA256(canonical, appSecret))
```

- Query 只取 URL 查询串（不含表单体），按 key 字典序、value 字典序拼接并 URL 编码；JSON 或表单请求体统一由 `SHA256(body)` 覆盖。
- 时间戳窗口默认 ±300 秒；Nonce 使用 Redis `SETNX`，TTL 5 分钟。
- 签名比较使用 `MessageDigest.isEqual`，避免时序攻击。
- AppSecret 使用 AES-256-GCM 加密存储，重置后旧 Secret 立即失效。

### 5.3 限流与配额

- QPS：Redis Lua 令牌桶，key `ratelimit:{appId}:{apiId}`，原子补充与扣减。
- 日/月配额：Redis Lua 读取用量、比较上限、`INCR` 并首次设置过期，原子执行。
- 超限统一返回 HTTP 429。

### 5.4 缓存

- Cache Aside：`cache:app:{appKey}`、`cache:api:{code}`、`cache:sub:{appId}:{apiId}`。
- 空值缓存防穿透；随机 TTL 防雪崩；Redisson 互斥重建防击穿。
- 写操作更新 DB 后精确删除缓存。

### 5.5 调用日志可靠性

- 调用结束后发送 `CallLogMessage` 到 RabbitMQ；发送失败同步落库。
- 消费者手动 ACK；异常 `basicNack(requeue=false)` 进入死信队列。
- `tb_call_log.trace_id` 唯一索引 + `INSERT IGNORE` 保证重复消息幂等。
- `tb_usage_stat` 使用 `INSERT ... ON DUPLICATE KEY UPDATE` 原子累加。

### 5.6 安全设计

- IP 白名单：默认只使用 TCP 连接的真实来源地址；只有显式开启 `agentgate.security.trust-proxy` 才读取 `X-Forwarded-For`/`X-Real-IP`，避免客户端伪造请求头绕过白名单。
- SSRF：仅 http/https，禁止 userinfo；拦截回环、私网、链路本地（含 `169.254.169.254`）、CGNAT、IPv6 ULA；转发禁用重定向；每次转发前重新校验 DNS。
- 上游直连：`RestTemplate` 显式使用 `Proxy.NO_PROXY`，不继承操作系统/JVM 代理，避免 SSRF 校验的地址与实际出口不一致。
- 脱敏：敏感级别 ≥ 2 时递归掩码 `phone/mobile/idCard/email/address/bankCard/password/secret/token` 等字段。
- 审计：管理端写操作通过 `@AuditLog` + AOP 记录操作人、动作、资源、参数摘要和 IP。
- 配置：数据库、Redis、RabbitMQ、AES 密钥、管理员密码均支持环境变量注入。

---

## 6. 数据表

| 表 | 作用 | 关键约束 |
|---|---|---|
| `tb_user` | 用户 | `username` 唯一 |
| `tb_app` | 开发者应用 | `app_key` 唯一 |
| `tb_api` | 开放 API 定义 | `code` 唯一 |
| `tb_quota_plan` | 配额套餐 | — |
| `tb_subscription` | 应用订阅 | `(app_id, api_id)` 唯一 |
| `tb_call_log` | 调用日志 | `trace_id` 唯一 |
| `tb_usage_stat` | 日用量统计 | `(app_id, api_id, stat_date)` 唯一 |
| `tb_audit_log` | 管理端审计 | 只追加 |

---

## 7. 已知边界

- 上游超时目前使用全局连接/读取超时（3s/5s）；`tb_api.timeout_ms` 已落库，作为后续按 API 设置超时的扩展点。
- IP 白名单支持精确 IP 与 `*`，不支持 CIDR。
- 鉴权错误响应使用统一 `Result` 结构；限流响应未附 `X-RateLimit-*` 头（令牌桶内部状态未回传）。
- 本项目的“网关”是单体内的 `Filter + Controller 代理`，生产环境可拆分独立网关进程。

---

## 8. 简历与面试

### 8.1 简历 bullet

- 基于 Spring Boot + MyBatis-Plus + MySQL + Redis + RabbitMQ 实现智能体工具开放平台，覆盖应用接入、API 注册/订阅、配额限流、签名鉴权、调用审计与运营看板。
- 设计 AK/SK + HMAC-SHA256 + 时间戳 + Nonce 防重放机制，AppSecret 使用 AES-256-GCM 加密存储，结合 IP 白名单与 SSRF 防护保障工具调用边界。
- 使用 Redis Lua 实现令牌桶限流与日/月配额原子扣减，避免并发超发；使用 Cache Aside + 空值缓存 + Redisson 互斥重建解决缓存穿透/击穿。
- 基于 RabbitMQ 异步落库调用日志，手动 ACK + 死信 + `trace_id` 唯一索引幂等，MQ 不可用时同步兜底，保证日志链路可靠。
- 使用拦截器 + ThreadLocal 实现登录态与角色鉴权，AOP 记录管理端操作审计，响应按敏感级别自动脱敏。

### 8.2 面试可讲点

1. 为什么把网关放在单体应用里，生产如何拆分。
2. canonical string 为什么包含 body hash、timestamp、nonce；如何防重放。
3. Secret 为什么可逆加密而不是哈希；密钥管理与重置后的缓存一致性。
4. 固定窗口、滑动窗口、令牌桶的取舍；为什么用 Redis Lua。
5. QPS 与日/月配额的区别；为什么限流与配额都必须原子。
6. Cache Aside 的删除时机、空值缓存、互斥重建、随机 TTL。
7. RabbitMQ 手动 ACK、死信、消费幂等、发送失败兜底。
8. 条件 UPDATE + 唯一索引兜底，为什么不能只靠 Redis 判断。
9. SSRF 的完整攻击面：私网、云元数据、重定向、DNS 重绑定。
10. 与护栏项目组合成“模型输入输出 + 工具调用边界”的智能体安全闭环。

更多细节见 [docs/design.md](docs/design.md)、[docs/api.md](docs/api.md)、[docs/demo.md](docs/demo.md)。
