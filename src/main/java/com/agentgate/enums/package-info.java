/**
 * 业务枚举层：集中定义角色、状态和错误码。状态转换规则仅表达允许的关系，实际操作还必须在 Service 中检查权限并通过条件更新处理并发。
 *
 * <p>建议结合 docs/learning-guide.md 阅读，先跟踪请求链路，再观察数据库与缓存变化。</p>
 */
package com.agentgate.enums;
