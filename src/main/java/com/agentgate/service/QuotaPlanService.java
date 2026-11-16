package com.agentgate.service;

import com.agentgate.dto.QuotaPlanDTO;
import com.agentgate.entity.QuotaPlan;

import java.util.List;

public interface QuotaPlanService {

    QuotaPlan create(QuotaPlanDTO dto);

    QuotaPlan update(Long id, QuotaPlanDTO dto);

    List<QuotaPlan> listAll();

    QuotaPlan getById(Long id);
}
