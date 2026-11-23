package com.agentgate.exception;

import com.agentgate.enums.ErrorCode;
import lombok.Getter;

@Getter
/**
 * 教学说明：携带业务 code/message 的运行时异常；由 MVC Advice 或签名 Filter 转为客户端错误，不用于正常成功返回。
 * 文件路径：src/main/java/com/agentgate/exception/BizException.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    /**
     * 构造携带业务错误码与消息的异常，由请求层统一转换为错误响应。
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造携带业务错误码与消息的异常，由请求层统一转换为错误响应。
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 构造携带业务错误码与消息的异常，由请求层统一转换为错误响应。
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
