package com.agentgate.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 开放接口鉴权所需的内部信息，包含解密后的 secret，禁止直接返回给客户端。
 */
@Data
public class AppAuthInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String appKey;
    private String appSecret;
    private String status;
    private String ipWhitelist;
}
