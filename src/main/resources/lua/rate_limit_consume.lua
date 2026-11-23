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
