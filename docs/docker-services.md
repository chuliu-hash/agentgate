# Docker 依赖服务

项目根目录的 `docker-compose.yml` 提供 MySQL 8、Redis 7 和 RabbitMQ 3.13，数据分别保存在 Docker volume 中。应用本身仍由 Maven/Spring Boot 启动，容器只负责基础设施。

## 启动

安装 Docker Desktop 后，在项目根目录执行：

```powershell
Copy-Item .env.example .env
docker compose up -d
docker compose ps
```

默认端口如下：

| 服务 | 地址 | 默认账号 |
| --- | --- | --- |
| MySQL | `127.0.0.1:3306` | root / 123456 |
| Redis | `127.0.0.1:6379` | 密码 123456 |
| RabbitMQ | `127.0.0.1:5672` | hmdp / hmdp |
| RabbitMQ 管理台 | http://127.0.0.1:15672 | hmdp / hmdp |

MySQL 首次创建数据卷时会自动执行 `src/main/resources/db/agentgate.sql`。如果数据库已经初始化过，修改 SQL 文件不会自动重新执行；需要确认数据可删除后执行 `docker compose down -v`，再重新 `up`。

## 启动后端

应用在宿主机运行时，现有 `application.yaml` 默认值正好连接上述端口：

```powershell
mvn spring-boot:run
```

如果修改了 `.env` 中的密码，需要在启动 Spring Boot 前同步设置环境变量：

```powershell
$env:DB_PASSWORD='你的 MySQL 密码'
$env:REDIS_PASSWORD='你的 Redis 密码'
$env:RABBITMQ_USERNAME='你的 RabbitMQ 用户'
$env:RABBITMQ_PASSWORD='你的 RabbitMQ 密码'
mvn spring-boot:run
```

检查：

```powershell
curl.exe http://127.0.0.1:8081/actuator/health
```

## 常用操作

```powershell
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f rabbitmq
docker compose restart
docker compose stop
docker compose down
```

`down` 会删除容器但保留数据卷；`down -v` 会同时删除数据卷和全部本地数据库、Redis、RabbitMQ 数据，适合重新初始化开发环境，生产环境不要执行。

如将 Spring Boot 也放进 Compose，连接地址应改为服务名 `mysql`、`redis`、`rabbitmq`，不能使用 `127.0.0.1`；当前配置按“后端在宿主机、依赖在 Docker”设计。

## 本机服务与端口冲突

当前使用的容器名为 agentgate-mysql、agentgate-redis、agentgate-rabbitmq。Windows MySQL80、WSL Redis 及旧 hmdp 容器不应同时占用相同端口。执行 docker compose ps 确认三项健康；机器重启后仍需检查原服务是否自动启动。停止依赖使用 docker compose stop，重新启动使用 docker compose up -d；不要通过停止整个 WSL 或 Docker Desktop 来切换服务。
