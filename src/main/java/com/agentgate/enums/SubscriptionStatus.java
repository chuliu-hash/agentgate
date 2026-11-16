package com.agentgate.enums;

public enum SubscriptionStatus {
    PENDING,
    ACTIVE,
    REJECTED;

    public boolean canTransitionTo(SubscriptionStatus target) {
        if (this == target) {
            return false;
        }
        switch (this) {
            case PENDING:
                return target == ACTIVE || target == REJECTED;
            case ACTIVE:
                return target == REJECTED;
            case REJECTED:
                return target == PENDING;
            default:
                return false;
        }
    }
}
