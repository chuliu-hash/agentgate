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
/**
 * 教学说明：保存管理操作审计并提供有上限的分页查询；与调用日志不同，没有 MQ 异步消费链路。
 * 文件路径：src/main/java/com/agentgate/service/impl/AuditLogServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 持久化管理操作审计记录；审计写入的异常处理由调用切面决定。
     */
    @Override
    public void save(AuditLog auditLog) {
        // 执行插入并由数据库约束最终校验唯一性；实体主键按表配置回填。
        auditLogMapper.insert(auditLog);
    }

    /**
     * 按筛选条件与页码查询记录；调用方传入的应用范围用于限制查询，页大小在业务层约束。
     */
    @Override
    public PageResult<AuditLog> page(long page, long size) {
        long current = Math.max(1L, page);
        long pageSize = Math.min(Math.max(1L, size), 100L);
        // 把筛选条件与分页参数交给 MyBatis-Plus，避免将全部记录加载后再分页。
        Page<AuditLog> result = auditLogMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getCreateTime));
        return PageResult.of(result);
    }
}
