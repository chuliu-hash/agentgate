package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class IpWhitelistDTO {

    @Size(max = 512, message = "IP 白名单最长 512 个字符")
    private String ipWhitelist;
}
