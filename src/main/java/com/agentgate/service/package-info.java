/**
 * 业务契约层：接口定义可用能力及输入输出；Spring 在运行时注入 service.impl 中的实现。接口声明与代理对象不是同一个概念，事务和其他 AOP 依赖经过代理调用。
 *
 * <p>建议结合 docs/learning-guide.md 阅读，先跟踪请求链路，再观察数据库与缓存变化。</p>
 */
package com.agentgate.service;
