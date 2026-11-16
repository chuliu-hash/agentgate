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
