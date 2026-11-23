# 消息队列、安全实现与调试实践

[返回学习导航](learning-guide.md)

## 8. RabbitMQ 从零理解与日志处理

可把 Exchange 理解成分拣台，Routing Key 是分拣标签，Queue 是待办箱，Consumer 是处理人员。HTTP 线程不必等所有数据库统计完成；但 MQ 发送本身仍由当前线程调用，并非 `@Async`。

`RabbitMQConfig` 创建持久 DirectExchange `call.log.exchange`、持久队列 `call.log.queue`，用 `call.log` 绑定；JSON 转换器将 DTO 序列化。正常消息进入队列后，`CallLogListener` 调用 `saveDirect`，成功 `basicAck`；异常 `basicNack(requeue=false)` 交给死信 Exchange `call.log.dlx.exchange`，再路由到 `call.log.dlq`。当前没有自动处理死信的消费者，需要人工排查补偿。

配置为 manual ACK、消费者并发 5、prefetch 10。ACK 是消费者确认已处理，不是发送方确认已发布。消费后崩溃、未 ACK 会导致消息再次投递，所以必须幂等。

`CallLogMapper.insertIgnore` 依赖 trace_id 唯一索引；插入返回 0 时不再计数，否则 `UsageStatMapper.upsertIncrement` 利用联合唯一键执行 `ON DUPLICATE KEY UPDATE`，避免 Java 读改写丢增量。消费者从 Spring 代理调用 `@Transactional saveDirect`，正常将日志插入和统计累加放在一个数据库事务中。统计日期和日志时间取处理时刻，跨午夜延迟不一定归属请求发生日。

发送抛异常时 `CallLogDispatchServiceImpl.asyncSave` 通过 `CallLogService` 的 Spring 代理同步调用 `saveDirect`，兜底再失败则只记错误；跨 Bean 调用使事务注解正常生效。配置未实现发布确认/返回回调的完整保障，不能把“convertAndSend 没抛异常”当作消息最终落库；进程崩溃、消息路由问题仍可能丢日志。这是学习实现，不是严格 exactly-once。

## 9. 安全、异常、审计的具体行为

`SsrfUtils` 限制 http/https，拒绝 URL userinfo 与危险 IP，对解析出的地址检查回环、私网、链路本地、CGNAT、IPv6 等；登记和每次转发都验证，RestTemplate 禁重定向并直连。这降低 SSRF 风险，但 DNS 检查与真正连接分开，不能宣称完全消除 DNS 重绑定；生产还需出口网络约束。

`IpUtils` 默认读取连接远端地址。启用 trust-proxy 才使用转发头，故只有可信代理能过滤伪造头时才打开。白名单支持精确地址和 `*`，不是 CIDR 网段匹配。

`MaskUtils` 遍历 JSON 对象和数组，以忽略大小写的字段名匹配手机号、身份证、邮箱、地址、银行卡及 token 等，再保留部分字符或全部替换。它不是自动识别文本中的个人信息；字段未命中、非 JSON、解析失败都会保留原内容，不能当作完整数据防泄漏系统。

`@AuditLog` 是自定义方法注解，与 `entity.AuditLog` 名同但用途不同。`AuditLogAspect.around` 执行业务前后捕获成功/异常，记录用户、action、resourceType、首个数字参数作为资源 ID、参数 JSON 和 IP，摘要截断为 1000 字符。审计写失败不阻断业务；截断不等于脱敏，新增带密钥/密码的管理接口时不能直接记录全部参数。

Controller 中的 `BizException` 由 `WebExceptionAdvice` 转为 HTTP 状态和 `Result`，参数校验失败转 400，未知异常转 500。Filter 位于 MVC 之外，自己捕获异常并构造错误响应；不要以为全局 Advice 会自动接住所有 Filter 异常。业务 code 4290 对应 HTTP 429，code 1004 对应 HTTP 401，JSON 内 code 不总等于 HTTP 状态。

`DashboardServiceImpl` 读取调用日志聚合当天调用数、2xx 成功数、平均延迟、活跃应用、Top5 API。开发者查询先验证 appId 归属，管理员传 null 表示全局。成功指标与统计 error_count 的“≥400 为错误”不是严格互补，3xx 尤其要区分。MQ 有延迟，看板不保证请求结束即更新。

## 10. 动手学习与排错

按 `docs/api.md` 完成注册→登录→创建应用→浏览 API/套餐→申请订阅→管理员审批→签名调用。`scripts/demo.sh` 提供完整演示；运行前查看其外部命令依赖，若需使用 Python，先确认要使用的 Conda 环境。不要猜测固定数据库 ID，使用接口返回的 ID。

建议在 IDEA 依次设置断点：`UserServiceImpl.login`、`RefreshTokenInterceptor.preHandle`、`AppServiceImpl.getOwnedApp`、`SignatureFilter.doFilterInternal`、`RateLimitServiceImpl.checkAndConsume`、`OpenApiProxyServiceImpl.forward`、`CallLogListener.onMessage`。观察 request attribute、Redis key、SQL 和响应，而非只背类名。

| 现象 | 从哪里查 |
| --- | --- |
| 启动时表不存在/拒绝连接 | MySQL URL、建表脚本、凭据、DataInitializer 日志 |
| Redisson 创建失败 | Redis 地址、端口、密码、服务状态 |
| MQ 连接失败 | RabbitMQ 用户权限、端口和 vhost 默认配置 |
| 登录后仍 401 | Authorization 头、Redis Token 是否存在及 TTL |
| 管理端 403 | Token 中是否为 ADMIN |
| 签名失败 | 秒级时间戳、路径、query 编码、body 原始字节、当前 Secret |
| Nonce 重放 | 每次请求生成新的 Nonce，不要原样重发签名头 |
| 未订阅/API 下线 | 数据库状态、应用 ID 与 API ID、审批结果、对应缓存 |
| 429 | 区分 code 429 与 4290，查令牌桶或日期额度 key |
| 上游错误/脱敏不生效 | traceId、目标 URL、方法、上游状态、JSON Content-Type、字段名 |
| 日志暂不可见 | 队列积压、消费者日志、死信队列、数据库约束 |

`mvn test` 运行现有 JUnit 5 工具类及状态机测试；`mvn -Dtest=SignatureUtilsTest test` 单独测试签名。它们不等于全链路集成测试，不能证明中间件连接、角色隔离或 MQ 故障恢复正确。阅读测试时把“安排输入→执行方法→断言结果”三个阶段标出来，再为自己的改动补充行为测试。

## 11. 逐文件查阅入口

详见 [源码讲解索引](source-walkthrough/README.md)。每个章节按路径列出文件用途、真实字段/方法声明及完整源码；复杂机制结合本文对应章节阅读。源码中的注解、异常分支、返回值和 SQL 同样是实现的一部分，不要只看方法名。
