/**
 * HTTP 接口层：Mapping 注解决定路径和请求方法；RequestBody 将 JSON 转成 DTO，Valid 触发参数校验。Controller 委托 Service 执行业务，不应信任客户端传来的用户身份。
 *
 * <p>建议结合 docs/learning-guide.md 阅读，先跟踪请求链路，再观察数据库与缓存变化。</p>
 */
package com.agentgate.controller;
