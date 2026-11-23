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
/**
 * 教学说明：负责套餐创建、修改、列表及按 ID 查询，DTO 到实体赋值；缺少套餐抛业务异常，配额参数由 DTO 约束。
 * 文件路径：src/main/java/com/agentgate/service/impl/QuotaPlanServiceImpl.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class QuotaPlanServiceImpl implements QuotaPlanService {

    private final QuotaPlanMapper quotaPlanMapper;

    /**
     * 使用输入 DTO 创建业务记录；具体唯一性、安全校验与默认状态见方法中的业务分支。
     */
    @Override
    public QuotaPlan create(QuotaPlanDTO dto) {
        QuotaPlan plan = new QuotaPlan();
        copy(dto, plan);
        // 执行插入并由数据库约束最终校验唯一性；实体主键按表配置回填。
        quotaPlanMapper.insert(plan);
        return plan;
    }

    /**
     * 查询已有记录并复制允许修改的字段，写回数据库；涉及缓存的数据随后失效。
     */
    @Override
    public QuotaPlan update(Long id, QuotaPlanDTO dto) {
        QuotaPlan plan = getById(id);
        copy(dto, plan);
        quotaPlanMapper.updateById(plan);
        return plan;
    }

    /**
     * 查询业务记录列表；管理员权限由调用入口的登录拦截器控制。
     */
    @Override
    public List<QuotaPlan> listAll() {
        // 按下方查询条件及排序读取列表，范围限制决定调用者可以看到哪些记录。
        return quotaPlanMapper.selectList(
                new LambdaQueryWrapper<QuotaPlan>().orderByAsc(QuotaPlan::getId));
    }

    /**
     * 按主键读取业务记录；不存在时抛出对应业务异常，不隐式执行所有权校验。
     */
    @Override
    public QuotaPlan getById(Long id) {
        // 按数据库主键查询；查到记录后仍需根据当前操作检查存在性或业务权限。
        QuotaPlan plan = quotaPlanMapper.selectById(id);
        // 校验条件成立时拒绝本次操作，抛出 PLAN_NOT_FOUND；不再继续后续正常业务步骤。
        if (plan == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
        }
        return plan;
    }

    /**
     * 将 DTO 中允许修改的业务字段复制到实体；不复制数据库 ID 或自动生成的状态。
     */
    private void copy(QuotaPlanDTO dto, QuotaPlan plan) {
        plan.setName(dto.getName());
        plan.setQpsLimit(dto.getQpsLimit());
        plan.setDailyQuota(dto.getDailyQuota());
        plan.setMonthlyQuota(dto.getMonthlyQuota());
        plan.setDescription(dto.getDescription());
    }
}
