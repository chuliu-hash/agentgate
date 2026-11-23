package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
/**
 * 教学说明：套餐名称、QPS、日/月配额和说明，NotNull/Min 保证计数上限为正，名称长度受约束。
 * 文件路径：src/main/java/com/agentgate/dto/QuotaPlanDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
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
