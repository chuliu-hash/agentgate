package com.agentgate.controller;

import com.agentgate.dto.DashboardSummaryDTO;
import com.agentgate.dto.Result;
import com.agentgate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardSummaryDTO> summary() {
        return Result.ok(dashboardService.summary(null));
    }
}
