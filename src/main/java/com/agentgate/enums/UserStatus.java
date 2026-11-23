package com.agentgate.enums;

/**
 * 用户账号状态：ACTIVE 允许登录，DISABLED 禁止登录。
 * 与 AppStatus 分开定义，分别表达用户账号和调用应用的状态。
 * 持久化时使用 name()，与数据库现有字符串值保持一致。
 */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
