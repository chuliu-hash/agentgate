package com.agentgate.service;

import com.agentgate.dto.ApiCreateDTO;
import com.agentgate.entity.ApiDefinition;

import java.util.List;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/ApiService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface ApiService {

    /**
     * 使用输入 DTO 创建业务记录；具体唯一性、安全校验与默认状态见方法中的业务分支。
     */
    ApiDefinition create(ApiCreateDTO dto);

    /**
     * 查询已有记录并复制允许修改的字段，写回数据库；涉及缓存的数据随后失效。
     */
    ApiDefinition update(Long id, ApiCreateDTO dto);

    /**
     * 请求将 API 转为 PUBLISHED，实际转换规则与并发检查由状态更新方法执行。
     */
    void publish(Long id);

    /**
     * 请求将 API 转为 OFFLINE，保留定义和历史调用记录。
     */
    void offline(Long id);

    /**
     * 查询业务记录列表；管理员权限由调用入口的登录拦截器控制。
     */
    List<ApiDefinition> listAll();

    /**
     * 仅查询状态为 PUBLISHED 的 API。
     */
    List<ApiDefinition> listPublished();

    /**
     * 按主键读取业务记录；不存在时抛出对应业务异常，不隐式执行所有权校验。
     */
    ApiDefinition getById(Long id);

    /**
     * 按业务编码读取 API，通过缓存减少数据库访问；不存在时可能返回空值。
     */
    ApiDefinition getByCode(String code);

    /**
     * 读取 API 并要求状态为 PUBLISHED；不存在或未上架时拒绝开放调用。
     */
    ApiDefinition getPublishedByCode(String code);
}
