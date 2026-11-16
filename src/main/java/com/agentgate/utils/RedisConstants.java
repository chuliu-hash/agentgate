package com.agentgate.utils;

public final class RedisConstants {

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
