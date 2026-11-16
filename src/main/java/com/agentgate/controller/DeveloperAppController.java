package com.agentgate.controller;

import com.agentgate.dto.AppCreateDTO;
import com.agentgate.dto.AppSecretDTO;
import com.agentgate.dto.IpWhitelistDTO;
import com.agentgate.dto.Result;
import com.agentgate.entity.App;
import com.agentgate.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/developer/apps")
@RequiredArgsConstructor
public class DeveloperAppController {

    private final AppService appService;

    @PostMapping
    public Result<AppSecretDTO> create(@Valid @RequestBody AppCreateDTO dto) {
        return Result.ok(appService.create(dto));
    }

    @GetMapping
    public Result<List<App>> list() {
        return Result.ok(appService.listMine());
    }

    @PostMapping("/{id}/reset-secret")
    public Result<AppSecretDTO> resetSecret(@PathVariable Long id) {
        return Result.ok(appService.resetSecret(id));
    }

    @PutMapping("/{id}/ip-whitelist")
    public Result<Void> updateIpWhitelist(@PathVariable Long id,
                                          @Valid @RequestBody IpWhitelistDTO dto) {
        appService.updateIpWhitelist(id, dto.getIpWhitelist());
        return Result.ok();
    }

    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        appService.disable(id);
        return Result.ok();
    }
}
