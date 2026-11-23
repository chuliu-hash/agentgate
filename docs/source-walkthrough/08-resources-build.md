# 构建、配置、SQL、Lua 与演示脚本

[返回源码索引](README.md) · [学习指南](../learning-guide.md)

## pom.xml

源码：[pom.xml](../../pom.xml)

Maven 构建入口：定义 Boot 2.7.18 父项目、Java 8、依赖版本、测试依赖和 Boot 打包插件；编译阶段 Lombok 处理注解，package 生成可执行 JAR。


### 完整代码

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/>
    </parent>

    <groupId>com.agentgate</groupId>
    <artifactId>agentgate</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>agentgate</name>
    <description>AgentGate - 智能体工具开放平台</description>

    <properties>
        <java.version>1.8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <redisson.version>3.23.5</redisson.version>
        <hutool.version>5.8.25</hutool.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson</artifactId>
            <version>${redisson.version}</version>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## application.yaml

源码：[src/main/resources/application.yaml](../../src/main/resources/application.yaml)

运行参数：数据源、Redis/Lettuce 池、MQ 手动确认/并发/prefetch、Jackson、MyBatis 映射、密钥和安全开关。${环境变量:默认值} 表示优先环境覆盖；默认密码仅供演示。


### 完整代码

```yaml
server:
  port: 8081

spring:
  application:
    name: agentgate
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/agentgate?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}
  redis:
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:123456}
    lettuce:
      pool:
        max-active: 30
        max-idle: 15
        min-idle: 5
        time-between-eviction-runs: 10s
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:hmdp}
    password: ${RABBITMQ_PASSWORD:hmdp}
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 5
        prefetch: 10
        default-requeue-rejected: false
  jackson:
    default-property-inclusion: non_null
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  type-aliases-package: com.agentgate.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

agentgate:
  # AES-256 主密钥：生产必须通过环境变量注入，长度 32 字节
  secret-key: ${AGENTGATE_SECRET_KEY:AgentGate@2026SecretKey32Bytes!!}
  openapi:
    max-body-bytes: 1048576
    timestamp-window-seconds: 300
    nonce-ttl-minutes: 5
    connect-timeout-ms: 3000
    read-timeout-ms: 5000
  ssrf:
    # 本地演示时允许回环地址；生产必须为 false
    allow-loopback: ${AGENTGATE_SSRF_ALLOW_LOOPBACK:true}
  security:
    # 只有部署在可信反向代理之后才开启，否则 X-Forwarded-For 可被伪造绕过 IP 白名单
    trust-proxy: ${AGENTGATE_TRUST_PROXY:false}
  admin:
    username: ${AGENTGATE_ADMIN_USERNAME:admin}
    password: ${AGENTGATE_ADMIN_PASSWORD:admin123}

logging:
  level:
    com.agentgate: debug
    org.springframework.amqp: info
```

## agentgate.sql

源码：[src/main/resources/db/agentgate.sql](../../src/main/resources/db/agentgate.sql)

初始化数据库和八张表。逐列阅读类型、默认值、主键及联合唯一索引；trace_id 唯一与统计联合唯一是消息消费幂等的基础。应用初始化器只插数据，不负责建表。


### 完整代码

```sql
-- AgentGate 建库建表脚本（MySQL 8.0）
-- 管理员、套餐、演示 API 由应用启动时的 DataInitializer 幂等补齐。

CREATE DATABASE IF NOT EXISTS agentgate
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE agentgate;

CREATE TABLE IF NOT EXISTS tb_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(32)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    nickname    VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
    role        VARCHAR(16)  NOT NULL DEFAULT 'DEVELOPER' COMMENT 'ADMIN/DEVELOPER',
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS tb_app (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id           BIGINT       NOT NULL COMMENT '所属开发者',
    name              VARCHAR(64)  NOT NULL COMMENT '应用名称',
    app_key           VARCHAR(64)  NOT NULL COMMENT 'AppKey',
    app_secret_cipher VARCHAR(256) NOT NULL COMMENT 'AES-GCM 加密后的 AppSecret',
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    ip_whitelist      VARCHAR(512) DEFAULT NULL COMMENT '逗号分隔，* 表示不限制',
    create_time       DATETIME     NOT NULL,
    update_time       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_key (app_key),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开发者应用表';

CREATE TABLE IF NOT EXISTS tb_api (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code              VARCHAR(64)  NOT NULL COMMENT '对外稳定编码',
    name              VARCHAR(128) NOT NULL COMMENT 'API 名称',
    method            VARCHAR(8)   NOT NULL DEFAULT 'ANY' COMMENT 'GET/POST/PUT/DELETE/ANY',
    upstream_url      VARCHAR(512) NOT NULL COMMENT '上游地址',
    version           VARCHAR(32)  NOT NULL DEFAULT 'v1' COMMENT '版本',
    sensitivity_level TINYINT      NOT NULL DEFAULT 1 COMMENT '1 普通 2 敏感 3 高敏',
    status            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
    timeout_ms        INT          NOT NULL DEFAULT 5000 COMMENT '上游超时（预留）',
    description       VARCHAR(512) DEFAULT NULL,
    create_time       DATETIME     NOT NULL,
    update_time       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开放 API 定义表';

CREATE TABLE IF NOT EXISTS tb_quota_plan (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(64)  NOT NULL COMMENT '套餐名称',
    qps_limit     INT          NOT NULL COMMENT 'QPS 上限',
    daily_quota   INT          NOT NULL COMMENT '日调用上限',
    monthly_quota INT          NOT NULL COMMENT '月调用上限',
    description   VARCHAR(256) DEFAULT NULL,
    create_time   DATETIME     NOT NULL,
    update_time   DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '配额套餐表';

CREATE TABLE IF NOT EXISTS tb_subscription (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    app_id        BIGINT       NOT NULL COMMENT '应用 ID',
    api_id        BIGINT       NOT NULL COMMENT 'API ID',
    quota_plan_id BIGINT       NOT NULL COMMENT '配额套餐 ID',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACTIVE/REJECTED',
    reject_reason VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
    create_time   DATETIME     NOT NULL,
    update_time   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_api (app_id, api_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '应用订阅表';

CREATE TABLE IF NOT EXISTS tb_call_log (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    trace_id      VARCHAR(64) NOT NULL COMMENT '链路 ID',
    app_id        BIGINT      DEFAULT NULL COMMENT '应用 ID',
    api_id        BIGINT      DEFAULT NULL COMMENT 'API ID',
    method        VARCHAR(8)  DEFAULT NULL,
    path          VARCHAR(512) DEFAULT NULL,
    status_code   INT         DEFAULT NULL,
    latency_ms    BIGINT      DEFAULT NULL,
    client_ip     VARCHAR(64) DEFAULT NULL,
    error_code    INT         DEFAULT NULL,
    request_size  INT         DEFAULT NULL,
    response_size INT         DEFAULT NULL,
    create_time   DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trace_id (trace_id),
    KEY idx_app_time (app_id, create_time),
    KEY idx_api_time (api_id, create_time),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开放接口调用日志';

CREATE TABLE IF NOT EXISTS tb_usage_stat (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    app_id      BIGINT   NOT NULL COMMENT '应用 ID',
    api_id      BIGINT   NOT NULL COMMENT 'API ID',
    stat_date   DATE     NOT NULL COMMENT '统计日期',
    call_count  BIGINT   NOT NULL DEFAULT 0 COMMENT '调用次数',
    error_count BIGINT   NOT NULL DEFAULT 0 COMMENT '错误次数',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_api_date (app_id, api_id, stat_date),
    KEY idx_date (stat_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '按日用量统计';

CREATE TABLE IF NOT EXISTS tb_audit_log (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT        DEFAULT NULL COMMENT '操作人 ID',
    username      VARCHAR(32)   DEFAULT NULL COMMENT '操作人',
    action        VARCHAR(64)   NOT NULL COMMENT '动作',
    resource_type VARCHAR(64)   DEFAULT NULL COMMENT '资源类型',
    resource_id   VARCHAR(64)   DEFAULT NULL COMMENT '资源 ID',
    detail        VARCHAR(1024) DEFAULT NULL COMMENT '关键参数（截断）',
    ip            VARCHAR(64)   DEFAULT NULL COMMENT '客户端 IP',
    create_time   DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理端操作审计';
```

## rate_limit_consume.lua

源码：[src/main/resources/lua/rate_limit_consume.lua](../../src/main/resources/lua/rate_limit_consume.lua)

一次脚本原子完成令牌桶补充/判断/扣减与日/月配额判断/双增。KEYS 依次是限流 Hash、日计数、月计数；ARGV 是桶容量、每秒补充量、当前毫秒、本次令牌数、日/月上限和两个 TTL。返回 1 放行、0 QPS 超限、-1 日配额用尽、-2 月配额用尽、-3 参数或已有数据非法。所有判断在写入之前，拒绝路径不写任何 key。


### 完整代码

```lua
-- 一次原子完成：令牌桶补充/判断/扣减 + 日/月配额判断/双增。
-- KEYS[1] = 令牌桶 Hash（ratelimit:{appId}:{apiId}）
-- KEYS[2] = 日配额（quota:day:{appId}:{apiId}:{yyyyMMdd}）
-- KEYS[3] = 月配额（quota:month:{appId}:{apiId}:{yyyyMM}）
-- ARGV[1] = 桶容量；ARGV[2] = 每秒补充令牌数；ARGV[3] = 当前毫秒
-- ARGV[4] = 本次消耗令牌数；ARGV[5] = 日上限；ARGV[6] = 月上限
-- ARGV[7] = 日 key TTL 秒；ARGV[8] = 月 key TTL 秒
-- 返回：1 = 放行；0 = QPS 超限；-1 = 日配额用尽；-2 = 月配额用尽；-3 = 参数/已有数据非法
local rateKey = KEYS[1]
local dayKey = KEYS[2]
local monthKey = KEYS[3]

local capacity = tonumber(ARGV[1])
local refillPerSecond = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])
local dayLimit = tonumber(ARGV[5])
local monthLimit = tonumber(ARGV[6])
local dayTtl = tonumber(ARGV[7])
local monthTtl = tonumber(ARGV[8])

-- 先校验入参，避免 tonumber 得到 nil 后在比较时抛错。
-- TTL <= 0 必须拒绝：EXPIRE key 0 会直接删除 key，等同于重置配额。
if capacity == nil or capacity <= 0
        or refillPerSecond == nil or refillPerSecond < 0
        or now == nil or now <= 0
        or requested == nil or requested <= 0 or requested > capacity
        or dayLimit == nil or dayLimit <= 0
        or monthLimit == nil or monthLimit <= 0
        or dayTtl == nil or dayTtl <= 0
        or monthTtl == nil or monthTtl <= 0 then
    return -3
end

-- HMGET 对不存在的字段返回 false；只有字段缺失才初始化，损坏值交由下面判空拒绝。
local bucket = redis.call('HMGET', rateKey, 'tokens', 'last')
local tokens
local last
if bucket[1] == false then
    tokens = capacity
else
    tokens = tonumber(bucket[1])
end
if bucket[2] == false then
    last = now
else
    last = tonumber(bucket[2])
end
if tokens == nil or last == nil then
    return -3
end

-- 时间回拨时按 0 补充，避免出现负补充。
local deltaMs = now - last
if deltaMs < 0 then
    deltaMs = 0
end
tokens = math.min(capacity, tokens + (deltaMs / 1000.0) * refillPerSecond)

-- 全部检查通过前不写任何 key：拒绝路径零写入，不会消耗令牌或配额。
if tokens < requested then
    return 0
end

local dayUsed = tonumber(redis.call('GET', dayKey) or '0')
if dayUsed == nil then
    return -3
end
if dayUsed >= dayLimit then
    return -1
end

local monthUsed = tonumber(redis.call('GET', monthKey) or '0')
if monthUsed == nil then
    return -3
end
if monthUsed >= monthLimit then
    return -2
end

-- 全部检查通过后才写入：扣令牌、日/月各 +1 并设置 TTL。
tokens = tokens - requested
redis.call('HSET', rateKey, 'tokens', tokens, 'last', now)
redis.call('PEXPIRE', rateKey, 60000)

redis.call('INCR', dayKey)
-- dayTtl/monthTtl 是到周期结束再加 60 秒的剩余秒数，因此每次成功重设 TTL
-- 不会改变绝对过期时间，还能自愈历史上没有 TTL 的 key。
redis.call('EXPIRE', dayKey, dayTtl)

redis.call('INCR', monthKey)
redis.call('EXPIRE', monthKey, monthTtl)

return 1
```

## demo.sh

源码：[scripts/demo.sh](../../scripts/demo.sh)

端到端演示入口：阅读变量、外部命令检查、登录/应用/订阅/审批/签名请求顺序；需先启动服务。此处收录脚本并不表示已执行；涉及 Python 时先选择 Conda 环境。


### 完整代码

```bash
#!/usr/bin/env bash
#
# AgentGate 端到端演示：注册 -> 建应用 -> 订阅审批 -> 签名调用 -> 防重放 -> 脱敏 -> 日志看板
#
# 依赖：curl、openssl、sed、grep、date
# 用法：
#   bash scripts/demo.sh
#   SERVER_PORT=8082 bash scripts/demo.sh
#   BASE_URL=http://127.0.0.1:8082 bash scripts/demo.sh

set -u

BASE_URL="${BASE_URL:-http://127.0.0.1:${SERVER_PORT:-8081}}"

extract_str() {
  printf '%s' "$1" | sed -n "s/.*\"$2\":\"\([^\"]*\)\".*/\1/p" | head -n 1
}

extract_num() {
  printf '%s' "$1" | sed -n "s/.*\"$2\":\([0-9][0-9]*\).*/\1/p" | head -n 1
}

section() {
  echo
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

require_value() {
  if [ -z "$1" ]; then
    echo "ERROR: $2"
    exit 1
  fi
}

section "0. 目标服务 $BASE_URL"
HEALTH=$(curl -s "$BASE_URL/actuator/health" || true)
echo "health: $HEALTH"
if ! printf '%s' "$HEALTH" | grep -q '"status":"UP"'; then
  echo "ERROR: 服务未启动，请先执行 mvn spring-boot:run"
  exit 1
fi

DEV_USER="dev_$(date +%s)"
DEV_PASS="123456"

section "1. 注册并登录开发者 $DEV_USER"
curl -s -X POST "$BASE_URL/user/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$DEV_USER\",\"password\":\"$DEV_PASS\",\"nickname\":\"demo\"}" > /dev/null
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/user/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$DEV_USER\",\"password\":\"$DEV_PASS\"}")
DEV_TOKEN=$(extract_str "$LOGIN_RESP" token)
require_value "$DEV_TOKEN" "开发者登录失败：$LOGIN_RESP"
echo "developer token: $DEV_TOKEN"

section "2. 创建应用（AppSecret 只返回一次）"
APP_RESP=$(curl -s -X POST "$BASE_URL/developer/apps" \
  -H "Authorization: Bearer $DEV_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"demo-app","ipWhitelist":"*"}')
echo "$APP_RESP"
APP_ID=$(extract_num "$APP_RESP" appId)
APP_KEY=$(extract_str "$APP_RESP" appKey)
APP_SECRET=$(extract_str "$APP_RESP" appSecret)
require_value "$APP_ID" "创建应用失败"
require_value "$APP_SECRET" "未拿到 appSecret：$APP_RESP"

section "3. 管理员登录，读取套餐与已上架 API"
ADMIN_RESP=$(curl -s -X POST "$BASE_URL/user/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}')
ADMIN_TOKEN=$(extract_str "$ADMIN_RESP" token)
require_value "$ADMIN_TOKEN" "管理员登录失败：$ADMIN_RESP"
echo "admin token: $ADMIN_TOKEN"

PLANS=$(curl -s "$BASE_URL/admin/quota-plans" -H "Authorization: Bearer $ADMIN_TOKEN")
PLAN_ID=$(printf '%s' "$PLANS" | grep -o '"id":[0-9]*' | head -n 1 | grep -o '[0-9]*')
if [ -z "$PLAN_ID" ]; then
  curl -s -X POST "$BASE_URL/admin/quota-plans" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    -d '{"name":"Demo","qpsLimit":2,"dailyQuota":1000,"monthlyQuota":10000}' > /dev/null
  PLANS=$(curl -s "$BASE_URL/admin/quota-plans" -H "Authorization: Bearer $ADMIN_TOKEN")
  PLAN_ID=$(printf '%s' "$PLANS" | grep -o '"id":[0-9]*' | head -n 1 | grep -o '[0-9]*')
fi
require_value "$PLAN_ID" "没有可用的配额套餐"
echo "quota plan id: $PLAN_ID"

APIS=$(curl -s "$BASE_URL/developer/apis" -H "Authorization: Bearer $DEV_TOKEN")
WEATHER_API_ID=$(printf '%s' "$APIS" | grep -o '{"id":[0-9]*,"code":"weather"' | head -n 1 | grep -o '[0-9]*')
ORDER_API_ID=$(printf '%s' "$APIS" | grep -o '{"id":[0-9]*,"code":"order"' | head -n 1 | grep -o '[0-9]*')
require_value "$WEATHER_API_ID" "未找到 weather API，请确认 DataInitializer 已执行"
require_value "$ORDER_API_ID" "未找到 order API，请确认 DataInitializer 已执行"
echo "weather api id: $WEATHER_API_ID, order api id: $ORDER_API_ID"

section "4. 订阅 weather 并审批"
SUB_RESP=$(curl -s -X POST "$BASE_URL/developer/subscriptions" \
  -H "Authorization: Bearer $DEV_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"appId\":$APP_ID,\"apiId\":$WEATHER_API_ID,\"quotaPlanId\":$PLAN_ID}")
echo "$SUB_RESP"
SUB_ID=$(extract_num "$SUB_RESP" id)
require_value "$SUB_ID" "订阅申请失败"
curl -s -X POST "$BASE_URL/admin/subscriptions/$SUB_ID/approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"approved":true}'
echo

# ---------------------------------------------------------------------------
# 签名工具
# ---------------------------------------------------------------------------
TS=""
NONCE=""
SIG=""
BODY_HASH=""

sign_request() {
  local method="$1"
  local path="$2"
  local query="$3"
  local body="${4:-}"
  TS=$(date +%s)
  NONCE=$(date +%s%N)
  BODY_HASH=$(printf '%s' "$body" | openssl dgst -sha256 | awk '{print $NF}')
  CANONICAL=$(printf '%s\n%s\n%s\n%s\n%s\n%s' \
    "$method" "$path" "$query" "$TS" "$NONCE" "$BODY_HASH")
  SIG=$(printf '%s' "$CANONICAL" | openssl dgst -sha256 -hmac "$APP_SECRET" -r | cut -d' ' -f1)
}

signed_get() {
  local path="$1"
  local query="$2"
  local url="$BASE_URL$path"
  if [ -n "$query" ]; then
    url="$url?$query"
  fi
  curl -s -w '\nHTTP %{http_code}\n' "$url" \
    -H "X-App-Key: $APP_KEY" \
    -H "X-Timestamp: $TS" \
    -H "X-Nonce: $NONCE" \
    -H "X-Signature: $SIG"
}

section "5. 正确签名调用 /openapi/weather?city=beijing"
sign_request "GET" "/openapi/weather" "city=beijing" ""
echo "canonical:"
printf '%s\n' "$CANONICAL"
echo "signature: $SIG"
signed_get "/openapi/weather" "city=beijing"

section "6. 重放同一个 Nonce（预期 1003）"
signed_get "/openapi/weather" "city=beijing"

section "7. 错误签名（预期 1004）"
sign_request "GET" "/openapi/weather" "city=beijing" ""
SIG="deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"
signed_get "/openapi/weather" "city=beijing"

section "8. 订阅 order 并验证响应脱敏"
ORDER_SUB_RESP=$(curl -s -X POST "$BASE_URL/developer/subscriptions" \
  -H "Authorization: Bearer $DEV_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"appId\":$APP_ID,\"apiId\":$ORDER_API_ID,\"quotaPlanId\":$PLAN_ID}")
echo "$ORDER_SUB_RESP"
ORDER_SUB_ID=$(extract_num "$ORDER_SUB_RESP" id)
require_value "$ORDER_SUB_ID" "order 订阅申请失败"
curl -s -X POST "$BASE_URL/admin/subscriptions/$ORDER_SUB_ID/approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"approved":true}' > /dev/null
echo "调用 /openapi/order/1001："
sign_request "GET" "/openapi/order/1001" "" ""
signed_get "/openapi/order/1001" ""

section "9. 调用日志、看板、审计日志"
sleep 2

echo "--- 最近调用日志 ---"
curl -s "$BASE_URL/admin/call-logs?page=1&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
echo
echo "--- 平台看板 ---"
curl -s "$BASE_URL/admin/dashboard" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
echo
echo "--- 操作审计 ---"
curl -s "$BASE_URL/admin/audit-logs?page=1&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
echo

section "完成"
echo "提示：可继续用管理员账号 admin/admin123 在数据库中观察 tb_call_log、tb_usage_stat、tb_audit_log。"
```

## .gitignore

源码：[.gitignore](../../.gitignore)

Git 忽略规则，排除构建输出、IDE 或本地文件；忽略不等于删除文件，也不会自动取消已跟踪文件。


### 完整代码

```text
target/
logs/
*.log

.idea/
*.iml
*.ipr
*.iws
.vscode/
.settings/
.classpath
.project

.DS_Store
Thumbs.db

# 本地环境变量与密钥，不要提交
.env
application-local.yaml
application-local.yml
```

