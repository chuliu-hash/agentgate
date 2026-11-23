package com.agentgate.service.impl;

import com.agentgate.dto.DashboardSummaryDTO;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.mapper.ApiDefinitionMapper;
import com.agentgate.mapper.CallLogMapper;
import com.agentgate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 教学说明：从调用日志 Mapper 聚合当日调用量、2xx 成功量、成功率、平均耗时、活跃应用和 Top5 API；appId 为空表示全局。
 * 文件路径：src/main/java/com/agentgate/service/impl/DashboardServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class DashboardServiceImpl implements DashboardService {

    private final CallLogMapper callLogMapper;
    private final ApiDefinitionMapper apiDefinitionMapper;

    /**
     * 聚合当日调用日志生成看板，appId 为空表示全局；成功按 2xx 统计，异步日志会导致短时滞后。
     */
    @Override
    public DashboardSummaryDTO summary(Long appId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        long todayCalls = callLogMapper.countSince(start, appId);
        // 教学说明：成功按 HTTP 2xx 统计，不是简单的“没有 Java 异常”；MQ 异步写日志使看板可能短暂滞后。
        long successCalls = callLogMapper.countSuccessSince(start, appId);
        Double avgLatency = callLogMapper.averageLatencySince(start, appId);

        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setTodayCalls(todayCalls);
        summary.setSuccessCalls(successCalls);
        summary.setSuccessRate(todayCalls == 0
                ? 0D
                : Math.round(successCalls * 10000D / todayCalls) / 100D);
        summary.setAvgLatencyMs(avgLatency == null ? 0D : Math.round(avgLatency * 100D) / 100D);
        summary.setActiveApps(callLogMapper.countDistinctAppsSince(start, appId));
        summary.setTopApis(enrichTopApis(callLogMapper.topApisSince(start, appId)));
        return summary;
    }

    /**
     * 批量查询热门 API 名称并补充到聚合行，避免每行单独查询数据库。
     */
    private List<Map<String, Object>> enrichTopApis(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> apiIds = rows.stream()
                .map(row -> toLong(row.get("apiId")))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> names = new HashMap<>();
        if (!apiIds.isEmpty()) {
            for (ApiDefinition api : apiDefinitionMapper.selectBatchIds(apiIds)) {
                names.put(api.getId(), api.getName());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Long apiId = toLong(row.get("apiId"));
            Map<String, Object> item = new HashMap<>();
            item.put("apiId", apiId);
            item.put("apiName", apiId == null ? null : names.getOrDefault(apiId, "未知 API"));
            item.put("callCount", toLong(row.get("callCount")));
            result.add(item);
        }
        return result;
    }

    /**
     * 将数据库返回的 Number 安全转为 Long，非数值返回空值。
     */
    private Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}
