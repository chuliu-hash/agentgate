package com.agentgate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * 教学说明：登录结果：Token 与 UserDTO；客户端应保存 Token 并放入后续 Authorization 请求头。
 * 文件路径：src/main/java/com/agentgate/dto/LoginResultDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class LoginResultDTO {

    private String token;
    private UserDTO userDTO;
}
