package com.agentgate.service;

import com.agentgate.dto.AppAuthInfo;
import com.agentgate.dto.AppCreateDTO;
import com.agentgate.dto.AppSecretDTO;
import com.agentgate.entity.App;

import java.util.List;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/AppService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface AppService {

    /**
     * 使用输入 DTO 创建业务记录；具体唯一性、安全校验与默认状态见方法中的业务分支。
     */
    AppSecretDTO create(AppCreateDTO dto);

    /**
     * 按当前登录用户或已验证归属的应用查询记录，防止客户端任意选择其他所有者。
     */
    List<App> listMine();

    /**
     * 验证应用归属后重新生成并加密保存 SK，删除鉴权缓存；新明文仅通过本次结果返回。
     */
    AppSecretDTO resetSecret(Long appId);

    /**
     * 验证应用归属后更新白名单并删除鉴权缓存；具体 IP 匹配由开放请求校验执行。
     */
    void updateIpWhitelist(Long appId, String ipWhitelist);

    /**
     * 验证应用归属后设置 DISABLED 并删除鉴权缓存；保留应用及其历史关联记录。
     */
    void disable(Long appId);

    /**
     * 按主键读取业务记录；不存在时抛出对应业务异常，不隐式执行所有权校验。
     */
    App getById(Long appId);

    /** 校验当前登录用户是否拥有该应用 */
    /**
     * 查询应用并比对当前登录用户 ID；不存在或非所有者时拒绝操作，不额外校验应用是否启用。
     */
    // 先核实应用存在且归当前登录用户所有，不能仅凭客户端传来的应用主键授权。
    App getOwnedApp(Long appId);

    /**
     * 按 AK 查询并缓存验签资料，数据库 SK 解密后参与缓存；返回模型包含明文 Secret，不得公开输出。
     */
    AppAuthInfo getAuthInfo(String appKey);
}
