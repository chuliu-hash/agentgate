package com.agentgate.service;

import com.agentgate.dto.PageResult;
import com.agentgate.entity.AuditLog;

public interface AuditLogService {

    void save(AuditLog auditLog);

    PageResult<AuditLog> page(long page, long size);
}
