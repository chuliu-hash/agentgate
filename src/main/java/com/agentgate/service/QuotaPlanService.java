package com.agentgate.service;

import com.agentgate.dto.QuotaPlanDTO;
import com.agentgate.entity.QuotaPlan;

import java.util.List;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/QuotaPlanService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface QuotaPlanService {

    /**
     * 使用输入 DTO 创建业务记录；具体唯一性、安全校验与默认状态见方法中的业务分支。
     */
    QuotaPlan create(QuotaPlanDTO dto);

    /**
     * 查询已有记录并复制允许修改的字段，写回数据库；涉及缓存的数据随后失效。
     */
    QuotaPlan update(Long id, QuotaPlanDTO dto);

    /**
     * 查询业务记录列表；管理员权限由调用入口的登录拦截器控制。
     */
    List<QuotaPlan> listAll();

    /**
     * 按主键读取业务记录；不存在时抛出对应业务异常，不隐式执行所有权校验。
     */
    QuotaPlan getById(Long id);
}
