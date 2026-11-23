package com.agentgate.controller;

import com.agentgate.annotation.AuditLog;
import com.agentgate.dto.QuotaPlanDTO;
import com.agentgate.dto.Result;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.service.QuotaPlanService;
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
@RequestMapping("/admin/quota-plans")
@RequiredArgsConstructor
/**
 * 教学说明：管理员套餐管理入口：列表、创建、更新；Valid 校验 DTO，AuditLog 记录写操作。
 * 文件路径：src/main/java/com/agentgate/controller/AdminQuotaPlanController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AdminQuotaPlanController {

    private final QuotaPlanService quotaPlanService;

    /**
     * HTTP 入口：接收请求并返回业务结果。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<List<QuotaPlan>> list() {
        return Result.ok(quotaPlanService.listAll());
    }

    /**
     * HTTP 入口：使用输入 DTO 创建业务记录；具体唯一性、安全校验与默认状态见方法中的业务分支。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping
    @AuditLog(action = "CREATE_QUOTA_PLAN", resourceType = "QUOTA_PLAN")
    public Result<QuotaPlan> create(@Valid @RequestBody QuotaPlanDTO dto) {
        return Result.ok(quotaPlanService.create(dto));
    }

    /**
     * HTTP 入口：查询已有记录并复制允许修改的字段，写回数据库；涉及缓存的数据随后失效。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_QUOTA_PLAN", resourceType = "QUOTA_PLAN")
    public Result<QuotaPlan> update(@PathVariable Long id,
                                    @Valid @RequestBody QuotaPlanDTO dto) {
        return Result.ok(quotaPlanService.update(id, dto));
    }
}
