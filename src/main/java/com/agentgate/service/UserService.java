package com.agentgate.service;

import com.agentgate.dto.LoginFormDTO;
import com.agentgate.dto.LoginResultDTO;
import com.agentgate.dto.RegisterFormDTO;
import com.agentgate.dto.UserDTO;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/UserService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface UserService {

    /**
     * 注册开发者账号；检查用户名并用 BCrypt 保存密码哈希，数据库唯一索引处理并发重名。
     */
    void register(RegisterFormDTO dto);

    /**
     * 校验账号密码及启用状态，生成 Token 并保存 Redis 登录快照；返回 Token 与不含密码的用户信息。
     */
    LoginResultDTO login(LoginFormDTO dto);

    /**
     * 删除 Redis Token 并清理当前线程用户；删除后后续请求不能继续使用该登录态。
     */
    void logout(String token);

    /**
     * 读取当前请求线程的用户快照；未恢复登录态时抛出未登录异常。
     */
    UserDTO me();
}
