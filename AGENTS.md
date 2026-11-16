# Repository Guidelines

## 项目结构与模块组织

AgentGate 是基于 Java 8、Spring Boot 2.7.18 的单模块 Maven 应用，提供工具 API 开放、签名认证、配额控制和调用审计。

- `src/main/java/com/agentgate/`：业务代码；`controller` 处理 HTTP 请求，`service` 实现业务，`mapper` 使用 MyBatis-Plus 访问数据库。
- `filter`、`interceptor`、`aspect`：请求过滤、登录检查与审计；`listener`：RabbitMQ 消息消费。
- `src/main/resources/`：`application.yaml`、`db/agentgate.sql` 和 Redis Lua 脚本。
- `src/test/java/com/agentgate/`：工具类及状态机测试。
- `docs/`：设计、接口与演示说明；`scripts/demo.sh`：演示脚本；`target/`：构建产物，勿手动修改。

## 构建、测试与本地开发

准备 JDK 8、Maven 3.6+、MySQL 8、Redis 6+ 和 RabbitMQ 3.x。首次运行前导入 `src/main/resources/db/agentgate.sql`，并配置服务连接信息。

- `mvn test`：运行自动化测试。
- `mvn clean package`：清理、测试并生成可执行 JAR。
- `mvn spring-boot:run`：启动本地应用，默认端口为 `8081`。
- `curl http://127.0.0.1:8081/actuator/health`：检查服务健康状态。
- `bash scripts/demo.sh`：在应用及依赖服务启动后运行演示，需要 Bash。

## 编码风格与命名

使用 UTF-8、四空格缩进并保持 Java 8 兼容性。类名使用 `PascalCase`，方法与变量使用 `camelCase`，常量使用 `UPPER_SNAKE_CASE`。沿用 `*Controller`、`*Service`、`*Mapper` 等分层命名及现有 Lombok 用法。新增代码遵循相邻文件风格；当前 `pom.xml` 未配置专用格式化或 lint 插件。

## 测试规范

使用 Spring Boot Test 提供的 JUnit 5，测试类以 `*Test` 结尾并镜像源代码包路径。可用 `mvn -Dtest=SignatureUtilsTest test` 运行单个测试。修复缺陷时添加行为回归测试，重点覆盖签名、SSRF、脱敏、加密和状态转换的边界情况。当前未配置覆盖率门槛；提交前确保相关测试及完整测试套件通过。

## 提交与 Pull Request

当前目录无 Git 元数据，无法核实历史提交惯例。建议使用简短、明确的祈使式提交标题，每次提交聚焦一个变更。PR 应说明问题、行为变化、关联 issue、测试命令与结果；涉及接口、配置或数据库时同步更新 `docs/` 并解释兼容性影响。

## 安全配置与协作约定

通过环境变量配置数据库、Redis、RabbitMQ 凭据及 `AGENTGATE_SECRET_KEY`、`AGENTGATE_ADMIN_PASSWORD`，不得提交真实密钥。生产环境设置 `AGENTGATE_SSRF_ALLOW_LOOPBACK=false`，仅在可信反向代理后启用代理信任。

协作问答使用中文。需要运行 Python 时，先询问使用哪个 Conda 虚拟环境；网络异常时可使用本地代理 `127.0.0.1:7897`。
