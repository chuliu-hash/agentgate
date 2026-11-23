package com.agentgate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * 教学说明：创建或重置应用 Secret 的输出，包含应用 ID、AK 和明文 SK；普通应用列表不应用它长期暴露密钥。
 * 文件路径：src/main/java/com/agentgate/dto/AppSecretDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AppSecretDTO {

    private Long appId;
    private String appKey;
    /** 仅在创建或重置时返回一次 */
    private String appSecret;
}
