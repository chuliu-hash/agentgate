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
/**
 * 教学说明：管理员全局看板，summary(null) 表示不按应用过滤。
 * 文件路径：src/main/java/com/agentgate/controller/AdminDashboardController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /**
     * HTTP 入口：聚合当日调用日志生成看板，appId 为空表示全局；成功按 2xx 统计，异步日志会导致短时滞后。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<DashboardSummaryDTO> summary() {
        return Result.ok(dashboardService.summary(null));
    }
}
