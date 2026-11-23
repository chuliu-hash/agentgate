package com.agentgate.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * 教学说明：JUnit 5 API 与订阅状态转换测试，验证枚举允许/拒绝的边；不替代并发数据库状态更新测试。
 * 文件路径：src/test/java/com/agentgate/enums/StateMachineTest.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

class StateMachineTest {

    /**
     * 验证 API 状态允许与禁止的转换。
     */
    @Test
    void apiStateTransitions() {
        assertTrue(ApiStatus.DRAFT.canTransitionTo(ApiStatus.PUBLISHED));
        assertTrue(ApiStatus.PUBLISHED.canTransitionTo(ApiStatus.OFFLINE));
        assertTrue(ApiStatus.OFFLINE.canTransitionTo(ApiStatus.PUBLISHED));

        assertFalse(ApiStatus.DRAFT.canTransitionTo(ApiStatus.OFFLINE));
        assertFalse(ApiStatus.PUBLISHED.canTransitionTo(ApiStatus.DRAFT));
        assertFalse(ApiStatus.PUBLISHED.canTransitionTo(ApiStatus.PUBLISHED));
    }

    /**
     * 验证订阅状态转换规则，不代表接口已开放所有转换。
     */
    @Test
    void subscriptionStateTransitions() {
        assertTrue(SubscriptionStatus.PENDING.canTransitionTo(SubscriptionStatus.ACTIVE));
        assertTrue(SubscriptionStatus.PENDING.canTransitionTo(SubscriptionStatus.REJECTED));
        assertTrue(SubscriptionStatus.REJECTED.canTransitionTo(SubscriptionStatus.PENDING));

        assertFalse(SubscriptionStatus.ACTIVE.canTransitionTo(SubscriptionStatus.PENDING));
        assertFalse(SubscriptionStatus.PENDING.canTransitionTo(SubscriptionStatus.PENDING));
    }
}
