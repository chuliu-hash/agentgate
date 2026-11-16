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
@TableName("tb_api")
public class ApiDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 对外暴露的稳定编码，如 weather */
    private String code;
    private String name;
    /** GET / POST / PUT / DELETE / ANY */
    private String method;
    /** 上游基础地址，如 http://127.0.0.1:8081/mock-api/weather */
    private String upstreamUrl;
    private String version;
    /** 1 普通，2 敏感（响应脱敏），3 高敏 */
    private Integer sensitivityLevel;
    private String status;
    private Integer timeoutMs;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
