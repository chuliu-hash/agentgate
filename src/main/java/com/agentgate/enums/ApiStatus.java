package com.agentgate.enums;

public enum ApiStatus {
    DRAFT,
    PUBLISHED,
    OFFLINE;

    public boolean canTransitionTo(ApiStatus target) {
        if (this == target) {
            return false;
        }
        switch (this) {
            case DRAFT:
                return target == PUBLISHED;
            case PUBLISHED:
                return target == OFFLINE;
            case OFFLINE:
                return target == PUBLISHED;
            default:
                return false;
        }
    }
}
