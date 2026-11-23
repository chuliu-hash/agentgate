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
/**
 * 教学说明：先验证应用归属，再获取该应用看板；不能信任客户端随意传来的 appId。
 * 文件路径：src/main/java/com/agentgate/controller/DeveloperDashboardController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class DeveloperDashboardController {

    private final DashboardService dashboardService;
    private final AppService appService;

    /**
     * HTTP 入口：聚合当日调用日志生成看板，appId 为空表示全局；成功按 2xx 统计，异步日志会导致短时滞后。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<DashboardSummaryDTO> summary(@RequestParam Long appId) {
        // 先核实应用存在且归当前登录用户所有，不能仅凭客户端传来的应用主键授权。
        appService.getOwnedApp(appId);
        return Result.ok(dashboardService.summary(appId));
    }
}
