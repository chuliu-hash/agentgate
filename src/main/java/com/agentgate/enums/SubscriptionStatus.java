package com.agentgate.enums;
/**
 * 教学说明：PENDING/ACTIVE/REJECTED 及允许流转规则；枚举允许不代表 Controller 已暴露所有转换入口。
 * 文件路径：src/main/java/com/agentgate/enums/SubscriptionStatus.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public enum SubscriptionStatus {
    PENDING,
    ACTIVE,
    REJECTED;

    /**
     * 判断枚举定义是否允许转换到目标状态；只返回规则判断，不执行数据库更新或权限校验。
     */
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
