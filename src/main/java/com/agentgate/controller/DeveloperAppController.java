package com.agentgate.controller;

import com.agentgate.dto.AppCreateDTO;
import com.agentgate.dto.AppSecretDTO;
import com.agentgate.dto.IpWhitelistDTO;
import com.agentgate.dto.Result;
import com.agentgate.entity.App;
import com.agentgate.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/developer/apps")
@RequiredArgsConstructor
/**
 * 教学说明：开发者应用入口：创建、列出本人应用、重置 Secret、修改白名单、禁用。ID 路径参数不能代替归属校验，归属在 AppService 实现。
 * 文件路径：src/main/java/com/agentgate/controller/DeveloperAppController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class DeveloperAppController {

    private final AppService appService;

    /**
     * HTTP 入口：使用输入 DTO 创建业务记录；具体唯一性、安全校验与默认状态见方法中的业务分支。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping
    public Result<AppSecretDTO> create(@Valid @RequestBody AppCreateDTO dto) {
        return Result.ok(appService.create(dto));
    }

    /**
     * HTTP 入口：接收请求并返回业务结果。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<List<App>> list() {
        return Result.ok(appService.listMine());
    }

    /**
     * HTTP 入口：验证应用归属后重新生成并加密保存 SK，删除鉴权缓存；新明文仅通过本次结果返回。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping("/{id}/reset-secret")
    public Result<AppSecretDTO> resetSecret(@PathVariable Long id) {
        return Result.ok(appService.resetSecret(id));
    }

    /**
     * HTTP 入口：验证应用归属后更新白名单并删除鉴权缓存；具体 IP 匹配由开放请求校验执行。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PutMapping("/{id}/ip-whitelist")
    public Result<Void> updateIpWhitelist(@PathVariable Long id,
                                          @Valid @RequestBody IpWhitelistDTO dto) {
        appService.updateIpWhitelist(id, dto.getIpWhitelist());
        return Result.ok();
    }

    /**
     * HTTP 入口：验证应用归属后设置 DISABLED 并删除鉴权缓存；保留应用及其历史关联记录。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        appService.disable(id);
        return Result.ok();
    }
}
