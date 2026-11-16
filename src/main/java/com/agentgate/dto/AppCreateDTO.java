package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AppCreateDTO {

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 64, message = "应用名称最长 64 个字符")
    private String name;

    @Size(max = 512, message = "IP 白名单最长 512 个字符")
    private String ipWhitelist;
}
