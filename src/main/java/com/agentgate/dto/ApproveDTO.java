package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
/**
 * 教学说明：审批输入，approved 决定通过或拒绝，reason 承载拒绝原因；状态流转由业务实现控制。
 * 文件路径：src/main/java/com/agentgate/dto/ApproveDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class ApproveDTO {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Size(max = 256, message = "拒绝原因最长 256 个字符")
    private String reason;
}
