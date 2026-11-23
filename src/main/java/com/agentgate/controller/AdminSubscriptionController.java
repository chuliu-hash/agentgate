package com.agentgate.controller;

import com.agentgate.annotation.AuditLog;
import com.agentgate.dto.ApproveDTO;
import com.agentgate.dto.Result;
import com.agentgate.entity.Subscription;
import com.agentgate.enums.SubscriptionStatus;
import com.agentgate.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
/**
 * 教学说明：管理员订阅列表及审批入口；传 PENDING 时查待审批，其他值走全部列表；ApproveDTO 表达同意与拒绝原因。
 * 文件路径：src/main/java/com/agentgate/controller/AdminSubscriptionController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * HTTP 入口：接收请求并返回业务结果。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<List<Subscription>> list(
            @RequestParam(required = false) String status) {
        if (SubscriptionStatus.PENDING.name().equals(status)) {
            return Result.ok(subscriptionService.listPending());
        }
        return Result.ok(subscriptionService.listAll());
    }

    /**
     * HTTP 入口：审批待审订阅，按结果转为 ACTIVE 或 REJECTED；更新条件包含旧状态 PENDING，未命中时拒绝覆盖并发修改。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @PostMapping("/{id}/approve")
    @AuditLog(action = "APPROVE_SUBSCRIPTION", resourceType = "SUBSCRIPTION")
    public Result<Void> approve(@PathVariable Long id,
                                @Valid @RequestBody ApproveDTO dto) {
        subscriptionService.approve(id, dto.getApproved(), dto.getReason());
        return Result.ok();
    }
}
