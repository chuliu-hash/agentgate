package com.agentgate.service.impl;

import com.agentgate.dto.SubscribeDTO;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.App;
import com.agentgate.entity.Subscription;
import com.agentgate.enums.ApiStatus;
import com.agentgate.enums.ErrorCode;
import com.agentgate.enums.SubscriptionStatus;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.SubscriptionMapper;
import com.agentgate.service.ApiService;
import com.agentgate.service.AppService;
import com.agentgate.service.QuotaPlanService;
import com.agentgate.service.SubscriptionService;
import com.agentgate.utils.CacheClient;
import com.agentgate.utils.RedisConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * 教学说明：申请核验应用归属、API 上架与套餐；联合唯一索引防重复；拒绝后可重申；审批条件更新防并发；getActive 缓存有效订阅，变更删缓存。
 * 文件路径：src/main/java/com/agentgate/service/impl/SubscriptionServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final AppService appService;
    private final ApiService apiService;
    private final QuotaPlanService quotaPlanService;
    private final CacheClient cacheClient;

    /**
     * 申请订阅：核验应用所有者、API 已上架及套餐存在；首次创建 PENDING，拒绝后复用原记录重新待审。联合唯一索引与旧状态条件更新处理并发，不能直接获得调用权限。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Subscription apply(SubscribeDTO dto) {

        // 客户端传来的 appId 不可信；先核验app的所有者是否是当前发起请求的用户，再允许代表此应用申请订阅。
        appService.getOwnedApp(dto.getAppId());

        // API 必须存在且已上架；草稿与下架状态都不能申请订阅。
        ApiDefinition api = apiService.getById(dto.getApiId());
        // 校验条件成立时拒绝本次操作，抛出 API_OFFLINE；不再继续后续正常业务步骤。
        if (!ApiStatus.PUBLISHED.name().equals(api.getStatus())) {
            throw new BizException(ErrorCode.API_OFFLINE);
        }

        // 检查配额套餐是否存在
        quotaPlanService.getById(dto.getQuotaPlanId());

        // 判断当前app是否已有针对相同 api 的订阅
        Subscription existing = findOne(dto.getAppId(), dto.getApiId());

        // 订阅不存在，创建订阅
        if (existing == null) {
            Subscription subscription = new Subscription();
            subscription.setAppId(dto.getAppId());
            subscription.setApiId(dto.getApiId());
            subscription.setQuotaPlanId(dto.getQuotaPlanId());
            subscription.setStatus(SubscriptionStatus.PENDING.name());
            try {
                // 执行插入并由数据库约束最终校验唯一性；实体主键按表配置回填。
                subscriptionMapper.insert(subscription);
            } catch (DuplicateKeyException e) {
                // 并发请求可能同时查询到“订阅不存在”；
                // 通过 (app_id, api_id) 联合唯一索引，保证同一应用对同一 API 只有一条订阅。
                throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
            }
            // 删除订阅缓存
            deleteCache(dto.getAppId(), dto.getApiId());
            return subscription;
        }

        // 拒绝后复用原记录改回 PENDING，更新套餐并清空拒绝原因，仍需管理员重新审批。
        if (SubscriptionStatus.REJECTED.name().equals(existing.getStatus())) {
            // 执行条件更新；留意 WHERE 中的主键和旧状态，以及后续影响行数检查。
            int rows = subscriptionMapper.update(null, new LambdaUpdateWrapper<Subscription>()
                    .eq(Subscription::getId, existing.getId())
                    .eq(Subscription::getStatus, SubscriptionStatus.REJECTED.name())
                    .set(Subscription::getStatus, SubscriptionStatus.PENDING.name())
                    .set(Subscription::getQuotaPlanId, dto.getQuotaPlanId())
                    .set(Subscription::getRejectReason, null)
                    .set(Subscription::getUpdateTime, LocalDateTime.now()));  // entity为null, FieldFill不会自动填充

            // 条件更新未命中：记录可能已被其他请求修改或删除。
            if (rows == 0) {
                throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
            }
            deleteCache(dto.getAppId(), dto.getApiId());
            return findOne(dto.getAppId(), dto.getApiId());
        }

        throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
    }

    /**
     * 按当前登录用户或已验证归属的应用查询记录，防止客户端任意选择其他所有者。
     */
    @Override
    public List<Subscription> listMine(Long appId) {
        // 先核实应用存在且归当前登录用户所有，不能仅凭客户端传来的应用主键授权。
        App app = appService.getOwnedApp(appId);
        // 按下方查询条件及排序读取列表，范围限制决定调用者可以看到哪些记录。
        return subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getAppId, app.getId())
                        .orderByDesc(Subscription::getCreateTime));
    }

    /**
     * 查询 PENDING 订阅并按创建时间排列，供管理员处理待办。
     */
    @Override
    public List<Subscription> listPending() {
        // 按下方查询条件及排序读取列表，范围限制决定调用者可以看到哪些记录。
        return subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getStatus, SubscriptionStatus.PENDING.name())
                        .orderByAsc(Subscription::getCreateTime));
    }

    /**
     * 查询业务记录列表；管理员权限由调用入口的登录拦截器控制。
     */
    @Override
    public List<Subscription> listAll() {
        // 按下方查询条件及排序读取列表，范围限制决定调用者可以看到哪些记录。
        return subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .orderByDesc(Subscription::getCreateTime));
    }

    /**
     * 审批待审订阅，按结果转为 ACTIVE 或 REJECTED；更新条件包含旧状态 PENDING，未命中时拒绝覆盖并发修改。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, boolean approved, String reason) {
        // 按数据库主键查询；查到记录后仍需根据当前操作检查存在性或业务权限。
        Subscription subscription = subscriptionMapper.selectById(id);
        // 校验条件成立时拒绝本次操作，抛出 SUBSCRIPTION_NOT_FOUND；不再继续后续正常业务步骤。
        if (subscription == null) {
            throw new BizException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
        // 校验条件成立时拒绝本次操作，抛出 STATE_TRANSITION_INVALID；不再继续后续正常业务步骤。
        if (!SubscriptionStatus.PENDING.name().equals(subscription.getStatus())) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "只有待审批的订阅可以审批");
        }
        SubscriptionStatus target = approved ? SubscriptionStatus.ACTIVE : SubscriptionStatus.REJECTED;
        // 执行条件更新；留意 WHERE 中的主键和旧状态，以及后续影响行数检查。
        int rows = subscriptionMapper.update(null, new LambdaUpdateWrapper<Subscription>()
                .eq(Subscription::getId, id)
                .eq(Subscription::getStatus, SubscriptionStatus.PENDING.name())
                .set(Subscription::getStatus, target.name())
                .set(Subscription::getRejectReason, approved ? null : reason)
                .set(Subscription::getUpdateTime, LocalDateTime.now()));
        // 校验条件成立时拒绝本次操作，抛出 STATE_TRANSITION_INVALID；不再继续后续正常业务步骤。
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "订阅状态已被其他操作修改");
        }
        deleteCache(subscription.getAppId(), subscription.getApiId());
    }

    /**
     * 按应用/API 读取 ACTIVE 订阅并缓存；不存在时返回空值，不代表调用方已拥有应用管理权限。
     */
    @Override
    public Subscription getActive(Long appId, Long apiId) {
        String key = RedisConstants.CACHE_SUB_KEY + appId + ":" + apiId;
        return cacheClient.getOrLoad(key, Subscription.class,
                RedisConstants.CACHE_SUB_TTL_MINUTES, () -> findActive(appId, apiId));
    }

    /** 查询可用于开放调用的 ACTIVE 订阅；无匹配记录时返回空值并由缓存层处理空值缓存。 */
    private Subscription findActive(Long appId, Long apiId) {
        return subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getAppId, appId)
                        .eq(Subscription::getApiId, apiId)
                        .eq(Subscription::getStatus, SubscriptionStatus.ACTIVE.name())
                        .last("limit 1"));
    }

    /**
     * 按应用与 API 查询已有订阅，不限制审批状态；用于判断首次申请与拒绝后重申。
     */
    private Subscription findOne(Long appId, Long apiId) {
        return subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getAppId, appId)
                        .eq(Subscription::getApiId, apiId)
                        .last("limit 1"));
    }

    /**
     * 删除对应业务缓存，使下一次读取重新加载；删除与数据库提交不是一个跨存储原子事务。
     */
    private void deleteCache(Long appId, Long apiId) {
        cacheClient.delete(RedisConstants.CACHE_SUB_KEY + appId + ":" + apiId);
    }
}
