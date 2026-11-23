package com.agentgate.controller;

import com.agentgate.dto.Result;
import com.agentgate.dto.SubscribeDTO;
import com.agentgate.entity.Subscription;
import com.agentgate.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/developer/subscriptions")
@RequiredArgsConstructor
/**
 * 教学说明：接收订阅申请及 appId 列表查询，委托 SubscriptionService 校验应用所有者并处理审批状态。
 * 文件路径：src/main/java/com/agentgate/controller/DeveloperSubscriptionController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class DeveloperSubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * HTTP 入口：申请订阅：核验应用所有者、API 已上架及套餐存在；首次创建 PENDING，拒绝后复用原记录重新待审。联合唯一索引与旧状态条件更新处理并发，不能直接获得调用权限。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping
    public Result<Subscription> apply(@Valid @RequestBody SubscribeDTO dto) {
        return Result.ok(subscriptionService.apply(dto));
    }

    /**
     * HTTP 入口：接收请求并返回业务结果。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<List<Subscription>> list(@RequestParam Long appId) {
        return Result.ok(subscriptionService.listMine(appId));
    }
}
