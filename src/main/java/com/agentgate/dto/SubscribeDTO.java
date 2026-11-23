package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
/**
 * 教学说明：订阅申请输入：appId、apiId、quotaPlanId，均非空；归属、存在性和发布状态必须额外查询判断。
 * 文件路径：src/main/java/com/agentgate/dto/SubscribeDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class SubscribeDTO {

    @NotNull(message = "应用 ID 不能为空")
    private Long appId;

    @NotNull(message = "API ID 不能为空")
    private Long apiId;

    @NotNull(message = "配额套餐 ID 不能为空")
    private Long quotaPlanId;
}
