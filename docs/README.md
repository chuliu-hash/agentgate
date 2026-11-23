# AgentGate 文档中心

这里按“先运行、再使用、后理解实现、最后查源码”的顺序组织项目文档。后端是 Spring Boot 单体应用，前端是独立 Nginx 静态站点；两者通过 Nginx `/api/` 代理连接。

## 1. 快速开始

真实数据接入见 [天气与汇率 API](public-apis.md)，保留原 Mock 的同时提供真实上游调用。

使用 Apifox 调试：导入 [OpenAPI 文件](apifox/agentgate.openapi.json)，操作步骤及动态签名脚本见 [Apifox 测试指南](apifox/README.md)。

| 文档 | 用途 |
| --- | --- |
| [前后端启动与页面使用](frontend-demo.md) | 启动 Spring Boot 与 Nginx，使用用户控制台完成登录、应用、订阅和在线调用 |
| [接口文档](api.md) | 查阅所有用户、开发者、管理员和开放 API 的请求格式 |
| [端到端操作流程](frontend-demo.md) | 使用页面完成注册、审批、签名调用和日志查看 |

启动前准备 Docker Desktop。执行 `docker compose up -d` 即可启动 MySQL、Redis 和 RabbitMQ；数据库初始化与环境变量见 [Docker 依赖服务](docker-services.md) 和 [后端运行架构](learning-01-runtime-architecture.md)。

## 2. 按学习目标阅读

| 顺序 | 文档 | 解决的问题 |
| --- | --- | --- |
| 1 | [运行环境与总体架构](learning-01-runtime-architecture.md) | 项目解决什么问题、组件如何启动、请求如何分层 |
| 2 | [身份认证与请求链路](learning-02-request-flow.md) | 用户 Token 和应用 AK/SK 有什么区别，一次调用经过哪些类 |
| 3 | [数据、缓存与限流](learning-03-data-cache.md) | 表结构、状态机、Cache Aside、Redisson 锁和 Lua 限额 |
| 4 | [消息、安全与排错](learning-04-messaging-security.md) | RabbitMQ、ACK、幂等、SSRF、脱敏、审计及常见故障 |

## 3. 设计与面试参考

[设计说明与面试要点](design.md) 集中记录单体架构、登录态、签名、限流配额、缓存、消息可靠性、状态机和安全设计的取舍。它适合在完成学习文档后查阅；接口参数和实际源码以 `api.md` 与 `src/` 为准。

面试准备可按层次阅读[AgentGate Java 后端面试手册](interview/README.md)，其中包含项目业务介绍、Java/Spring 基础题、核心链路深挖和场景题参考回答。

## 4. 源码索引

[逐文件源码讲解](source-walkthrough/README.md) 按启动配置、请求安全、Controller、Service、数据模型、工具类、测试和资源八个模块收录源码说明，共覆盖 111 个文件。需要定位字段、方法或完整代码时再进入此目录。

## 5. 文档边界

- `api.md` 是接口契约；修改 Controller、DTO 或错误码时同步更新。
- `frontend-demo.md` 是当前独立前端的部署与使用说明，文件名保留历史名称，但页面本身是正常用户控制台。
- `frontend-demo.md` 是前后端分离部署及用户操作说明。
- `design.md` 和 `learning-*` 解释设计与实现细节；出现差异时，以当前源码行为为准，并在文档中标注边界。

```text
docs/
├── README.md                         # 本入口
├── frontend-demo.md                  # Nginx 前端与后端代理
├── api.md                            # API 契约

├── learning-01~04-*.md               # 分层学习教程
├── design.md                         # 设计取舍与面试参考
└── source-walkthrough/               # 逐文件源码说明
```
