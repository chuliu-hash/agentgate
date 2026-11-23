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
/**
 * 教学说明：封装 JSON 缓存和 getOrLoad：空值缓存、随机 TTL、Redisson 抢锁与二次查缓存；锁失败仍可能回源，不是绝对串行重建。
 * 文件路径：src/main/java/com/agentgate/utils/CacheClient.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class CacheClient {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    /**
     * 读取并反序列化缓存；缺失或无法解析时返回空值，脏数据会被删除。
     * 空值标记与未命中的区别由 getOrLoad 继续检查键是否存在。
     */
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

    /**
     * 将对象序列化为 JSON 并写缓存，TTL 加随机抖动以分散失效时间。
     */
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

    /**
     * 写入短期空值标记，减少对不存在资源的重复数据库查询。
     */
    public void setNull(String key) {
        redisTemplate.opsForValue().set(key, RedisConstants.CACHE_NULL_VALUE, 1, TimeUnit.MINUTES);
    }

    /**
     * 删除指定缓存键；不删除数据库业务记录。
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Cache Aside + 空值缓存 + Redisson 互斥重建。
     */
    public <T> T getOrLoad(String key, Class<T> type, long ttlMinutes, Supplier<T> loader) {
        // 再次检查缓存：其他线程可能已完成重建，此时无需重复查数据库。
        T cached = get(key, type);
        // 缓存中已有真实对象，直接返回，避免重复回源。
        if (cached != null) {
            return cached;
        }
        // get 返回空值但键存在，表示命中空值缓存，直接返回不存在。
        if (redisTemplate.hasKey(key)) {
            // 空值缓存命中
            return null;
        }
        // 同一个业务缓存键使用同一把分布式锁，降低并发回源压力。
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_CACHE_KEY + key);
        boolean locked = false;
        try {
            // 教学说明：最多等待 2 秒，锁租期 10 秒；显式租期不是永久保护，回源时间超过租期仍可能并发重建。
            locked = lock.tryLock(2, 10, TimeUnit.SECONDS);
            // 只有获得锁的线程进入重建分支；仍要二次查缓存，防止等待期间别人已完成重建。
            if (locked) {
                // 再次检查缓存：其他线程可能已完成重建，此时无需重复查数据库。
                cached = get(key, type);
                // 缓存中已有真实对象，直接返回，避免重复回源。
                if (cached != null) {
                    return cached;
                }
                // get 返回空值但键存在，表示命中空值缓存，直接返回不存在。
                if (redisTemplate.hasKey(key)) {
                    return null;
                }
                // 只有锁持有者回源，避免缓存失效时大量线程同时查询数据库。
                T value = loader.get();
                // 缓存空值结果，避免资源不存在时每次请求都查询数据库。
                if (value == null) {
                    setNull(key);
                    return null;
                }
                set(key, value, ttlMinutes);
                return value;
            }
            // 未抢到锁：等待一小段时间后重试缓存，仍没有再回源
            Thread.sleep(100);
            // 再次检查缓存：其他线程可能已完成重建，此时无需重复查数据库。
            cached = get(key, type);
            // 先返回真实缓存；若 key 存在则说明命中空值缓存，避免再次回源。
            if (cached != null) {
                return cached;
            }
            if (redisTemplate.hasKey(key)) {
                return null;
            }
            // 锁等待超时且缓存仍未建立时回源，优先保证请求可用性。
            return loader.get();
        } catch (InterruptedException e) {
            // 恢复中断标记，让上层仍能感知中断；本分支按现有实现继续回源。
            Thread.currentThread().interrupt();
            // 中断后无法继续等待锁，直接回源完成当前请求。
            return loader.get();
        } finally {
            // 仅由持锁线程释放锁；finally 路径确保异常时也尝试释放。
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
