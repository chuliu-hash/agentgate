package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class QuotaPlanDTO {

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 64, message = "套餐名称最长 64 个字符")
    private String name;

    @NotNull(message = "QPS 上限不能为空")
    @Min(value = 1, message = "QPS 上限至少为 1")
    private Integer qpsLimit;

    @NotNull(message = "日配额不能为空")
    @Min(value = 1, message = "日配额至少为 1")
    private Integer dailyQuota;

    @NotNull(message = "月配额不能为空")
    @Min(value = 1, message = "月配额至少为 1")
    private Integer monthlyQuota;

    @Size(max = 256, message = "描述最长 256 个字符")
    private String description;
}
