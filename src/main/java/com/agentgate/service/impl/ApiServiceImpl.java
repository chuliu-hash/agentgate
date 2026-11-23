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
/**
 * 教学说明：创建/修改时校验 code 和上游地址；发布/下架使用状态规则与旧状态条件更新；按 code 缓存查询；变更删除旧、新 code 缓存。
 * 文件路径：src/main/java/com/agentgate/service/impl/ApiServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class ApiServiceImpl implements ApiService {

    private final ApiDefinitionMapper apiDefinitionMapper;
    private final CacheClient cacheClient;

    @Value("${agentgate.ssrf.allow-loopback:false}")
    private boolean allowLoopback;

    /**
     * 使用输入 DTO 创建业务记录；具体唯一性、安全校验与默认状态见方法中的业务分支。
     */
    @Override
    public ApiDefinition create(ApiCreateDTO dto) {
        // 校验条件成立时拒绝本次操作，抛出 API_CODE_EXISTS；不再继续后续正常业务步骤。
        if (findByCode(dto.getCode()) != null) {
            throw new BizException(ErrorCode.API_CODE_EXISTS);
        }
        // 验证上游地址是否允许访问；每次转发仍需重新验证，不能只依赖登记时检查。
        SsrfUtils.validate(dto.getUpstreamUrl(), allowLoopback);

        ApiDefinition api = new ApiDefinition();
        copy(dto, api);
        api.setStatus(ApiStatus.DRAFT.name());

        try {
            // 执行插入并由数据库约束最终校验唯一性；实体主键按表配置回填。
            apiDefinitionMapper.insert(api);
        } catch (DuplicateKeyException e) {
            // 并发创建时 code 唯一索引兜底
            throw new BizException(ErrorCode.API_CODE_EXISTS);
        }
        return api;
    }

    /**
     * 查询已有记录并复制允许修改的字段，写回数据库；涉及缓存的数据随后失效。
     */
    @Override
    public ApiDefinition update(Long id, ApiCreateDTO dto) {
        ApiDefinition api = getById(id);
        ApiDefinition sameCode = findByCode(dto.getCode());
        // 校验条件成立时拒绝本次操作，抛出 API_CODE_EXISTS；不再继续后续正常业务步骤。
        if (sameCode != null && !sameCode.getId().equals(id)) {
            throw new BizException(ErrorCode.API_CODE_EXISTS);
        }
        // 验证上游地址是否允许访问；每次转发仍需重新验证，不能只依赖登记时检查。
        SsrfUtils.validate(dto.getUpstreamUrl(), allowLoopback);
        String oldCode = api.getCode();
        copy(dto, api);
        apiDefinitionMapper.updateById(api);
        deleteCache(oldCode);
        deleteCache(dto.getCode());
        return api;
    }

    /**
     * 请求将 API 转为 PUBLISHED，实际转换规则与并发检查由状态更新方法执行。
     */
    @Override
    public void publish(Long id) {
        transition(id, ApiStatus.PUBLISHED);
    }

    /**
     * 请求将 API 转为 OFFLINE，保留定义和历史调用记录。
     */
    @Override
    public void offline(Long id) {
        transition(id, ApiStatus.OFFLINE);
    }

    /**
     * 查询业务记录列表；管理员权限由调用入口的登录拦截器控制。
     */
    @Override
    public List<ApiDefinition> listAll() {
        // 按下方查询条件及排序读取列表，范围限制决定调用者可以看到哪些记录。
        return apiDefinitionMapper.selectList(
                new LambdaQueryWrapper<ApiDefinition>().orderByDesc(ApiDefinition::getCreateTime));
    }

    /**
     * 仅查询状态为 PUBLISHED 的 API。
     */
    @Override
    public List<ApiDefinition> listPublished() {
        // 按下方查询条件及排序读取列表，范围限制决定调用者可以看到哪些记录。
        return apiDefinitionMapper.selectList(
                new LambdaQueryWrapper<ApiDefinition>()
                        .eq(ApiDefinition::getStatus, ApiStatus.PUBLISHED.name())
                        .orderByDesc(ApiDefinition::getCreateTime));
    }

    /**
     * 按主键读取业务记录；不存在时抛出对应业务异常，不隐式执行所有权校验。
     */
    @Override
    public ApiDefinition getById(Long id) {
        // 按数据库主键查询；查到记录后仍需根据当前操作检查存在性或业务权限。
        ApiDefinition api = apiDefinitionMapper.selectById(id);
        // 校验条件成立时拒绝本次操作，抛出 API_NOT_FOUND；不再继续后续正常业务步骤。
        if (api == null) {
            throw new BizException(ErrorCode.API_NOT_FOUND);
        }
        return api;
    }

    /**
     * 按业务编码读取 API，通过缓存减少数据库访问；不存在时可能返回空值。
     */
    @Override
    public ApiDefinition getByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String key = RedisConstants.CACHE_API_KEY + code;
        return cacheClient.getOrLoad(key, ApiDefinition.class,
                RedisConstants.CACHE_API_TTL_MINUTES, () -> findByCode(code));
    }

    /**
     * 读取 API 并要求状态为 PUBLISHED；不存在或未上架时拒绝开放调用。
     */
    @Override
    public ApiDefinition getPublishedByCode(String code) {
        ApiDefinition api = getByCode(code);
        // 校验条件成立时拒绝本次操作，抛出 API_NOT_FOUND；不再继续后续正常业务步骤。
        if (api == null) {
            throw new BizException(ErrorCode.API_NOT_FOUND);
        }
        // 校验条件成立时拒绝本次操作，抛出 API_OFFLINE；不再继续后续正常业务步骤。
        if (!ApiStatus.PUBLISHED.name().equals(api.getStatus())) {
            throw new BizException(ErrorCode.API_OFFLINE);
        }
        return api;
    }

    // 教学说明：状态转换不仅要检查枚举规则，还要在 UPDATE 的 WHERE 中带旧状态，防止并发操作互相覆盖。
    /**
     * 验证 API 状态转换规则，用 ID 和旧状态条件更新；影响行数为零时报告并发变化，成功后删除缓存。
     */
    private void transition(Long id, ApiStatus target) {
        ApiDefinition api = getById(id);
        ApiStatus current;
        try {
            current = ApiStatus.valueOf(api.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID);
        }
        // 校验条件成立时拒绝本次操作，抛出 STATE_TRANSITION_INVALID；不再继续后续正常业务步骤。
        if (!current.canTransitionTo(target)) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID,
                    "API 不能从 " + current + " 流转到 " + target);
        }
        // 执行条件更新；留意 WHERE 中的主键和旧状态，以及后续影响行数检查。
        int rows = apiDefinitionMapper.update(null, new LambdaUpdateWrapper<ApiDefinition>()
                .eq(ApiDefinition::getId, id)
                .eq(ApiDefinition::getStatus, current.name())
                .set(ApiDefinition::getStatus, target.name())
                .set(ApiDefinition::getUpdateTime, LocalDateTime.now()));
        // 校验条件成立时拒绝本次操作，抛出 STATE_TRANSITION_INVALID；不再继续后续正常业务步骤。
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "状态已被其他操作修改，请刷新后重试");
        }
        deleteCache(api.getCode());
    }

    /**
     * 直接按 API 编码查询数据库，用于唯一性预检查与缓存加载。
     */
    private ApiDefinition findByCode(String code) {
        return apiDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ApiDefinition>()
                        .eq(ApiDefinition::getCode, code)
                        .last("limit 1"));
    }

    /**
     * 将 DTO 中允许修改的业务字段复制到实体；不复制数据库 ID 或自动生成的状态。
     */
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

    /**
     * 删除对应业务缓存，使下一次读取重新加载；删除与数据库提交不是一个跨存储原子事务。
     */
    private void deleteCache(String code) {
        if (StringUtils.hasText(code)) {
            cacheClient.delete(RedisConstants.CACHE_API_KEY + code);
        }
    }
}
