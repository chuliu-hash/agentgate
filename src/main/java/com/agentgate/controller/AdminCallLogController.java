package com.agentgate.controller;

import com.agentgate.dto.CallLogQueryDTO;
import com.agentgate.dto.PageResult;
import com.agentgate.dto.Result;
import com.agentgate.entity.CallLog;
import com.agentgate.service.CallLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/call-logs")
@RequiredArgsConstructor
/**
 * 教学说明：管理员全局调用日志入口：分页和 traceId 查询时传 null 范围，表示不限制单个应用；依赖拦截器保证 ADMIN。
 * 文件路径：src/main/java/com/agentgate/controller/AdminCallLogController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AdminCallLogController {

    private final CallLogService callLogService;

    /**
     * HTTP 入口：按筛选条件与页码查询记录；调用方传入的应用范围用于限制查询，页大小在业务层约束。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<PageResult<CallLog>> page(CallLogQueryDTO query) {
        return Result.ok(callLogService.page(query, null));
    }

    /**
     * HTTP 入口：接收请求并返回业务结果。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping("/{traceId}")
    public Result<CallLog> get(@PathVariable String traceId) {
        return Result.ok(callLogService.getByTraceId(traceId, null));
    }
}
