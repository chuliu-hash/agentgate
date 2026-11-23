package com.agentgate.enums;
/**
 * 教学说明：DRAFT/PUBLISHED/OFFLINE 及 canTransitionTo 规则；业务实现还要用条件 SQL 防并发覆盖。
 * 文件路径：src/main/java/com/agentgate/enums/ApiStatus.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public enum ApiStatus {
    DRAFT,
    PUBLISHED,
    OFFLINE;

    /**
     * 判断枚举定义是否允许转换到目标状态；只返回规则判断，不执行数据库更新或权限校验。
     */
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
