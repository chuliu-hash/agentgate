package com.agentgate.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_audit_log")
/**
 * 教学说明：数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。
 * 文件路径：src/main/java/com/agentgate/entity/AuditLog.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detail;
    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
