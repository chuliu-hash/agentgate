package com.agentgate.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
/**
 * 教学说明：看板输出：当日调用数、成功数/成功率、平均耗时、活跃应用和热门 API 列表，计算逻辑在 DashboardServiceImpl。
 * 文件路径：src/main/java/com/agentgate/dto/DashboardSummaryDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class DashboardSummaryDTO {

    private long todayCalls;
    private long successCalls;
    private double successRate;
    private double avgLatencyMs;
    private long activeApps;
    private List<Map<String, Object>> topApis;
}
