package com.agentgate.controller;

import com.agentgate.dto.PageResult;
import com.agentgate.dto.Result;
import com.agentgate.entity.AuditLog;
import com.agentgate.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
/**
 * 教学说明：分页查看管理操作审计，调用 AuditLogService；不同于开放接口调用日志。
 * 文件路径：src/main/java/com/agentgate/controller/AdminAuditLogController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    /**
     * HTTP 入口：按筛选条件与页码查询记录；调用方传入的应用范围用于限制查询，页大小在业务层约束。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<PageResult<AuditLog>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(auditLogService.page(page, size));
    }
}
