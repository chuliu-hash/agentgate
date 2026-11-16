package com.agentgate.service;

import com.agentgate.dto.ApiCreateDTO;
import com.agentgate.entity.ApiDefinition;

import java.util.List;

public interface ApiService {

    ApiDefinition create(ApiCreateDTO dto);

    ApiDefinition update(Long id, ApiCreateDTO dto);

    void publish(Long id);

    void offline(Long id);

    List<ApiDefinition> listAll();

    List<ApiDefinition> listPublished();

    ApiDefinition getById(Long id);

    ApiDefinition getByCode(String code);

    ApiDefinition getPublishedByCode(String code);
}
