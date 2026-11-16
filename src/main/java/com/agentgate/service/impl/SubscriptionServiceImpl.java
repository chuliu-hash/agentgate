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
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final AppService appService;
    private final ApiService apiService;
    private final QuotaPlanService quotaPlanService;
    private final CacheClient cacheClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Subscription apply(SubscribeDTO dto) {
        App app = appService.getOwnedApp(dto.getAppId());
        ApiDefinition api = apiService.getById(dto.getApiId());
        if (!ApiStatus.PUBLISHED.name().equals(api.getStatus())) {
            throw new BizException(ErrorCode.API_OFFLINE);
        }
        quotaPlanService.getById(dto.getQuotaPlanId());

        Subscription existing = findOne(dto.getAppId(), dto.getApiId());
        if (existing == null) {
            Subscription subscription = new Subscription();
            subscription.setAppId(dto.getAppId());
            subscription.setApiId(dto.getApiId());
            subscription.setQuotaPlanId(dto.getQuotaPlanId());
            subscription.setStatus(SubscriptionStatus.PENDING.name());
            try {
                subscriptionMapper.insert(subscription);
            } catch (DuplicateKeyException e) {
                throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
            }
            deleteCache(dto.getAppId(), dto.getApiId());
            return subscription;
        }
        if (SubscriptionStatus.REJECTED.name().equals(existing.getStatus())) {
            int rows = subscriptionMapper.update(null, new LambdaUpdateWrapper<Subscription>()
                    .eq(Subscription::getId, existing.getId())
                    .eq(Subscription::getStatus, SubscriptionStatus.REJECTED.name())
                    .set(Subscription::getStatus, SubscriptionStatus.PENDING.name())
                    .set(Subscription::getQuotaPlanId, dto.getQuotaPlanId())
                    .set(Subscription::getRejectReason, null)
                    .set(Subscription::getUpdateTime, LocalDateTime.now()));
            if (rows == 0) {
                throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
            }
            deleteCache(dto.getAppId(), dto.getApiId());
            return findOne(dto.getAppId(), dto.getApiId());
        }
        throw new BizException(ErrorCode.SUBSCRIPTION_EXISTS);
    }

    @Override
    public List<Subscription> listMine(Long appId) {
        App app = appService.getOwnedApp(appId);
        return subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getAppId, app.getId())
                        .orderByDesc(Subscription::getCreateTime));
    }

    @Override
    public List<Subscription> listPending() {
        return subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getStatus, SubscriptionStatus.PENDING.name())
                        .orderByAsc(Subscription::getCreateTime));
    }

    @Override
    public List<Subscription> listAll() {
        return subscriptionMapper.selectList(
                new LambdaQueryWrapper<Subscription>()
                        .orderByDesc(Subscription::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, boolean approved, String reason) {
        Subscription subscription = subscriptionMapper.selectById(id);
        if (subscription == null) {
            throw new BizException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }
        if (!SubscriptionStatus.PENDING.name().equals(subscription.getStatus())) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "只有待审批的订阅可以审批");
        }
        SubscriptionStatus target = approved ? SubscriptionStatus.ACTIVE : SubscriptionStatus.REJECTED;
        int rows = subscriptionMapper.update(null, new LambdaUpdateWrapper<Subscription>()
                .eq(Subscription::getId, id)
                .eq(Subscription::getStatus, SubscriptionStatus.PENDING.name())
                .set(Subscription::getStatus, target.name())
                .set(Subscription::getRejectReason, approved ? null : reason)
                .set(Subscription::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_TRANSITION_INVALID, "订阅状态已被其他操作修改");
        }
        deleteCache(subscription.getAppId(), subscription.getApiId());
    }

    @Override
    public Subscription getActive(Long appId, Long apiId) {
        String key = RedisConstants.CACHE_SUB_KEY + appId + ":" + apiId;
        return cacheClient.getOrLoad(key, Subscription.class,
                RedisConstants.CACHE_SUB_TTL_MINUTES, () -> subscriptionMapper.selectOne(
                        new LambdaQueryWrapper<Subscription>()
                                .eq(Subscription::getAppId, appId)
                                .eq(Subscription::getApiId, apiId)
                                .eq(Subscription::getStatus, SubscriptionStatus.ACTIVE.name())
                                .last("limit 1")));
    }

    private Subscription findOne(Long appId, Long apiId) {
        return subscriptionMapper.selectOne(
                new LambdaQueryWrapper<Subscription>()
                        .eq(Subscription::getAppId, appId)
                        .eq(Subscription::getApiId, apiId)
                        .last("limit 1"));
    }

    private void deleteCache(Long appId, Long apiId) {
        cacheClient.delete(RedisConstants.CACHE_SUB_KEY + appId + ":" + apiId);
    }
}
