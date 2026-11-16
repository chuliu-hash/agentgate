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
@TableName("tb_call_log")
public class CallLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private Long appId;
    private Long apiId;
    private String method;
    private String path;
    private Integer statusCode;
    private Long latencyMs;
    private String clientIp;
    private Integer errorCode;
    private Integer requestSize;
    private Integer responseSize;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
