# 数据模型、状态流转、缓存与限流

[返回学习导航](learning-guide.md)

## 6. 业务数据与并发控制

| 表/实体 | 保存什么 | 关键关系或约束 |
| --- | --- | --- |
| tb_user / User | 用户及 BCrypt 密码、角色 | username 唯一 |
| tb_app / App | 所有人、AK、加密 SK、白名单、状态 | user_id 指向用户；app_key 唯一 |
| tb_api / ApiDefinition | code、方法、上游、敏感级别、状态 | code 唯一；timeout_ms 尚未用于逐 API 超时 |
| tb_quota_plan / QuotaPlan | QPS、日/月额度 | 订阅引用套餐 |
| tb_subscription / Subscription | 应用订阅某 API 的审批状态 | app_id + api_id 唯一 |
| tb_call_log / CallLog | 调用结果及耗时 | trace_id 唯一，支持幂等 |
| tb_usage_stat / UsageStat | 每应用/API/日期统计 | 三字段联合唯一，用于原子累加 |
| tb_audit_log / AuditLog | 管理操作人、动作、参数摘要、IP | 与开放调用日志是两套记录 |

Java 关联字段不是 ORM 自动加载，也不要仅凭字段名假定数据库声明了外键；具体以 SQL 为准。实体用 `@TableName` 等注解映射表，驼峰字段映射下划线列；MyBatis 配置分页插件与时间自动填充。

API 状态：`DRAFT → PUBLISHED → OFFLINE → PUBLISHED`，不允许无意义的重复转换。`ApiServiceImpl.transition` 先判断规则，再用 `WHERE id=? AND status=旧状态` 更新，检查影响行数防并发覆盖。

订阅申请创建 PENDING，管理员审批为 ACTIVE 或 REJECTED；拒绝后可重新申请。`SubscriptionServiceImpl` 核验应用归属、API 状态、套餐，依赖联合唯一约束防重复；重新申请和审批使用旧状态条件更新。枚举虽然允许 ACTIVE→REJECTED，当前审批方法只接受 PENDING，不能把枚举能力当作已有撤销接口。

## 7. Redis：缓存、锁、限流、配额

### 缓存为什么不是简单 get/set

`CacheClient.getOrLoad` 先查缓存，再检查空值标记，然后通过 Redisson 尝试抢锁，成功后再次查缓存、回源查询、保存结果。空值缓存 1 分钟防止不断查询不存在的对象；TTL 加 0～119 秒随机抖动，降低成批失效；锁减少热点同时回源。尝试锁等待 2 秒、租期 10 秒；未抢到锁会短暂等待后查缓存，仍未命中则自己回源，所以不是严格单线程回源。鉴权缓存含 Secret，需要限制 Redis 访问。

Cache Aside 意为数据库为主、缓存按需加载。写操作先更新数据库再删缓存，正常下一次读取回源。事务提交前删除缓存、并发旧读回填、删除失败都可能造成短时不一致；本项目没有实现严格强一致。

### Lua 为什么能避免并发超扣

Java 连发 GET、判断、INCR，中间可插入其他请求；Redis 执行一个 Lua 脚本期间不会穿插其他命令。`@PostConstruct` 只负责加载脚本，真正每次调用通过 `redisTemplate.execute` 在 Redis 上执行。

`rate_limit_consume.lua` 用一次脚本执行完成令牌桶与日/月配额：读取 Hash 中 tokens/last，按毫秒差补充并封顶；令牌不足返回 0，不触碰配额。随后读取日/月计数，任一达上限返回 -1/-2，同样不扣令牌；只有全部通过才扣 1 个令牌、两个计数各加 1 并设置 TTL。key 分别是 `ratelimit:appId:apiId`、`quota:day:...:yyyyMMdd`、`quota:month:...:yyyyMM`。

所有判断都在写入之前，拒绝路径零写入，所以 QPS 与配额是整体原子的，不会出现“QPS 通过但配额不足时令牌被白白消耗”。上游失败仍不退额度，因为那发生在脚本之后。Redis 配额与 MySQL 用量统计不是一份数据，Redis 丢失会影响限额，不能据此宣称持久化计费。三 key 脚本未使用同槽 hash tag，当前单机配置不能直接照搬 Redis Cluster。

