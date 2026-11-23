package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
/**
 * 教学说明：接收注册用户名、密码、昵称，使用 NotBlank/Size 声明必填和长度约束；配合 Controller 的 Valid 生效。
 * 文件路径：src/main/java/com/agentgate/dto/RegisterFormDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class RegisterFormDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    private String password;

    @Size(max = 32, message = "昵称最长 32 个字符")
    private String nickname;
}
