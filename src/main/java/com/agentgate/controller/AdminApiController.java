package com.agentgate.controller;

import com.agentgate.annotation.AuditLog;
import com.agentgate.dto.ApiCreateDTO;
import com.agentgate.dto.Result;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.service.ApiService;
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
@RequestMapping("/admin/apis")
@RequiredArgsConstructor
/**
 * 教学说明：管理员 API 管理入口：列表、新增、修改、上架、下线；写操作带 AuditLog 注解，由切面记录成功或失败。
 * 文件路径：src/main/java/com/agentgate/controller/AdminApiController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AdminApiController {

    private final ApiService apiService;

    /**
     * HTTP 入口：接收请求并返回业务结果。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<List<ApiDefinition>> list() {
        return Result.ok(apiService.listAll());
    }

    /**
     * HTTP 入口：使用输入 DTO 创建业务记录；具体唯一性、安全校验与默认状态见方法中的业务分支。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping
    @AuditLog(action = "CREATE_API", resourceType = "API")
    public Result<ApiDefinition> create(@Valid @RequestBody ApiCreateDTO dto) {
        return Result.ok(apiService.create(dto));
    }

    /**
     * HTTP 入口：查询已有记录并复制允许修改的字段，写回数据库；涉及缓存的数据随后失效。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_API", resourceType = "API")
    public Result<ApiDefinition> update(@PathVariable Long id,
                                        @Valid @RequestBody ApiCreateDTO dto) {
        return Result.ok(apiService.update(id, dto));
    }

    /**
     * HTTP 入口：请求将 API 转为 PUBLISHED，实际转换规则与并发检查由状态更新方法执行。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping("/{id}/publish")
    @AuditLog(action = "PUBLISH_API", resourceType = "API")
    public Result<Void> publish(@PathVariable Long id) {
        apiService.publish(id);
        return Result.ok();
    }

    /**
     * HTTP 入口：请求将 API 转为 OFFLINE，保留定义和历史调用记录。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping("/{id}/offline")
    @AuditLog(action = "OFFLINE_API", resourceType = "API")
    public Result<Void> offline(@PathVariable Long id) {
        apiService.offline(id);
        return Result.ok();
    }
}
