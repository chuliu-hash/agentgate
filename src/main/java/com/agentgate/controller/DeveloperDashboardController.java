package com.agentgate.controller;

import com.agentgate.dto.DashboardSummaryDTO;
import com.agentgate.dto.Result;
import com.agentgate.service.AppService;
import com.agentgate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/developer/dashboard")
@RequiredArgsConstructor
public class DeveloperDashboardController {

    private final DashboardService dashboardService;
    private final AppService appService;

    @GetMapping
    public Result<DashboardSummaryDTO> summary(@RequestParam Long appId) {
        appService.getOwnedApp(appId);
        return Result.ok(dashboardService.summary(appId));
    }
}
