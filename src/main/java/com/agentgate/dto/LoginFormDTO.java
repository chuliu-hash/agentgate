package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
/**
 * 教学说明：接收用户名和密码，声明非空校验；不是数据库实体，也不保存登录状态。
 * 文件路径：src/main/java/com/agentgate/dto/LoginFormDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class LoginFormDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
