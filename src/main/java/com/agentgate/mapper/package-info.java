/**
 * 数据库访问层：BaseMapper 提供常用增删改查，自定义注解 SQL 处理幂等写入与统计。SQL 参数绑定使用占位符；唯一索引是并发安全的最终约束，不能只依赖 Java 预查询。
 *
 * <p>建议结合 docs/learning-guide.md 阅读，先跟踪请求链路，再观察数据库与缓存变化。</p>
 */
package com.agentgate.mapper;
