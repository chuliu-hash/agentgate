package com.agentgate.enums;
/**
 * 教学说明：应用 ACTIVE/DISABLED 状态枚举，Filter 拒绝非 ACTIVE 应用；没有自动恢复接口。
 * 文件路径：src/main/java/com/agentgate/enums/AppStatus.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public enum AppStatus {
    ACTIVE,
    DISABLED
}
