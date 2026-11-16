package com.agentgate.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardSummaryDTO {

    private long todayCalls;
    private long successCalls;
    private double successRate;
    private double avgLatencyMs;
    private long activeApps;
    private List<Map<String, Object>> topApis;
}
