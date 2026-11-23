package com.agentgate.utils;
/**
 * 教学说明：集中管理登录、Nonce、鉴权缓存、订阅缓存及锁 key 前缀和有效期；改前缀会影响已有缓存与登录态。
 * 文件路径：src/main/java/com/agentgate/utils/RedisConstants.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public final class RedisConstants {

    /**
     * 私有构造器禁止直接实例化，仅通过静态方法使用此工具类。
     */
    private RedisConstants() {
    }

    public static final String LOGIN_TOKEN_KEY = "login:token:";
    public static final long LOGIN_TOKEN_TTL_MINUTES = 120L;

    public static final String CACHE_APP_KEY = "cache:app:";
    public static final long CACHE_APP_TTL_MINUTES = 30L;

    public static final String CACHE_API_KEY = "cache:api:";
    public static final long CACHE_API_TTL_MINUTES = 10L;

    public static final String CACHE_SUB_KEY = "cache:sub:";
    public static final long CACHE_SUB_TTL_MINUTES = 10L;

    public static final String CACHE_NULL_VALUE = "";

    public static final String NONCE_KEY = "nonce:";
    public static final String RATE_LIMIT_KEY = "ratelimit:";
    public static final String QUOTA_DAY_KEY = "quota:day:";
    public static final String QUOTA_MONTH_KEY = "quota:month:";
    public static final String LOCK_CACHE_KEY = "lock:cache:";
}
