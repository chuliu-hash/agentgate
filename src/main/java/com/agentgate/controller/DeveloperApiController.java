package com.agentgate.controller;

import com.agentgate.dto.Result;
import com.agentgate.entity.ApiDefinition;
import com.agentgate.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/developer/apis")
@RequiredArgsConstructor
/**
 * 教学说明：开发者 API 列表入口，仅返回已上架 API；调用 ApiService.listPublished。
 * 文件路径：src/main/java/com/agentgate/controller/DeveloperApiController.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class DeveloperApiController {

    private final ApiService apiService;

    /**
     * HTTP 入口：仅查询状态为 PUBLISHED 的 API。 具体数据操作委托给 Service，登录与角色校验由请求拦截器执行。
     */
    @GetMapping
    public Result<List<ApiDefinition>> listPublished() {
        return Result.ok(apiService.listPublished());
    }
}
