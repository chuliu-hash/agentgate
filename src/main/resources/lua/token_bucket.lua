-- KEYS[1] = 限流 key
-- ARGV[1] = 桶容量, ARGV[2] = 每秒补充令牌数, ARGV[3] = 当前毫秒, ARGV[4] = 本次请求令牌数
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillPerSecond = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local bucket = redis.call('HMGET', key, 'tokens', 'last')
local tokens = tonumber(bucket[1])
local last = tonumber(bucket[2])

if tokens == nil then
    tokens = capacity
end
if last == nil then
    last = now
end

local delta = math.max(0, now - last) / 1000.0
tokens = math.min(capacity, tokens + delta * refillPerSecond)

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call('HMSET', key, 'tokens', tokens, 'last', now)
redis.call('PEXPIRE', key, 60000)
return allowed
