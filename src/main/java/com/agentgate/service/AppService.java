package com.agentgate.service;

import com.agentgate.dto.AppAuthInfo;
import com.agentgate.dto.AppCreateDTO;
import com.agentgate.dto.AppSecretDTO;
import com.agentgate.entity.App;

import java.util.List;

public interface AppService {

    AppSecretDTO create(AppCreateDTO dto);

    List<App> listMine();

    AppSecretDTO resetSecret(Long appId);

    void updateIpWhitelist(Long appId, String ipWhitelist);

    void disable(Long appId);

    App getById(Long appId);

    /** 校验当前登录用户是否拥有该应用 */
    App getOwnedApp(Long appId);

    AppAuthInfo getAuthInfo(String appKey);
}
