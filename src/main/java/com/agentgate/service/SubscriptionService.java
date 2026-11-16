package com.agentgate.service;

import com.agentgate.dto.SubscribeDTO;
import com.agentgate.entity.Subscription;

import java.util.List;

public interface SubscriptionService {

    Subscription apply(SubscribeDTO dto);

    List<Subscription> listMine(Long appId);

    List<Subscription> listPending();

    List<Subscription> listAll();

    void approve(Long id, boolean approved, String reason);

    Subscription getActive(Long appId, Long apiId);
}
