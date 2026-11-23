# AgentGate 设计说明与面试要点

## 1. 定位与边界

AgentGate 是智能体工具调用的边界治理层。它不负责模型推理，也不做 Prompt 检测，而是解决：

- 谁能调用（应用身份 AK/SK）
- 能调用什么（API 目录 + 订阅审批）
- 能调用多少（QPS 与日/月配额）
- 调用是否安全（IP 白名单、SSRF、响应脱敏）
- 调用能否追溯（traceId、调用日志、管理端审计）

与第一个项目「智能体安全护栏」组合：

```text
用户输入 → 护栏输入检测 → 大模型 → 护栏输出检测 → Agent 编排
                                                    │
                                                    ▼
                                             AgentGate 工具调用
                                    身份 / 权限 / 配额 / 脱敏 / 审计
```

---

## 2. 为什么是单模块

约束：JDK8、未学习微服务、需要快速做出可演示、可面试的项目。

选择：

- 单模块 Spring Boot + 分层包结构，和常见 Java 后端项目一致。
- 用 `Filter + Controller 代理` 实现“网关”能力，不引入 Spring Cloud Gateway。
- 组件全部可在一台机器上启动，降低演示和排错成本。

代价与取舍：

- 网关与应用同进程，资源隔离和独立扩缩容能力弱。
- 生产可把 `SignatureFilter + OpenApiController + 限流/配额` 拆为独立网关服务，注册中心承担配置同步。
- 面试时主动说明“当前拆分的边界在哪里”，比堆组件更能体现架构判断。

---

## 3. 数据模型

8 张表围绕“应用—API—订阅—调用”闭环：

```text
tb_user 1 ── n tb_app 1 ── n tb_subscription n ── 1 tb_api
                                      │
                                      └── 1 tb_quota_plan

tb_app + tb_api ── 1 ── n tb_call_log
tb_app + tb_api + stat_date ── 1 ── tb_usage_stat
tb_user ── 1 ── n tb_audit_log
```

关键索引：

- `tb_subscription(app_id, api_id)` 唯一：同一应用对同一 API 只有一条订阅。
- `tb_call_log.trace_id` 唯一：消费幂等。
- `tb_usage_stat(app_id, api_id, stat_date)` 唯一：按日聚合。
- `tb_call_log(app_id, create_time)`、`tb_call_log(api_id, create_time)`：看板查询。

---

## 4. 登录态与权限

### 4.1 方案

- BCrypt 存储密码。
- 登录生成 UUID Token，Redis Hash `login:token:{token}` 保存 `id/username/nickname/role`，TTL 2 小时。
- `RefreshTokenInterceptor` 每次请求恢复 `UserHolder` 并续期。
- `LoginInterceptor` 校验登录；`/admin/**` 额外校验 `ADMIN`。
- 登出删除 Redis Token。

### 4.2 为什么不用 JWT

| 维度 | Redis Token | JWT |
|---|---|---|
| 服务端撤销 | 删除 key 即可 | 需黑名单或短过期 |
| 存储 | Redis 有状态 | 无状态 |
| 续期 | 滑动过期简单 | 需 refresh token |
| 横向扩展 | 共享 Redis | 天然无状态 |

本项目需要“登出立即失效”和“管理端角色变更即时生效”，Redis Token 更直接。JWT 的优势在跨服务无状态校验，当前单模块用不上。

### 4.3 为什么修复旧项目的 logout 问题

黑马点评原项目登出只清理 ThreadLocal，Redis Token 仍然有效。本项目的登录态以 Redis 为准，登出必须删除 key，否则旧 Token 在 TTL 内一直可用。

---

## 5. AK/SK 签名

### 5.1 canonical string

```text
METHOD\nPATH\nQUERY\ntimestamp\nnonce\nSHA256(body)
```

包含这些字段的原因：

- **METHOD + PATH + QUERY**：绑定请求目标，防止把签名挪用到其他接口。QUERY 只取 URL 查询串，表单字段与 JSON 体一样由 body hash 覆盖，避免依赖容器解析请求体。
- **body hash**：绑定请求体，避免只改 body 不改签名。
- **timestamp**：限制签名有效期，降低泄露后的重放窗口。
- **nonce**：在有效期内保证一次性，服务端 `SETNX` 去重。
- **appSecret 不参与传输**：只用于 HMAC，抓包拿不到。

### 5.2 为什么查询参数要排序

客户端与服务端对 Query 的遍历顺序可能不同。统一按 key/value 字典序拼接，保证双方计算结果一致，同时避免同名多值参数被忽略。

### 5.3 为什么用常量时间比较

普通字符串 `equals` 在首个不同字符处返回，攻击者可以通过响应时间逐字节猜测签名。`MessageDigest.isEqual` 的耗时与内容无关。

### 5.4 为什么签名通过后才消费 Nonce

如果先 `SETNX` 再验签，攻击者可以用任意 AppKey + 随机 Nonce 刷 Redis，导致合法请求的 Nonce 被提前占用，也会浪费内存。先验签可以把无效请求挡在 Nonce 写入之前。

### 5.5 为什么 Secret 可逆加密

平台需要在请求时用明文 Secret 校验 HMAC，因此不能像密码一样单向哈希。方案是 AES-256-GCM 加密存储：

- 密钥来自配置 `agentgate.secret-key`，生产走环境变量/密钥管理。
- GCM 自带完整性校验，密文被篡改会解密失败。
- 每次加密使用随机 IV，同一明文产生不同密文。
- 重置 Secret 时更新密文并删除 `cache:app:{appKey}`，旧 Secret 立即失效。

风险：应用缓存 `AppAuthInfo` 中包含解密后的 Secret。当前依赖 Redis 内网隔离；更高安全级别可对缓存值二次加密或只缓存签名校验所需的摘要。

---

## 6. 限流与配额

### 6.1 QPS：Redis Lua 令牌桶

```lua
tokens = min(capacity, tokens + elapsed * refillPerSecond)
if tokens >= requested then tokens = tokens - requested; return 1 else return 0 end
```

- key：`ratelimit:{appId}:{apiId}`。
- Hash 保存 `tokens` 与 `last`，Lua 在 Redis 单线程内完成“补充 + 判断 + 扣减”。
- TTL 60 秒，避免冷 key 永久占用内存。
- 容量与补充速率都取套餐 QPS：允许 1 秒左右的突发，长期平均不超过 QPS。

对比：

| 算法 | 优点 | 缺点 |
|---|---|---|
| 固定窗口 | 实现简单 | 窗口边界双倍流量 |
| 滑动窗口 | 边界平滑 | 需要更多存储/计算 |
| 令牌桶 | 允许突发、长期限速 | 参数需要调优 |
| 漏桶 | 输出绝对平滑 | 不适合允许突发的 API |

### 6.2 一次 Lua 原子完成 QPS 与日/月配额

key：`ratelimit:{appId}:{apiId}`、`quota:day:{appId}:{apiId}:{yyyyMMdd}`、`quota:month:{appId}:{apiId}:{yyyyMM}`。

`rate_limit_consume.lua` 在一次脚本执行内：

1. 读取令牌桶 `tokens/last`，按毫秒差补充并封顶；
2. 令牌不足返回 0；
3. 读取日/月已用量，任一达到上限返回 -1/-2；
4. 全部通过后才扣 1 个令牌、日/月各 `INCR` 并重设到周期末尾加 60 秒的 TTL，返回 1。

所有判断都在写入之前，拒绝路径零写入，因此不会出现“令牌已扣但配额不足”。Redis 单线程执行脚本，也不会出现两个请求同时读到 999 再都放行。

### 6.3 为什么 QPS 与配额仍分语义

- QPS 控制瞬时压力，保护上游和本平台。
- 日/月配额控制商业用量与成本。
- 两类拒绝的业务含义和运营处理不同：对外仍分别返回 429 和 4290。
- 执行层已合并为一个脚本保证原子性；上游调用失败仍不退还已扣额度，属于另一类边界。

---

## 7. 缓存设计

采用 Cache Aside，而不是原项目的逻辑过期：

```text
读：cache hit → 返回
    cache miss → 空值缓存？→ 返回 null
               → Redisson 锁 → 二次检查 → 查 DB → 写缓存
写：更新 DB → 删除缓存
```

三类问题：

- **穿透**：查询不存在的 AppKey/API code 时缓存空值（TTL 1 分钟），避免每次都打 DB。
- **击穿**：热点 key 失效瞬间大量请求回源，用 Redisson 分布式锁保证只有一个线程重建。
- **雪崩**：TTL 加 0–120 秒随机抖动，避免同一批 key 同时过期。

为什么不用逻辑过期：

- 逻辑过期需要额外的过期字段、异步重建线程和预热流程。
- 本项目配置类数据读多写少，Cache Aside + 互斥重建足够，且行为更容易解释和测试。

缓存一致性：

- 更新 DB 后删除缓存，而不是更新缓存，避免并发写导致旧值覆盖新值。
- 删除失败的概率低；即使失败，TTL 上限 10–30 分钟会兜底。
- 订阅审批、Secret 重置、API 上下架都会精确删除对应 key。

---

## 8. RabbitMQ 调用日志

### 8.1 链路

```text
SignatureFilter finally
  → RabbitTemplate.convertAndSend(call.log.exchange, call.log)
  → call.log.queue
  → CallLogListener（手动 ACK）
  → tb_call_log (INSERT IGNORE by trace_id)
  → tb_usage_stat (ON DUPLICATE KEY UPDATE)
```

### 8.2 可靠性

- **发送失败**：捕获异常后同步落库，日志不丢，也不阻塞主请求。
- **消费失败**：`basicNack(requeue=false)`，消息进入 `call.log.dlx.exchange` → `call.log.dlq`，避免无限重试。
- **重复消费**：`trace_id` 唯一索引 + `INSERT IGNORE`，返回 0 时不再累加统计。
- **顺序**：日志允许乱序，查询按 `create_time` 排序；统计是累加，不依赖顺序。

### 8.3 为什么不在请求线程直接写库

- 调用日志是旁路数据，不应增加主请求延迟。
- 高并发下同步写库会放大数据库压力。
- 异步化后可通过 MQ 削峰，失败还有同步兜底。

---

## 9. 状态机与并发控制

### 9.1 API

```text
DRAFT → PUBLISHED → OFFLINE
                 ↗
OFFLINE → PUBLISHED
```

### 9.2 订阅

```text
PENDING → ACTIVE
PENDING → REJECTED
REJECTED → PENDING（重新申请）
```

### 9.3 条件更新

```sql
UPDATE tb_api SET status = 'PUBLISHED'
WHERE id = ? AND status = 'DRAFT';
```

- 影响行数为 0 说明状态已被其他请求修改，直接报“状态流转非法”。
- 不能只靠“先查再改”：两个管理员同时审批时，先查后改会都通过。
- 唯一索引是最后一道兜底：即使并发插入，也只会有一条订阅记录。

---

## 10. SSRF 防护

开放平台允许管理员配置上游 URL，必须防止把内网当作上游：

- 仅允许 `http/https`。
- 拒绝 URL 中的 userinfo（`http://user:pass@host`）。
- DNS 解析后检查所有地址：
  - 回环：`127.0.0.0/8`、`::1`
  - 私网：`10/8`、`172.16/12`、`192.168/16`
  - 链路本地：`169.254/16`（含云元数据 `169.254.169.254`）
  - CGNAT：`100.64/10`
  - 组播、`0.0.0.0/8`、IPv6 ULA `fc00::/7`
- `RestTemplate` 禁止跟随重定向，防止 302 到内网。
- `RestTemplate` 使用 `Proxy.NO_PROXY` 直连上游，避免继承操作系统/JVM 代理后，校验的是目标 IP、实际却从代理出网。
- 每次转发前重新校验，降低 DNS 重绑定风险。
- 本地演示通过 `agentgate.ssrf.allow-loopback=true` 放行回环；生产必须关闭。

残余风险：DNS 校验与实际连接之间仍存在 TOCTOU 窗口。生产可固定解析结果、使用网络层出口策略或专用代理。

### 10.1 IP 白名单与代理信任

- 应用可配置 `ip_whitelist`（逗号分隔，`*` 表示不限制），不匹配时拒绝调用。
- 默认使用 `request.getRemoteAddr()`，即 TCP 连接的真实来源地址。
- `X-Forwarded-For` 可被客户端任意伪造。只有部署在可信反向代理之后，才通过 `agentgate.security.trust-proxy=true` 让应用读取代理写入的客户端 IP；否则攻击者可伪造该头绕过白名单。
- 配置了代理后，应同时限制应用端口只接受代理访问，避免绕过代理直连。

---

## 11. 响应脱敏

- API 配置 `sensitivity_level`：1 普通、2 敏感、3 高敏。
- 级别 ≥ 2 时，对 JSON 响应递归遍历字段名：
  - `phone/mobile/telephone`：保留前 3 后 4
  - `idCard/idNo`：保留前 6 后 4
  - `email`：保留首字符与域名
  - `address`：保留前 6 个字符
  - `bankCard/cardNo`：只保留后 4 位
  - `password/secret/token/accessKey/secretKey`：全部替换
- 非 JSON 响应原样返回，避免破坏二进制或文本协议。
- 脱敏发生在平台出站前，调用方拿不到原始敏感字段。

---

## 12. 审计

`@AuditLog(action, resourceType)` 标注管理端写方法，AOP 记录：

- 操作人 `userId/username`（来自 `UserHolder`）
- action、resourceType、resourceId
- 参数摘要（JSON，截断 1000 字符）
- 客户端 IP、时间
- 成功/失败与失败原因

审计写入失败只记录告警，不影响业务主流程。审计表只追加，不提供修改接口。

---

## 13. 测试策略

纯单元测试，不依赖 MySQL/Redis/RabbitMQ：

- `SignatureUtilsTest`：SHA-256 空串、RFC 4231 HMAC 向量、参数排序与 URL 编码、查询串解析、常量时间比较。
- `SsrfUtilsTest`：回环、私网、云元数据、CGNAT、IPv6 ULA、非法 scheme、userinfo。
- `MaskUtilsTest`：手机号、邮箱、身份证、银行卡、嵌套与数组、非 JSON。
- `AesUtilsTest`：加解密往返、随机 IV、篡改密文、错误密钥。
- `StateMachineTest`：API 与订阅的合法/非法流转。

端到端验证由 `scripts/demo.sh` 覆盖：注册 → 建应用 → 订阅审批 → 签名调用 → 重放/错签 → 脱敏 → 日志/看板。

---

## 14. 面试问答速查

**Q：为什么不用 Spring Cloud Gateway？**
A：当前是单模块、单机演示，Filter + Controller 代理足够；拆分独立网关会引入注册中心、配置同步、部署编排，收益与复杂度不匹配。生产可按“鉴权/限流/转发”边界拆分。

**Q：签名为什么不用 RSA？**
A：AK/SK 对称 HMAC 性能好、实现简单，适合服务端到服务端；RSA 更适合一对多、公钥分发的场景。Secret 通过 AES 加密存储弥补对称密钥的保管问题。

**Q：Nonce 存在 Redis，Redis 挂了怎么办？**
A：属于可用性与安全性的取舍。当前选择 fail-closed：Redis 不可用时签名/限流链路失败，拒绝请求；生产可对读路径降级、对防重放保持强依赖。

**Q：限流为什么不用 Guava RateLimiter？**
A：单机限流在多实例下会超发；Redis Lua 在集群下共享状态且原子。Guava 适合单进程内部保护。

**Q：MQ 消息重复怎么办？**
A：`trace_id` 唯一索引 + `INSERT IGNORE`，插入 0 行则跳过统计累加。消费端不需要依赖“消息只投递一次”。

**Q：缓存删除失败怎么办？**
A：TTL 兜底；对一致性要求更高的场景可用延迟双删、binlog 订阅或本地消息表，但会增加复杂度，本项目按配置类数据的容忍度选择简单方案。

**Q：为什么调用日志记录失败请求？**
A：签名错误、未订阅、限流都是安全与运营的重要信号；`tb_call_log` 的 `app_id/api_id` 允许为空，便于统计攻击与配置问题。
