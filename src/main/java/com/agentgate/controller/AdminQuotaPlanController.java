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
public class AdminQuotaPlanController {

    private final QuotaPlanService quotaPlanService;

    @GetMapping
    public Result<List<QuotaPlan>> list() {
        return Result.ok(quotaPlanService.listAll());
    }

    @PostMapping
    @AuditLog(action = "CREATE_QUOTA_PLAN", resourceType = "QUOTA_PLAN")
    public Result<QuotaPlan> create(@Valid @RequestBody QuotaPlanDTO dto) {
        return Result.ok(quotaPlanService.create(dto));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_QUOTA_PLAN", resourceType = "QUOTA_PLAN")
    public Result<QuotaPlan> update(@PathVariable Long id,
                                    @Valid @RequestBody QuotaPlanDTO dto) {
        return Result.ok(quotaPlanService.update(id, dto));
    }
}
