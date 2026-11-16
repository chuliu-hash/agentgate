package com.agentgate.service;

import com.agentgate.dto.DashboardSummaryDTO;

public interface DashboardService {

    DashboardSummaryDTO summary(Long appId);
}
