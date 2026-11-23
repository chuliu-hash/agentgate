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
/**
 * 教学说明：生成 AK/SK、AES 加密保存 Secret；getOwnedApp 校验所有权；重置/禁用/白名单变更后删缓存；getAuthInfo 解密 SK 后缓存供验签使用。
 * 文件路径：src/main/java/com/agentgate/service/impl/AppServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AppServiceImpl implements AppService {

    private final AppMapper appMapper;
    private final CacheClient cacheClient;

    @Value("${agentgate.secret-key}")
    private String secretKey;

    /**
     * 使用输入 DTO 创建业务记录；具体唯一性、安全校验与默认状态见方法中的业务分支。
     */
    @Override
    public AppSecretDTO create(AppCreateDTO dto) {
        Long userId = requireUserId();
        // 生成应用标识 AK，用于开放调用时定位应用；它不替代 SK 的身份凭证作用。
        String appKey = "ak_" + RandomUtil.randomString(16);
        // 生成应用 Secret；只在创建或重置结果中返回原文，数据库中保存密文。
        String appSecret = RandomUtil.randomString(32);

        App app = new App();
        app.setUserId(userId);
        app.setName(dto.getName());
        app.setAppKey(appKey);
        // 教学说明：应用 Secret 与登录密码不同：服务端验签要取回原文，因此使用可逆加密存储。
        app.setAppSecretCipher(AesUtils.encrypt(appSecret, secretKey));
        app.setStatus(AppStatus.ACTIVE.name());
        app.setIpWhitelist(dto.getIpWhitelist());
        // 执行插入并由数据库约束最终校验唯一性；实体主键按表配置回填。
        appMapper.insert(app);
        return new AppSecretDTO(app.getId(), appKey, appSecret);
    }

    /**
     * 按当前登录用户或已验证归属的应用查询记录，防止客户端任意选择其他所有者。
     */
    @Override
    public List<App> listMine() {
        // 按下方查询条件及排序读取列表，范围限制决定调用者可以看到哪些记录。
        return appMapper.selectList(
                new LambdaQueryWrapper<App>()
                        .eq(App::getUserId, requireUserId())
                        .orderByDesc(App::getCreateTime));
    }

    /**
     * 验证应用归属后重新生成并加密保存 SK，删除鉴权缓存；新明文仅通过本次结果返回。
     */
    @Override
    public AppSecretDTO resetSecret(Long appId) {
        // 先核实应用存在且归当前登录用户所有，不能仅凭客户端传来的应用主键授权。
        App app = getOwnedApp(appId);
        // 生成应用 Secret；只在创建或重置结果中返回原文，数据库中保存密文。
        String appSecret = RandomUtil.randomString(32);
        // 执行条件更新；留意 WHERE 中的主键和旧状态，以及后续影响行数检查。
        appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getAppSecretCipher, AesUtils.encrypt(appSecret, secretKey))
                .set(App::getUpdateTime, LocalDateTime.now()));
        deleteCache(app.getAppKey());
        return new AppSecretDTO(app.getId(), app.getAppKey(), appSecret);
    }

    /**
     * 验证应用归属后更新白名单并删除鉴权缓存；具体 IP 匹配由开放请求校验执行。
     */
    @Override
    public void updateIpWhitelist(Long appId, String ipWhitelist) {
        // 先核实应用存在且归当前登录用户所有，不能仅凭客户端传来的应用主键授权。
        App app = getOwnedApp(appId);
        // 执行条件更新；留意 WHERE 中的主键和旧状态，以及后续影响行数检查。
        appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getIpWhitelist, ipWhitelist)
                .set(App::getUpdateTime, LocalDateTime.now()));
        deleteCache(app.getAppKey());
    }

    /**
     * 验证应用归属后设置 DISABLED 并删除鉴权缓存；保留应用及其历史关联记录。
     */
    @Override
    public void disable(Long appId) {
        // 先核实应用存在且归当前登录用户所有，不能仅凭客户端传来的应用主键授权。
        App app = getOwnedApp(appId);
        // 执行条件更新；留意 WHERE 中的主键和旧状态，以及后续影响行数检查。
        appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, appId)
                .set(App::getStatus, AppStatus.DISABLED.name())
                .set(App::getUpdateTime, LocalDateTime.now()));
        deleteCache(app.getAppKey());
    }

    /**
     * 按主键读取业务记录；不存在时抛出对应业务异常，不隐式执行所有权校验。
     */
    @Override
    public App getById(Long appId) {
        // 按数据库主键查询；查到记录后仍需根据当前操作检查存在性或业务权限。
        App app = appMapper.selectById(appId);
        // 校验条件成立时拒绝本次操作，抛出 APP_NOT_FOUND；不再继续后续正常业务步骤。
        if (app == null) {
            throw new BizException(ErrorCode.APP_NOT_FOUND);
        }
        return app;
    }

    /**
     * 按 AK 查询并缓存验签资料，数据库 SK 解密后参与缓存；返回模型包含明文 Secret，不得公开输出。
     */
    @Override
    public AppAuthInfo getAuthInfo(String appKey) {
        if (!StringUtils.hasText(appKey)) {
            return null;
        }
        String key = RedisConstants.CACHE_APP_KEY + appKey;
        return cacheClient.getOrLoad(key, AppAuthInfo.class,
                RedisConstants.CACHE_APP_TTL_MINUTES, () -> loadAuthInfo(appKey));
    }

    /** 从数据库加载应用鉴权资料，并解密 Secret 供签名校验使用。 */
    private AppAuthInfo loadAuthInfo(String appKey) {
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
    }

    /**
     * 查询应用并比对当前登录用户 ID；不存在或非所有者时拒绝操作，不额外校验应用是否启用。
     */
    @Override
    // 先核实应用存在且归当前登录用户所有，不能仅凭客户端传来的应用主键授权。
    public App getOwnedApp(Long appId) {
        App app = getById(appId);
        // 校验条件成立时拒绝本次操作，抛出 APP_ACCESS_DENIED；不再继续后续正常业务步骤。
        if (!app.getUserId().equals(requireUserId())) {
            throw new BizException(ErrorCode.APP_ACCESS_DENIED);
        }
        return app;
    }

    /**
     * 从当前线程取登录用户 ID，缺失时抛未登录异常。
     */
    private Long requireUserId() {
        Long userId = UserHolder.getUserId();
        // 校验条件成立时拒绝本次操作，抛出 UNAUTHORIZED；不再继续后续正常业务步骤。
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 删除对应业务缓存，使下一次读取重新加载；删除与数据库提交不是一个跨存储原子事务。
     */
    private void deleteCache(String appKey) {
        cacheClient.delete(RedisConstants.CACHE_APP_KEY + appKey);
    }
}
