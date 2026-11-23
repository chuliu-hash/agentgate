# AgentGate 接口文档

真实天气 `weather_live` 与汇率 `exchange_rate` 的参数和接入步骤见 [真实 API 接入](public-apis.md)，它们沿用下文开放接口签名规则。

统一响应体：

```json
{
  "success": true,
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1794800000000
}
```

登录后的管理端/开发者接口使用请求头：

```http
Authorization: Bearer <token>
```

`Bearer ` 前缀可省略。

---

## 1. 用户接口

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/user/register` | 注册开发者 | 否 |
| POST | `/user/login` | 登录，返回 token 与用户信息 | 否 |
| POST | `/user/logout` | 登出，删除 Redis Token | 是 |
| GET | `/user/me` | 当前用户 | 是 |

注册请求：

```json
{"username": "dev01", "password": "123456", "nickname": "开发者"}
```

登录响应：

```json
{
  "success": true,
  "code": 0,
  "data": {
    "token": "4b3b66dbde6e4b59913cb7d68f110245",
    "user": {"id": 2, "username": "dev01", "nickname": "开发者", "role": "DEVELOPER"}
  }
}
```

---

## 2. 开发者接口

### 2.1 应用

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/developer/apps` | 创建应用，返回 AppKey/AppSecret（Secret 仅返回一次） |
| GET | `/developer/apps` | 我的应用列表 |
| POST | `/developer/apps/{id}/reset-secret` | 重置 Secret，旧 Secret 立即失效 |
| PUT | `/developer/apps/{id}/ip-whitelist` | 更新 IP 白名单（逗号分隔，`*` 表示不限制） |
| POST | `/developer/apps/{id}/disable` | 禁用应用 |

创建请求：

```json
{"name": "demo-app", "ipWhitelist": "*"}
```

创建响应：

```json
{
  "success": true,
  "code": 0,
  "data": {
    "appId": 1,
    "appKey": "ak_E7sOJKuhpuCLv2Ca",
    "appSecret": "KtIpMbIQMKQNBG0MQKSBh5YnpEAo2Fdm"
  }
}
```

### 2.2 API 与订阅

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/developer/apis` | 已上架 API 列表 |
| POST | `/developer/subscriptions` | 申请订阅 |
| GET | `/developer/subscriptions?appId=1` | 我的订阅 |

订阅请求：

```json
{"appId": 1, "apiId": 1, "quotaPlanId": 2}
```

订阅状态：`PENDING → ACTIVE / REJECTED`，被拒绝后可重新申请，仍复用同一条记录。

### 2.3 日志与看板

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/developer/call-logs?appId=1` | 分页查询自己应用的调用日志 |
| GET | `/developer/call-logs/{traceId}?appId=1` | 按 traceId 查询 |
| GET | `/developer/dashboard?appId=1` | 自己应用的今日看板 |

调用日志支持参数：`appId`、`apiId`、`traceId`、`startTime`、`endTime`、`page`、`size`。时间格式 `yyyy-MM-dd HH:mm:ss`，`size` 最大 100。

---

## 3. 管理员接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/apis` | 全部 API |
| POST | `/admin/apis` | 创建 API（默认 DRAFT） |
| PUT | `/admin/apis/{id}` | 编辑 API |
| POST | `/admin/apis/{id}/publish` | 上架 |
| POST | `/admin/apis/{id}/offline` | 下架 |
| GET | `/admin/quota-plans` | 套餐列表 |
| POST | `/admin/quota-plans` | 创建套餐 |
| PUT | `/admin/quota-plans/{id}` | 编辑套餐 |
| GET | `/admin/subscriptions?status=PENDING` | 订阅列表，可按状态过滤 |
| POST | `/admin/subscriptions/{id}/approve` | 审批订阅 |
| GET | `/admin/call-logs` | 全量调用日志 |
| GET | `/admin/call-logs/{traceId}` | 按 traceId 查询 |
| GET | `/admin/audit-logs?page=1&size=10` | 操作审计 |
| GET | `/admin/dashboard` | 平台看板 |

API 创建请求：

```json
{
  "code": "weather",
  "name": "天气查询",
  "method": "GET",
  "upstreamUrl": "http://127.0.0.1:8081/mock-api/weather",
  "version": "v1",
  "sensitivityLevel": 1,
  "timeoutMs": 5000,
  "description": "演示 API"
}
```

套餐创建请求：

```json
{"name": "试用版", "qpsLimit": 2, "dailyQuota": 1000, "monthlyQuota": 10000}
```

审批请求：

```json
{"approved": true, "reason": ""}
```

`approved=false` 时 `reason` 会写入订阅记录。

---

## 4. 开放接口

### 4.1 路径

```text
ANY /openapi/{apiCode}
ANY /openapi/{apiCode}/{path...}
```

`/openapi/` 后的路径尾巴会拼接到 `upstream_url` 后面，查询参数原样转发。例如：

- API `weather` 的 `upstream_url = http://127.0.0.1:8081/mock-api/weather`
- 请求 `GET /openapi/weather?city=beijing` → 转发 `GET http://127.0.0.1:8081/mock-api/weather?city=beijing`
- API `order` 的 `upstream_url = http://127.0.0.1:8081/mock-api/order`
- 请求 `GET /openapi/order/1001` → 转发 `GET http://127.0.0.1:8081/mock-api/order/1001`

### 4.2 必填请求头

| Header | 说明 |
|---|---|
| `X-App-Key` | 应用 AppKey |
| `X-Timestamp` | Unix 秒级时间戳 |
| `X-Nonce` | 随机串，5 分钟内不可重复 |
| `X-Signature` | 小写十六进制 HMAC-SHA256 |

### 4.3 签名算法

```text
canonical = METHOD.toUpperCase() + "\n"
          + PATH + "\n"
          + QUERY + "\n"
          + timestamp + "\n"
          + nonce + "\n"
          + SHA256_HEX(body)

signature = HMAC_SHA256_HEX(appSecret, canonical)
```

- `PATH`：不含 Context Path 的请求路径，如 `/openapi/weather`。
- `QUERY`：所有查询参数按 key 字典序、同 key 的 value 字典序排序，再进行 URL 编码（UTF-8），用 `&` 连接；无参数则为空串。
- `body`：请求体原始字节；无请求体时取 `SHA256("") = e3b0c442...b855`。
- 比较采用常量时间，签名大小写不敏感（内部转小写）。

示例（`city=beijing`）：

```text
GET
/openapi/weather
city=beijing
1794802094
1794802094624912600
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

```bash
SIG=$(printf '%s' "$CANONICAL" | openssl dgst -sha256 -hmac "$APP_SECRET" -r | cut -d' ' -f1)

curl "http://127.0.0.1:8081/openapi/weather?city=beijing" \
  -H "X-App-Key: $APP_KEY" \
  -H "X-Timestamp: $TS" \
  -H "X-Nonce: $NONCE" \
  -H "X-Signature: $SIG"
```

### 4.4 校验顺序

1. 四个签名头是否齐全
2. 时间戳是否在 ±300 秒内
3. 应用是否存在且为 ACTIVE
4. IP 是否在白名单
5. 签名是否正确
6. Nonce 是否首次使用（签名通过后才消费 Nonce）
7. API 是否存在且已上架
8. 订阅是否为 ACTIVE
9. 一次 Lua 原子校验并扣减 QPS 令牌与日/月配额
10. 上游 SSRF 校验与转发

### 4.5 错误码

| code | HTTP | 含义 |
|---|---|---|
| 1001 | 401 | 缺少签名请求头 |
| 1002 | 401 | 时间戳过期或格式非法 |
| 1003 | 401 | Nonce 重复 |
| 1004 | 401 | 签名错误 |
| 1005 | 403 | 应用已禁用 |
| 1006 | 403 | IP 不在白名单 |
| 1007 | 403 | API 未上架/已下线 |
| 1008 | 403 | 未订阅或订阅未通过 |
| 1009 | 403 | 上游地址不安全（SSRF） |
| 2001 | 504 | 上游超时 |
| 2002 | 502 | 上游不可用 |
| 429 | 429 | QPS 超限 |
| 4290 | 429 | 日/月配额用尽 |

成功响应直接透传上游状态码、`Content-Type` 与响应体，并带 `X-Trace-Id` 响应头。

---

## 5. Mock 上游

仅供本地演示，不需要鉴权：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/mock-api/weather?city=beijing` | 天气 |
| GET | `/mock-api/order/{id}` | 订单，含 phone/email/address |
| GET | `/mock-api/user/{id}` | 用户，含手机号/身份证/银行卡 |

`order` 与 `user` 两个演示 API 的敏感级别分别为 2、3，经过 `/openapi/**` 调用时响应会被脱敏。
