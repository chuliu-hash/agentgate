package com.agentgate.controller;

import com.agentgate.dto.CallLogQueryDTO;
import com.agentgate.dto.PageResult;
import com.agentgate.dto.Result;
import com.agentgate.entity.CallLog;
import com.agentgate.service.AppService;
import com.agentgate.service.CallLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开发者只能查询自己应用产生的调用日志。
 */
@RestController
@RequestMapping("/developer/call-logs")
@RequiredArgsConstructor
/**
 * 教学说明：查询前通过 getOwnedApp 校验 appId 属于当前用户，再把 appId 作为日志查询范围，避免越权访问。
 * 文件路径：src/main/java/com/agentgate/controller/DeveloperCallLogController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class DeveloperCallLogController {

    private final CallLogService callLogService;
    private final AppService appService;

    /**
     * HTTP 入口：按筛选条件与页码查询记录；调用方传入的应用范围用于限制查询，页大小在业务层约束。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<PageResult<CallLog>> page(CallLogQueryDTO query,
                                            @RequestParam Long appId) {
        // 先核实应用存在且归当前登录用户所有，不能仅凭客户端传来的应用主键授权。
        appService.getOwnedApp(appId);
        return Result.ok(callLogService.page(query, appId));
    }

    /**
     * HTTP 入口：接收请求并返回业务结果。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping("/{traceId}")
    public Result<CallLog> get(@PathVariable String traceId, @RequestParam Long appId) {
        // 先核实应用存在且归当前登录用户所有，不能仅凭客户端传来的应用主键授权。
        appService.getOwnedApp(appId);
        return Result.ok(callLogService.getByTraceId(traceId, appId));
    }
}
