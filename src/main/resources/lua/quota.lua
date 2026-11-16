-- KEYS[1] = 日配额 key, KEYS[2] = 月配额 key
-- ARGV[1] = 日上限, ARGV[2] = 月上限, ARGV[3] = 日 key TTL 秒, ARGV[4] = 月 key TTL 秒
local dayKey = KEYS[1]
local monthKey = KEYS[2]
local dayLimit = tonumber(ARGV[1])
local monthLimit = tonumber(ARGV[2])
local dayTtl = tonumber(ARGV[3])
local monthTtl = tonumber(ARGV[4])

local dayUsed = tonumber(redis.call('GET', dayKey) or '0')
local monthUsed = tonumber(redis.call('GET', monthKey) or '0')

if dayUsed >= dayLimit or monthUsed >= monthLimit then
    return 0
end

redis.call('INCR', dayKey)
if dayUsed == 0 then
    redis.call('EXPIRE', dayKey, dayTtl)
end

redis.call('INCR', monthKey)
if monthUsed == 0 then
    redis.call('EXPIRE', monthKey, monthTtl)
end

return 1
