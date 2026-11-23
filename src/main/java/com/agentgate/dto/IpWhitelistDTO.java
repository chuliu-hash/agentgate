package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
/**
 * 教学说明：白名单修改输入，限制字符串长度；具体匹配格式由 IpUtils 实现，注解不提供网络地址验证。
 * 文件路径：src/main/java/com/agentgate/dto/IpWhitelistDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class IpWhitelistDTO {

    @Size(max = 512, message = "IP 白名单最长 512 个字符")
    private String ipWhitelist;
}
