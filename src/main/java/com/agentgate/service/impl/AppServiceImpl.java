package com.agentgate.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.agentgate.dto.AppAuthInfo;
import com.agentgate.dto.AppCreateDTO;
import com.agentgate.dto.AppSecretDTO;
import com.agentgate.entity.App;
import com.agentgate.enums.AppStatus;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.AppMapper;
import com.agentgate.service.AppService;
import com.agentgate.utils.AesUtils;
import com.agentgate.utils.CacheClient;
import com.agentgate.utils.RedisConstants;
import com.agentgate.utils.UserHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {

    private final AppMapper appMapper;
    private final CacheClient cacheClient;

    @Value("${agentgate.secret-key}")
    private String secretKey;

    @Override
    public AppSecretDTO create(AppCreateDTO dto) {
        Long userId = requireUserId();
        String appKey = "ak_" + RandomUtil.randomString(16);
        String appSecret = RandomUtil.randomString(32);

        App app = new App();
        app.setUserId(userId);
        app.setName(dto.getName());
        app.setAppKey(appKey);
        app.setAppSecretCipher(AesUtils.encrypt(appSecret, secretKey));
        app.setStatus(AppStatus.ACTIVE.name());
        app.setIpWhitelist(dto.getIpWhitelist());
        appMapper.insert(app);
        return new AppSecretDTO(app.getId(), appKey, appSecret);
    }

    @Override
    public List<App> listMine() {
        return appMapper.selectList(
                new LambdaQueryWrapper<App>()
                        .eq(App::getUserId, requireUserId())
                        .orderByDesc(App::getCreateTime));
    }

    @Override
    public AppSecretDTO resetSecret(Long appId) {
        App app = getOwnedApp(appId);
        String appSecret = RandomUtil.randomString(32);
        appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getAppSecretCipher, AesUtils.encrypt(appSecret, secretKey))
                .set(App::getUpdateTime, LocalDateTime.now()));
        deleteCache(app.getAppKey());
        return new AppSecretDTO(app.getId(), app.getAppKey(), appSecret);
    }

    @Override
    public void updateIpWhitelist(Long appId, String ipWhitelist) {
        App app = getOwnedApp(appId);
        appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getIpWhitelist, ipWhitelist)
                .set(App::getUpdateTime, LocalDateTime.now()));
        deleteCache(app.getAppKey());
    }

    @Override
    public void disable(Long appId) {
        App app = getOwnedApp(appId);
        appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getStatus, AppStatus.DISABLED.name())
                .set(App::getUpdateTime, LocalDateTime.now()));
        deleteCache(app.getAppKey());
    }

    @Override
    public App getById(Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new BizException(ErrorCode.APP_NOT_FOUND);
        }
        return app;
    }

    @Override
    public AppAuthInfo getAuthInfo(String appKey) {
        if (!StringUtils.hasText(appKey)) {
            return null;
        }
        String key = RedisConstants.CACHE_APP_KEY + appKey;
        return cacheClient.getOrLoad(key, AppAuthInfo.class,
                RedisConstants.CACHE_APP_TTL_MINUTES, () -> {
                    App app = appMapper.selectOne(
                            new LambdaQueryWrapper<App>()
                                    .eq(App::getAppKey, appKey)
                                    .last("limit 1"));
                    if (app == null) {
                        return null;
                    }
                    AppAuthInfo info = new AppAuthInfo();
                    info.setId(app.getId());
                    info.setUserId(app.getUserId());
                    info.setAppKey(app.getAppKey());
                    info.setStatus(app.getStatus());
                    info.setIpWhitelist(app.getIpWhitelist());
                    info.setAppSecret(AesUtils.decrypt(app.getAppSecretCipher(), secretKey));
                    return info;
                });
    }

    @Override
    public App getOwnedApp(Long appId) {
        App app = getById(appId);
        if (!app.getUserId().equals(requireUserId())) {
            throw new BizException(ErrorCode.APP_ACCESS_DENIED);
        }
        return app;
    }

    private Long requireUserId() {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private void deleteCache(String appKey) {
        cacheClient.delete(RedisConstants.CACHE_APP_KEY + appKey);
    }
}
