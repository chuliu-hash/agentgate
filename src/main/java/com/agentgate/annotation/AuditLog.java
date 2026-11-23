package com.agentgate.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理操作审计标记：标注在需要记录操作的 Controller 方法上。
 * RUNTIME 使切面在运行时能读取 action 和 resourceType；注解自身不执行数据库写入。
 * 实际记录由 AuditLogAspect 完成，区别于保存数据库记录的 entity.AuditLog。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 声明管理操作名称，供审计切面读取，不自行触发记录。
     */
    String action();

    /**
     * 声明操作资源类型，用于审计分类。
     */
    String resourceType() default "";
}
