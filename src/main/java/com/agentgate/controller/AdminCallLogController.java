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
public class AdminCallLogController {

    private final CallLogService callLogService;

    @GetMapping
    public Result<PageResult<CallLog>> page(CallLogQueryDTO query) {
        return Result.ok(callLogService.page(query, null));
    }

    @GetMapping("/{traceId}")
    public Result<CallLog> get(@PathVariable String traceId) {
        return Result.ok(callLogService.getByTraceId(traceId, null));
    }
}
