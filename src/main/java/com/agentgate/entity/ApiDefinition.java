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
/**
 * 教学说明：数据库实体：字段对应表记录，TableName 指定表，TableId 标记主键，TableField 声明时间填充等规则。Data 自动生成访问器；JsonIgnore 字段不会出现在普通 JSON 返回中。对照下方字段及建表 SQL 阅读，不应将实体误认为业务服务。
 * 文件路径：src/main/java/com/agentgate/entity/ApiDefinition.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
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
