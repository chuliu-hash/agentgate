package com.agentgate.dto;

import com.agentgate.enums.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
/**
 * 教学说明：管理接口统一包装 success/code/message/data/timestamp；静态 ok/fail 创建结果，业务 code 不总是 HTTP 状态。
 * 文件路径：src/main/java/com/agentgate/dto/Result.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private int code;
    private String message;
    private T data;
    private long timestamp = System.currentTimeMillis();

    /**
     * 创建成功响应包装，业务 code 为 0；可携带数据或返回无数据成功。
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 创建成功响应包装，业务 code 为 0；可携带数据或返回无数据成功。
     */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMessage(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 创建失败响应包装；JSON 业务 code 与 HTTP 状态需分别设置。
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建失败响应包装；JSON 业务 code 与 HTTP 状态需分别设置。
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
