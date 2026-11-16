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
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public Result<List<Subscription>> list(
            @RequestParam(required = false) String status) {
        if (SubscriptionStatus.PENDING.name().equals(status)) {
            return Result.ok(subscriptionService.listPending());
        }
        return Result.ok(subscriptionService.listAll());
    }

    @PostMapping("/{id}/approve")
    @AuditLog(action = "APPROVE_SUBSCRIPTION", resourceType = "SUBSCRIPTION")
    public Result<Void> approve(@PathVariable Long id,
                                @Valid @RequestBody ApproveDTO dto) {
        subscriptionService.approve(id, dto.getApproved(), dto.getReason());
        return Result.ok();
    }
}
