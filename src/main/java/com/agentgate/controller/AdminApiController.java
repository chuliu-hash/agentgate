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
public class AdminApiController {

    private final ApiService apiService;

    @GetMapping
    public Result<List<ApiDefinition>> list() {
        return Result.ok(apiService.listAll());
    }

    @PostMapping
    @AuditLog(action = "CREATE_API", resourceType = "API")
    public Result<ApiDefinition> create(@Valid @RequestBody ApiCreateDTO dto) {
        return Result.ok(apiService.create(dto));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_API", resourceType = "API")
    public Result<ApiDefinition> update(@PathVariable Long id,
                                        @Valid @RequestBody ApiCreateDTO dto) {
        return Result.ok(apiService.update(id, dto));
    }

    @PostMapping("/{id}/publish")
    @AuditLog(action = "PUBLISH_API", resourceType = "API")
    public Result<Void> publish(@PathVariable Long id) {
        apiService.publish(id);
        return Result.ok();
    }

    @PostMapping("/{id}/offline")
    @AuditLog(action = "OFFLINE_API", resourceType = "API")
    public Result<Void> offline(@PathVariable Long id) {
        apiService.offline(id);
        return Result.ok();
    }
}
