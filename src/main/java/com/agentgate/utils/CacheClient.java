package com.agentgate.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheClient {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    public <T> T get(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，删除脏数据 key={}", key, e);
            redisTemplate.delete(key);
            return null;
        }
    }

    public void set(String key, Object value, long ttlMinutes) {
        try {
            String json = objectMapper.writeValueAsString(value);
            // 随机抖动，避免同一批 key 同时失效
            long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 120);
            redisTemplate.opsForValue().set(key, json, ttlMinutes * 60 + jitterSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("缓存写入失败", e);
        }
    }

    public void setNull(String key) {
        redisTemplate.opsForValue().set(key, RedisConstants.CACHE_NULL_VALUE, 1, TimeUnit.MINUTES);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Cache Aside + 空值缓存 + Redisson 互斥重建。
     */
    public <T> T getOrLoad(String key, Class<T> type, long ttlMinutes, Supplier<T> loader) {
        T cached = get(key, type);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            // 空值缓存命中
            return null;
        }
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_CACHE_KEY + key);
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 10, TimeUnit.SECONDS);
            if (locked) {
                cached = get(key, type);
                if (cached != null) {
                    return cached;
                }
                if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    return null;
                }
                T value = loader.get();
                if (value == null) {
                    setNull(key);
                    return null;
                }
                set(key, value, ttlMinutes);
                return value;
            }
            // 未抢到锁：等待一小段时间后重试缓存，仍没有再回源
            Thread.sleep(100);
            cached = get(key, type);
            return cached != null ? cached : loader.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
