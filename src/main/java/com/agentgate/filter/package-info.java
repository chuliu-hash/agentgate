/**
 * Servlet 过滤层：在 Spring MVC Controller 之前执行。TraceIdFilter 先建立日志上下文，SignatureFilter 处理开放调用；必须主动向后调用 filterChain 才会继续。
 *
 * <p>建议结合 docs/learning-guide.md 阅读，先跟踪请求链路，再观察数据库与缓存变化。</p>
 */
package com.agentgate.filter;
