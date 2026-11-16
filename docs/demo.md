# AgentGate 演示步骤

## 0. 前置条件

确保以下服务已启动：

```bash
docker ps                      # MySQL、RabbitMQ
redis-cli -a 123456 ping       # PONG（WSL 中执行）
```

初始化数据库：

```bash
mysql -uroot -p123456 < src/main/resources/db/agentgate.sql
```

启动应用：

```bash
mvn spring-boot:run
```

如果 8081 已被占用：

```bash
SERVER_PORT=8082 mvn spring-boot:run
```

---

## 1. 一键演示

```bash
bash scripts/demo.sh
# 8081 被占用时：
SERVER_PORT=8082 bash scripts/demo.sh
```

脚本会依次完成：

1. 注册并登录开发者。
2. 创建应用，拿到 AppKey/AppSecret。
3. 管理员登录，读取配额套餐和已上架 API。
4. 开发者订阅 `weather`，管理员审批。
5. 正确签名调用 `/openapi/weather?city=beijing`。
6. 重复 Nonce，验证防重放。
7. 错误签名，验证签名校验。
8. 订阅 `order` 并调用 `/openapi/order/1001`，验证响应脱敏。
9. 查询调用日志、平台看板、审计日志。

---

## 2. 手工验证清单

### 2.1 注册登录

```bash
curl -X POST http://127.0.0.1:8081/user/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"dev01","password":"123456","nickname":"demo"}'

curl -X POST http://127.0.0.1:8081/user/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"dev01","password":"123456"}'
```

记录返回的 `token`。

### 2.2 创建应用

```bash
curl -X POST http://127.0.0.1:8081/developer/apps \
  -H "Authorization: Bearer $DEV_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"demo-app","ipWhitelist":"*"}'
```

**AppSecret 只返回这一次**，后续只能重置。

### 2.3 管理员审批

```bash
curl -X POST http://127.0.0.1:8081/user/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'

curl "http://127.0.0.1:8081/admin/subscriptions?status=PENDING" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -X POST http://127.0.0.1:8081/admin/subscriptions/1/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"approved":true}'
```

### 2.4 签名调用

```bash
APP_KEY=ak_xxx
APP_SECRET=xxx
TS=$(date +%s)
NONCE=$(date +%s%N)
BODY_HASH=$(printf '' | openssl dgst -sha256 | awk '{print $NF}')
PATH_ONLY=/openapi/weather
QUERY='city=beijing'

CANONICAL=$(printf 'GET\n%s\n%s\n%s\n%s\n%s' \
  "$PATH_ONLY" "$QUERY" "$TS" "$NONCE" "$BODY_HASH")

SIG=$(printf '%s' "$CANONICAL" | openssl dgst -sha256 -hmac "$APP_SECRET" -r | cut -d' ' -f1)

curl -i "http://127.0.0.1:8081${PATH_ONLY}?${QUERY}" \
  -H "X-App-Key: $APP_KEY" \
  -H "X-Timestamp: $TS" \
  -H "X-Nonce: $NONCE" \
  -H "X-Signature: $SIG"
```

预期：HTTP 200，响应体为天气 JSON，响应头带 `X-Trace-Id`。

### 2.5 防重放

用完全相同的四个头再请求一次：

```bash
curl "http://127.0.0.1:8081${PATH_ONLY}?${QUERY}" \
  -H "X-App-Key: $APP_KEY" \
  -H "X-Timestamp: $TS" \
  -H "X-Nonce: $NONCE" \
  -H "X-Signature: $SIG"
```

预期：

```json
{"success":false,"code":1003,"message":"重复的 nonce，拒绝重放"}
```

### 2.6 错误签名

换一个新的 Nonce 与时间戳，把 `X-Signature` 改成 `deadbeef`：

预期：

```json
{"success":false,"code":1004,"message":"签名校验失败"}
```

### 2.7 未订阅

用未订阅的 API code 调用（例如 `user`），预期 `code=1008`。

### 2.8 响应脱敏

订阅 `order` 并审批后，对 `/openapi/order/1001` 签名调用，预期：

```json
{
  "orderId": "1001",
  "status": "PAID",
  "amount": 199.0,
  "phone": "138****5678",
  "email": "b***@example.com",
  "address": "北京市朝阳区****"
}
```

### 2.9 限流

将订阅绑定 QPS 较低的套餐（如试用版 QPS=2），在 1 秒内连续发送 5 个不同 Nonce 的签名请求。

预期：前 2 个左右返回 200，其余返回：

```json
{"success":false,"code":429,"message":"请求过于频繁"}
```

### 2.10 日志与看板

```bash
curl "http://127.0.0.1:8081/admin/call-logs?page=1&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl "http://127.0.0.1:8081/admin/dashboard" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl "http://127.0.0.1:8081/admin/audit-logs?page=1&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

预期：

- 调用日志包含成功与失败记录，失败记录带 `errorCode`。
- 看板展示今日调用量、成功率、平均耗时、活跃应用、Top API。
- 审计日志包含审批、API 上下架等管理端写操作。

---

## 3. 常见问题

| 现象 | 排查 |
|---|---|
| 启动报数据库连接失败 | 确认 MySQL 已启动、库已创建、账号密码正确 |
| 启动报 Redis 连接失败 | 确认 Redis 密码与 `REDIS_PASSWORD` 一致 |
| 开放接口 1004 | 检查 PATH 是否包含 `/openapi/...`、Query 是否排序编码、body hash 是否正确 |
| 开放接口 1002 | 检查服务器与本机时间是否同步；时间戳单位是秒 |
| 注册返回 500（JSON parse error） | 请求体必须是 UTF-8；Windows 终端可通过脚本文件发送 |
| 日志表没有数据 | 确认 RabbitMQ 已启动；MQ 不可用时会同步兜底，但不影响调用 |
| 8081 被占用 | `SERVER_PORT=8082 mvn spring-boot:run`，演示脚本同样设置 `SERVER_PORT` |
