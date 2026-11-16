package com.agentgate.service;

import com.agentgate.entity.ApiDefinition;
import com.agentgate.entity.QuotaPlan;

public interface RateLimitService {

    /**
     * QPS 与日/月配额都通过时返回 true，否则抛出对应业务异常。
     */
    void checkAndConsume(Long appId, ApiDefinition api, QuotaPlan plan);
}
