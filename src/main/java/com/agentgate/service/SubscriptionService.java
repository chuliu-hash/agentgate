package com.agentgate.service;

import com.agentgate.dto.SubscribeDTO;
import com.agentgate.entity.Subscription;

import java.util.List;
/**
 * 教学说明：业务服务契约，声明 Controller 或其他 Service 可调用的方法及入参与返回类型。运行时由 Spring 注入同名 Impl；逐方法的状态校验、SQL 和缓存行为见本章对应实现类。
 * 文件路径：src/main/java/com/agentgate/service/SubscriptionService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface SubscriptionService {

    /**
     * 申请订阅：核验应用所有者、API 已上架及套餐存在；首次创建 PENDING，拒绝后复用原记录重新待审。联合唯一索引与旧状态条件更新处理并发，不能直接获得调用权限。
     */
    Subscription apply(SubscribeDTO dto);

    /**
     * 按当前登录用户或已验证归属的应用查询记录，防止客户端任意选择其他所有者。
     */
    List<Subscription> listMine(Long appId);

    /**
     * 查询 PENDING 订阅并按创建时间排列，供管理员处理待办。
     */
    List<Subscription> listPending();

    /**
     * 查询业务记录列表；管理员权限由调用入口的登录拦截器控制。
     */
    List<Subscription> listAll();

    /**
     * 审批待审订阅，按结果转为 ACTIVE 或 REJECTED；更新条件包含旧状态 PENDING，未命中时拒绝覆盖并发修改。
     */
    void approve(Long id, boolean approved, String reason);

    /**
     * 按应用/API 读取 ACTIVE 订阅并缓存；不存在时返回空值，不代表调用方已拥有应用管理权限。
     */
    Subscription getActive(Long appId, Long apiId);
}
