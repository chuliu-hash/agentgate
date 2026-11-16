package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class SubscribeDTO {

    @NotNull(message = "应用 ID 不能为空")
    private Long appId;

    @NotNull(message = "API ID 不能为空")
    private Long apiId;

    @NotNull(message = "配额套餐 ID 不能为空")
    private Long quotaPlanId;
}
