package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ApproveDTO {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Size(max = 256, message = "拒绝原因最长 256 个字符")
    private String reason;
}
