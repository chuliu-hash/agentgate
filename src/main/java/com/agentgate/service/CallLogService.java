package com.agentgate.service;

import com.agentgate.dto.CallLogMessage;
import com.agentgate.dto.CallLogQueryDTO;
import com.agentgate.dto.PageResult;
import com.agentgate.entity.CallLog;

public interface CallLogService {

    void asyncSave(CallLogMessage message);

    void saveDirect(CallLogMessage message);

    PageResult<CallLog> page(CallLogQueryDTO query, Long restrictAppId);

    CallLog getByTraceId(String traceId, Long restrictAppId);
}
