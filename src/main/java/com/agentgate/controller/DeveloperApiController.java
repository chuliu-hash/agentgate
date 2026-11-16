package com.agentgate.controller;

import com.agentgate.dto.Result;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/developer/apis")
@RequiredArgsConstructor
public class DeveloperApiController {

    private final ApiService apiService;

    @GetMapping
    public Result<List<ApiDefinition>> listPublished() {
        return Result.ok(apiService.listPublished());
    }
}
