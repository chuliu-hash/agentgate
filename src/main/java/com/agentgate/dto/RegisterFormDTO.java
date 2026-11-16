package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
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
