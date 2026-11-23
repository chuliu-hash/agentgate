package com.agentgate.config;

import com.agentgate.dto.Result;
import com.agentgate.enums.ErrorCode;
import com.agentgate.utils.HttpStatusResolver;
import com.agentgate.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
/**
 * 教学说明：捕获 MVC 业务异常、参数校验异常与未知异常，转换为 HTTP 状态及 Result；不替代 Filter 自己的异常处理。
 * 文件路径：src/main/java/com/agentgate/config/WebExceptionAdvice.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class WebExceptionAdvice {

    /**
     * 把业务异常转换为客户端可读的 Result 与对应 HTTP 状态。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        HttpStatus status = HttpStatusResolver.resolve(e.getCode());
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 提取参数绑定或校验失败信息，返回 HTTP 400。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidException(Exception e) {
        String message = "参数错误";
        if (e instanceof MethodArgumentNotValidException) {
            FieldError fieldError = ((MethodArgumentNotValidException) e).getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        } else if (e instanceof BindException) {
            FieldError fieldError = ((BindException) e).getBindingResult().getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        }
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 记录未处理异常并返回通用 HTTP 500，避免向调用方泄露内部堆栈。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SYSTEM_ERROR));
    }

}
