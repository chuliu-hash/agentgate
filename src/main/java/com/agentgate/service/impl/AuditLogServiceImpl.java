package com.agentgate.service.impl;

import com.agentgate.dto.PageResult;
import com.agentgate.entity.AuditLog;
import com.agentgate.mapper.AuditLogMapper;
import com.agentgate.service.AuditLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void save(AuditLog auditLog) {
        auditLogMapper.insert(auditLog);
    }

    @Override
    public PageResult<AuditLog> page(long page, long size) {
        long current = Math.max(1L, page);
        long pageSize = Math.min(Math.max(1L, size), 100L);
        Page<AuditLog> result = auditLogMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getCreateTime));
        return PageResult.of(result);
    }
}
