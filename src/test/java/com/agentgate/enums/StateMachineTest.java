package com.agentgate.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateMachineTest {

    @Test
    void apiStateTransitions() {
        assertTrue(ApiStatus.DRAFT.canTransitionTo(ApiStatus.PUBLISHED));
        assertTrue(ApiStatus.PUBLISHED.canTransitionTo(ApiStatus.OFFLINE));
        assertTrue(ApiStatus.OFFLINE.canTransitionTo(ApiStatus.PUBLISHED));

        assertFalse(ApiStatus.DRAFT.canTransitionTo(ApiStatus.OFFLINE));
        assertFalse(ApiStatus.PUBLISHED.canTransitionTo(ApiStatus.DRAFT));
        assertFalse(ApiStatus.PUBLISHED.canTransitionTo(ApiStatus.PUBLISHED));
    }

    @Test
    void subscriptionStateTransitions() {
        assertTrue(SubscriptionStatus.PENDING.canTransitionTo(SubscriptionStatus.ACTIVE));
        assertTrue(SubscriptionStatus.PENDING.canTransitionTo(SubscriptionStatus.REJECTED));
        assertTrue(SubscriptionStatus.REJECTED.canTransitionTo(SubscriptionStatus.PENDING));

        assertFalse(SubscriptionStatus.ACTIVE.canTransitionTo(SubscriptionStatus.PENDING));
        assertFalse(SubscriptionStatus.PENDING.canTransitionTo(SubscriptionStatus.PENDING));
    }
}
