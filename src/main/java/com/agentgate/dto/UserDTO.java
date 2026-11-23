package com.agentgate.dto;

import lombok.Data;

import java.io.Serializable;

@Data
/**
 * 教学说明：无密码的登录用户快照，用于返回客户端、Redis 会话恢复与 ThreadLocal 用户上下文。
 * 文件路径：src/main/java/com/agentgate/dto/UserDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String role;
}
