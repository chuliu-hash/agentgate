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
public class DashboardServiceImpl implements DashboardService {

    private final CallLogMapper callLogMapper;
    private final ApiDefinitionMapper apiDefinitionMapper;

    @Override
    public DashboardSummaryDTO summary(Long appId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        long todayCalls = callLogMapper.countSince(start, appId);
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

    private Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}
