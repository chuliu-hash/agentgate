package com.agentgate.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ApiCreateDTO {

    @NotBlank(message = "API 编码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,64}$", message = "API 编码只能包含字母、数字、下划线和中划线，长度 2-64")
    private String code;

    @NotBlank(message = "API 名称不能为空")
    @Size(max = 128, message = "API 名称最长 128 个字符")
    private String name;

    @NotBlank(message = "请求方法不能为空")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|ANY)$", message = "请求方法只能是 GET/POST/PUT/DELETE/ANY")
    private String method;

    @NotBlank(message = "上游地址不能为空")
    @Size(max = 512, message = "上游地址最长 512 个字符")
    private String upstreamUrl;

    @Size(max = 32, message = "版本号最长 32 个字符")
    private String version = "v1";

    @NotNull(message = "敏感级别不能为空")
    @Min(value = 1, message = "敏感级别最小为 1")
    @Max(value = 3, message = "敏感级别最大为 3")
    private Integer sensitivityLevel = 1;

    @Min(value = 100, message = "超时时间最小 100ms")
    @Max(value = 60000, message = "超时时间最大 60000ms")
    private Integer timeoutMs = 5000;

    @Size(max = 512, message = "描述最长 512 个字符")
    private String description;
}
