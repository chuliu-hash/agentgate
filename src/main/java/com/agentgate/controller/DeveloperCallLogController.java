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
public class DeveloperCallLogController {

    private final CallLogService callLogService;
    private final AppService appService;

    @GetMapping
    public Result<PageResult<CallLog>> page(CallLogQueryDTO query,
                                            @RequestParam Long appId) {
        appService.getOwnedApp(appId);
        return Result.ok(callLogService.page(query, appId));
    }

    @GetMapping("/{traceId}")
    public Result<CallLog> get(@PathVariable String traceId, @RequestParam Long appId) {
        appService.getOwnedApp(appId);
        return Result.ok(callLogService.getByTraceId(traceId, appId));
    }
}
