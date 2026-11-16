package com.agentgate.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.agentgate.dto.LoginFormDTO;
import com.agentgate.dto.LoginResultDTO;
import com.agentgate.dto.RegisterFormDTO;
import com.agentgate.dto.UserDTO;
import com.agentgate.entity.User;
import com.agentgate.enums.ErrorCode;
import com.agentgate.enums.UserRole;
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
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void register(RegisterFormDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRole(UserRole.DEVELOPER.name());
        user.setStatus("ACTIVE");
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时唯一索引兜底
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
    }

    @Override
    public LoginResultDTO login(LoginFormDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, dto.getUsername())
                        .last("limit 1"));
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String key = RedisConstants.LOGIN_TOKEN_KEY + token;
        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(user.getId()));
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("role", user.getRole());
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, RedisConstants.LOGIN_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setNickname(user.getNickname());
        userDTO.setRole(user.getRole());
        return new LoginResultDTO(token, userDTO);
    }

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            redisTemplate.delete(RedisConstants.LOGIN_TOKEN_KEY + token);
        }
        UserHolder.removeUser();
    }

    @Override
    public UserDTO me() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
