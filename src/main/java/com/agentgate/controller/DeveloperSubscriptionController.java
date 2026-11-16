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
public class DeveloperSubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public Result<Subscription> apply(@Valid @RequestBody SubscribeDTO dto) {
        return Result.ok(subscriptionService.apply(dto));
    }

    @GetMapping
    public Result<List<Subscription>> list(@RequestParam Long appId) {
        return Result.ok(subscriptionService.listMine(appId));
    }
}
