package com.agentgate.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 开放接口鉴权所需的内部信息，包含解密后的 secret，禁止直接返回给客户端。
 */
@Data
/**
 * 教学说明：开放接口鉴权缓存模型，含应用 ID、所有者、AK、明文 SK、状态与白名单；供 Filter 验签，不是公开返回模型。
 * 文件路径：src/main/java/com/agentgate/dto/AppAuthInfo.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AppAuthInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String appKey;
    private String appSecret;
    private String status;
    private String ipWhitelist;
}
