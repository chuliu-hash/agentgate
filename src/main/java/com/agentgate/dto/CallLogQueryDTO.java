package com.agentgate.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
/**
 * 教学说明：分页日志查询条件：应用/API/traceId、起止时间与页码大小；时间格式通过 DateTimeFormat 解析，实际页大小在 Service 限制。
 * 文件路径：src/main/java/com/agentgate/dto/CallLogQueryDTO.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class CallLogQueryDTO {

    private Long appId;
    private Long apiId;
    private String traceId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private long page = 1;
    private long size = 10;
}
