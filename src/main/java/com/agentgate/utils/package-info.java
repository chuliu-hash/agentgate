/**
 * 公共工具层：封装签名、缓存、加密、地址校验和线程上下文等复用逻辑。静态方法不会自动获得 Spring 事务；ThreadLocal 与 MDC 要在请求结束清理。
 *
 * <p>建议结合 docs/learning-guide.md 阅读，先跟踪请求链路，再观察数据库与缓存变化。</p>
 */
package com.agentgate.utils;
