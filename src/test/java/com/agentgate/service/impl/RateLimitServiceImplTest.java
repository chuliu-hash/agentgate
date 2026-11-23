package com.agentgate.service.impl;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证单脚本原子扣减的返回码映射、key/ARGV 构造与拒绝前不访问 Redis 的分支。
 * 固定时钟：UTC 2026-11-23T04:05:06Z 对应 Asia/Shanghai 2026-11-23 12:05:06。
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-23T04:05:06Z");

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RateLimitServiceImpl service;

    private QuotaPlan plan;
    private ApiDefinition api;

    @BeforeEach
    void setUp() {
        plan = new QuotaPlan();
        plan.setQpsLimit(10);
        plan.setDailyQuota(100);
        plan.setMonthlyQuota(1000);

        api = new ApiDefinition();
        api.setId(9L);

        ReflectionTestUtils.setField(service, "clock", Clock.fixed(FIXED_INSTANT, ZONE));
        service.init();
    }

    @Test
    void planIsNull_throwsPlanNotFoundAndSkipsRedis() {
        BizException ex = assertThrows(BizException.class,
                () -> service.checkAndConsume(7L, api, null));

        assertEquals(3009, ex.getCode());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void invalidPlanConfig_throwsBeforeRedis() {
        plan.setDailyQuota(0);

        BizException ex = assertThrows(BizException.class,
                () -> service.checkAndConsume(7L, api, plan));

        assertEquals(3009, ex.getCode());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void nullAppId_throwsParamErrorWithoutRedis() {
        BizException ex = assertThrows(BizException.class,
                () -> service.checkAndConsume(null, api, plan));

        assertEquals(400, ex.getCode());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void allowed_passesAndCallsScriptOnceWithExpectedKeysAndArgs() {
        stubScript(1L);

        service.checkAndConsume(7L, api, plan);

        // 日 TTL：12:05:06 到次日 00:00:00 共 42894 秒，再加 60 秒。
        // 月 TTL：到 12-01 00:00:00 共 647694 秒，再加 60 秒。
        verify(redisTemplate, times(1)).execute(
                any(RedisScript.class),
                eq(Arrays.asList(
                        "ratelimit:7:9",
                        "quota:day:7:9:20261123",
                        "quota:month:7:9:202611")),
                eq("10"),
                eq("10"),
                eq(String.valueOf(FIXED_INSTANT.toEpochMilli())),
                eq("1"),
                eq("100"),
                eq("1000"),
                eq("42954"),
                eq("647754"));
    }

    @Test
    void rateLimitedResult_throwsRateLimited() {
        stubScript(0L);

        BizException ex = assertThrows(BizException.class,
                () -> service.checkAndConsume(7L, api, plan));

        assertEquals(429, ex.getCode());
        verify(redisTemplate, times(1)).execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void dailyQuotaResult_throwsQuotaExceeded() {
        stubScript(-1L);

        BizException ex = assertThrows(BizException.class,
                () -> service.checkAndConsume(7L, api, plan));

        assertEquals(4290, ex.getCode());
    }

    @Test
    void monthlyQuotaResult_throwsQuotaExceeded() {
        stubScript(-2L);

        BizException ex = assertThrows(BizException.class,
                () -> service.checkAndConsume(7L, api, plan));

        assertEquals(4290, ex.getCode());
    }

    @Test
    void nullResult_throwsSystemError() {
        stubScript(null);

        BizException ex = assertThrows(BizException.class,
                () -> service.checkAndConsume(7L, api, plan));

        assertEquals(500, ex.getCode());
    }

    @Test
    void invalidResult_throwsSystemError() {
        stubScript(-3L);

        BizException ex = assertThrows(BizException.class,
                () -> service.checkAndConsume(7L, api, plan));

        assertEquals(500, ex.getCode());
    }

    @Test
    void redisFailure_propagatesAndDoesNotSwallow() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertThrows(RedisConnectionFailureException.class,
                () -> service.checkAndConsume(7L, api, plan));
    }

    private void stubScript(Long result) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(result);
    }
}
