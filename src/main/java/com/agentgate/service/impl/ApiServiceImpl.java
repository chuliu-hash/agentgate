package com.agentgate.service.impl;

import com.agentgate.dto.ApiCreateDTO;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.enums.ApiStatus;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.ApiDefinitionMapper;
import com.agentgate.service.ApiService;
import com.agentgate.utils.CacheClient;
import com.agentgate.utils.RedisConstants;
import com.agentgate.utils.SsrfUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {

    private final ApiDefinitionMapper apiDefinitionMapper;
    private final CacheClient cacheClient;

    @Value("${agentgate.ssrf.allow-loopback:false}")
    private boolean allowLoopback;

    @Override
    public ApiDefinition create(ApiCreateDTO dto) {
        if (findByCode(dto.getCode()) != null) {
            throw new BizException(ErrorCode.API_CODE_EXISTS);
        }
        SsrfUtils.validate(dto.getUpstreamUrl(), allowLoopback);
        ApiDefinition api = new ApiDefinition();
        copy(dto, api);
        api.setStatus(ApiStatus.DRAFT.name());
        try {
            apiDefinitionMapper.insert(api);
        } catch (DuplicateKeyException e) {
            // 并发创建时 code 唯一索引兜底
            throw new BizException(ErrorCode.API_CODE_EXISTS);
        }
        return api;
    }

    @Override
    public ApiDefinition update(Long id, ApiCreateDTO dto) {
        ApiDefinition api = getById(id);
        ApiDefinition sameCode = findByCode(dto.getCode());
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new BizException(ErrorCode.API_CODE_EXISTS);
        }
        SsrfUtils.validate(dto.getUpstreamUrl(), allowLoopback);
        String oldCode = api.getCode();
        copy(dto, api);
        apiDefinitionMapper.updateById(api);
        deleteCache(oldCode);
        deleteCache(dto.getCode());
        return api;
    }

    @Override
    public void publish(Long id) {
        transition(id, ApiStatus.PUBLISHED);
    }

    @Override
    public void offline(Long id) {
        transition(id, ApiStatus.OFFLINE);
    }

    @Override
    public List<ApiDefinition> listAll() {
        return apiDefinitionMapper.selectList(
                new LambdaQueryWrapper<ApiDefinition>().orderByDesc(ApiDefinition::getCreateTime));
    }

    @Override
    public List<ApiDefinition> listPublished() {
        return apiDefinitionMapper.selectList(
                new LambdaQueryWrapper<ApiDefinition>()
                        .eq(ApiDefinition::getStatus, ApiStatus.PUBLISHED.name())
                        .orderByDesc(ApiDefinition::getCreateTime));
    }

    @Override
    public ApiDefinition getById(Long id) {
        ApiDefinition api = apiDefinitionMapper.selectById(id);
        if (api == null) {
            throw new BizException(ErrorCode.API_NOT_FOUND);
        }
        return api;
    }

    @Override
    public ApiDefinition getByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String key = RedisConstants.CACHE_API_KEY + code;
        return cacheClient.getOrLoad(key, ApiDefinition.class,
                RedisConstants.CACHE_API_TTL_MINUTES, () -> findByCode(code));
    }

    @Override
    public ApiDefinition getPublishedByCode(String code) {
        ApiDefinition api = getByCode(code);
        if (api == null) {
            throw new BizException(ErrorCode.API_NOT_FOUND);
        }
        if (!ApiStatus.PUBLISHED.name().equals(api.getStatus())) {
            throw new BizException(ErrorCode.API_OFFLINE);
        }
        return api;
    }

    private void transition(Long id, ApiStatus target) {
        ApiDefinition api = getById(id);
        ApiStatus current;
        try {
            current = ApiStatus.valueOf(api.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID);
        }
        if (!current.canTransitionTo(target)) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID,
                    "API 不能从 " + current + " 流转到 " + target);
        }
        int rows = apiDefinitionMapper.update(null, new LambdaUpdateWrapper<ApiDefinition>()
                .eq(ApiDefinition::getId, id)
                .eq(ApiDefinition::getStatus, current.name())
                .set(ApiDefinition::getStatus, target.name())
                .set(ApiDefinition::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "状态已被其他操作修改，请刷新后重试");
        }
        deleteCache(api.getCode());
    }

    private ApiDefinition findByCode(String code) {
        return apiDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ApiDefinition>()
                        .eq(ApiDefinition::getCode, code)
                        .last("limit 1"));
    }

    private void copy(ApiCreateDTO dto, ApiDefinition api) {
        api.setCode(dto.getCode());
        api.setName(dto.getName());
        api.setMethod(dto.getMethod());
        api.setUpstreamUrl(dto.getUpstreamUrl());
        api.setVersion(StringUtils.hasText(dto.getVersion()) ? dto.getVersion() : "v1");
        api.setSensitivityLevel(dto.getSensitivityLevel() == null ? 1 : dto.getSensitivityLevel());
        api.setTimeoutMs(dto.getTimeoutMs() == null ? 5000 : dto.getTimeoutMs());
        api.setDescription(dto.getDescription());
    }

    private void deleteCache(String code) {
        if (StringUtils.hasText(code)) {
            cacheClient.delete(RedisConstants.CACHE_API_KEY + code);
        }
    }
}
