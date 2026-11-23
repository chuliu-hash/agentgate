package com.agentgate.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.agentgate.dto.LoginFormDTO;
import com.agentgate.dto.LoginResultDTO;
import com.agentgate.dto.RegisterFormDTO;
import com.agentgate.dto.UserDTO;
import com.agentgate.entity.User;
import com.agentgate.enums.ErrorCode;
import com.agentgate.enums.UserRole;
import com.agentgate.enums.UserStatus;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.UserMapper;
import com.agentgate.service.UserService;
import com.agentgate.utils.RedisConstants;
import com.agentgate.utils.UserHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
/**
 * 教学说明：注册查重并 BCrypt 存密码；登录校验密码和 ACTIVE 状态，生成 UUID Token 写 Redis Hash；登出删 key；me 从 UserHolder 取用户。
 * 文件路径：src/main/java/com/agentgate/service/impl/UserServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 注册开发者账号；检查用户名并用 BCrypt 保存密码哈希，数据库唯一索引处理并发重名。
     */
    @Override
    public void register(RegisterFormDTO dto) {
        // 按用户名统计已有账号；这是友好提示，不能代替数据库唯一索引的并发约束。
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        // 校验条件成立时拒绝本次操作，抛出 USERNAME_EXISTS；不再继续后续正常业务步骤。
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        // 教学说明：BCrypt 每次生成随机盐；数据库只保存哈希，登录时 checkpw 验证，不需要解密密码。
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRole(UserRole.DEVELOPER.name());
        user.setStatus(UserStatus.ACTIVE.name());
        try {
            // 执行插入并由数据库约束最终校验唯一性；实体主键按表配置回填。
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时唯一索引兜底
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
    }

    /**
     * 校验账号密码及启用状态，生成 Token 并保存 Redis 登录快照；返回 Token 与不含密码的用户信息。
     */
    @Override
    public LoginResultDTO login(LoginFormDTO dto) {
        // 按用户名读取账号和密码哈希，后续统一校验，避免直接比较明文密码。
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .last("limit 1"));
        // 校验条件成立时拒绝本次操作，抛出 PASSWORD_ERROR；不再继续后续正常业务步骤。
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        // 校验条件成立时拒绝本次操作，抛出 FORBIDDEN；不再继续后续正常业务步骤。
        if (!UserStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        // 生成不可预测的登录标识；用户信息保存在 Redis，而不是编码进 Token。
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = RedisConstants.LOGIN_TOKEN_KEY + token;
        Map<String, String> map = new HashMap<>();
        // 构造登录用户快照，只写身份与展示字段，不将密码哈希存入会话。
        map.put("id", String.valueOf(user.getId()));
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("role", user.getRole());
        // 将用户快照写入 Token 对应的 Redis Hash，后续请求据此恢复身份。
        redisTemplate.opsForHash().putAll(key, map);
        // 设置或续期会话 TTL，避免未主动登出的会话永久有效。
        redisTemplate.expire(key, RedisConstants.LOGIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);

        // 构造对外用户信息，避免把数据库实体中的密码等内部字段返回客户端。
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setNickname(user.getNickname());
        userDTO.setRole(user.getRole());
        return new LoginResultDTO(token, userDTO);
    }

    /**
     * 删除 Redis Token 并清理当前线程用户；删除后后续请求不能继续使用该登录态。
     */
    @Override
    public void logout(String token) {
        // 只有提供非空 Token 才尝试删除会话；线程上下文仍在后续统一清理。
        if (StringUtils.hasText(token)) {
            // 兼容标准 Bearer 认证头，去掉前缀后才是 Redis 中使用的 Token。
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            redisTemplate.delete(RedisConstants.LOGIN_TOKEN_KEY + token);
        }
        UserHolder.removeUser();
    }

    /**
     * 读取当前请求线程的用户快照；未恢复登录态时抛出未登录异常。
     */
    @Override
    public UserDTO me() {
        UserDTO userDTO = UserHolder.getUser();
        // 校验条件成立时拒绝本次操作，抛出 UNAUTHORIZED；不再继续后续正常业务步骤。
        if (userDTO == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userDTO;
    }
}
