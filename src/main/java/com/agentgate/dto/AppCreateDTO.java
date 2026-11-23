package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
/**
 * 教学说明：应用创建输入：名称和 IP 白名单，字段校验约束见源码；所有者从当前登录上下文取得，不能由客户端指定。
 * 文件路径：src/main/java/com/agentgate/dto/AppCreateDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AppCreateDTO {

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 64, message = "应用名称最长 64 个字符")
    private String name;

    @Size(max = 512, message = "IP 白名单最长 512 个字符")
    private String ipWhitelist;
}
