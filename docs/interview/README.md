# AgentGate Java 后端面试手册

本手册面向 Java 后端实习、校招和初中级岗位面试，按“先讲清项目，再补基础，再深挖实现，最后练场景题”的层次组织。回答以当前仓库实现为准；谈到改进方案时，会明确标为可演进方向，避免把规划说成已有功能。

## 阅读顺序

| 层次 | 文档 | 适合回答的问题 |
|---|---|---|
| 1. 项目与业务 | [项目介绍与业务问题](01-project-and-business.md) | 项目做什么、角色是谁、核心业务闭环是什么、如何做项目介绍 |
| 2. Java 与 Spring | [Java 后端基础题](02-java-spring.md) | Java 集合、并发、JVM、Spring MVC、IOC/AOP、事务 |
| 3. 鉴权与安全 | [开放 API 请求链路](03-auth-and-security.md) | AK/SK、HMAC、防重放、用户登录、代理转发、SSRF 与脱敏 |
| 4. 数据与并发 | [MySQL、Redis 与一致性](04-data-cache-concurrency.md) | 表设计、索引、缓存、Lua 原子性、状态机、幂等与并发更新 |
| 5. 消息与综合题 | [消息、排障与模拟面试](05-messaging-and-scenarios.md) | RabbitMQ、日志可靠性、故障分析、项目追问和回答框架 |

## 面试前速记

- **一句话**：AgentGate 是智能体工具调用的开放治理平台，统一管理应用身份、API 订阅、调用额度、上游代理和审计追踪。
- **核心链路**：`TraceIdFilter → SignatureFilter → OpenApiController → OpenApiProxyService → 上游 API`；调用结果在过滤器 `finally` 阶段形成日志消息，交给 RabbitMQ 异步落库。
- **核心技术**：Java 8、Spring Boot 2.7、MySQL、Redis、Redisson、RabbitMQ、MyBatis-Plus。
- **关键取舍**：单体应用便于快速演示和部署；开放调用用 AK/SK 签名；QPS 和日/月配额分开管；日志异步化并提供发送异常时的同步兜底。
- **诚实边界**：当前不是微服务，也没有 Outbox 或 RabbitMQ publisher confirm；QPS 与日/月配额已在单脚本内原子扣减，但上游失败后额度不自动返还，Redis 配额也不是持久化账本。被追问时说明已知限制并给出改进思路。

## 项目阅读入口

- [项目说明](../../README.md)
- [接口契约](../api.md)
- [设计说明](../design.md)
- [请求链路学习文档](../learning-02-request-flow.md)
- [数据、缓存与限流学习文档](../learning-03-data-cache.md)
- [消息与安全学习文档](../learning-04-messaging-security.md)

源码定位可从 `src/main/java/com/agentgate/` 下的 `filter`、`controller`、`service`、`mapper`、`entity`、`config`、`listener` 开始。
