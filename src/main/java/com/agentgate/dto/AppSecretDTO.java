package com.agentgate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppSecretDTO {

    private Long appId;
    private String appKey;
    /** 仅在创建或重置时返回一次 */
    private String appSecret;
}
