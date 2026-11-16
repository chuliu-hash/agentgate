package com.agentgate.service.impl;

import com.agentgate.dto.QuotaPlanDTO;
import com.agentgate.entity.QuotaPlan;
import com.agentgate.enums.ErrorCode;
import com.agentgate.exception.BizException;
import com.agentgate.mapper.QuotaPlanMapper;
import com.agentgate.service.QuotaPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuotaPlanServiceImpl implements QuotaPlanService {

    private final QuotaPlanMapper quotaPlanMapper;

    @Override
    public QuotaPlan create(QuotaPlanDTO dto) {
        QuotaPlan plan = new QuotaPlan();
        copy(dto, plan);
        quotaPlanMapper.insert(plan);
        return plan;
    }

    @Override
    public QuotaPlan update(Long id, QuotaPlanDTO dto) {
        QuotaPlan plan = getById(id);
        copy(dto, plan);
        quotaPlanMapper.updateById(plan);
        return plan;
    }

    @Override
    public List<QuotaPlan> listAll() {
        return quotaPlanMapper.selectList(
                new LambdaQueryWrapper<QuotaPlan>().orderByAsc(QuotaPlan::getId));
    }

    @Override
    public QuotaPlan getById(Long id) {
        QuotaPlan plan = quotaPlanMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
        }
        return plan;
    }

    private void copy(QuotaPlanDTO dto, QuotaPlan plan) {
        plan.setName(dto.getName());
        plan.setQpsLimit(dto.getQpsLimit());
        plan.setDailyQuota(dto.getDailyQuota());
        plan.setMonthlyQuota(dto.getMonthlyQuota());
        plan.setDescription(dto.getDescription());
    }
}
