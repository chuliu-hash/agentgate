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
