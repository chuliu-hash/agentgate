package com.agentgate.controller;

import com.agentgate.dto.LoginFormDTO;
import com.agentgate.dto.LoginResultDTO;
import com.agentgate.dto.RegisterFormDTO;
import com.agentgate.dto.Result;
import com.agentgate.dto.UserDTO;
import com.agentgate.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
/**
 * 教学说明：用户 HTTP 入口：接收注册/登录 DTO，触发校验，调用 UserService；提供登出及当前用户查询。业务规则在实现类。
 * 文件路径：src/main/java/com/agentgate/controller/UserController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class UserController {

    private final UserService userService;

    /**
     * HTTP 入口：注册开发者账号；检查用户名并用 BCrypt 保存密码哈希，数据库唯一索引处理并发重名。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterFormDTO dto) {
        userService.register(dto);
        return Result.ok();
    }

    /**
     * HTTP 入口：校验账号密码及启用状态，生成 Token 并保存 Redis 登录快照；返回 Token 与不含密码的用户信息。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping("/login")
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginFormDTO dto) {
        return Result.ok(userService.login(dto));
    }

    /**
     * HTTP 入口：删除 Redis Token 并清理当前线程用户；删除后后续请求不能继续使用该登录态。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "authorization", required = false) String token) {
        userService.logout(token);
        return Result.ok();
    }

    /**
     * HTTP 入口：读取当前请求线程的用户快照；未恢复登录态时抛出未登录异常。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping("/me")
    public Result<UserDTO> me() {
        return Result.ok(userService.me());
    }
}
