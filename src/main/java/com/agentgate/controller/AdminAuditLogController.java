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
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public Result<PageResult<AuditLog>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(auditLogService.page(page, size));
    }
}
