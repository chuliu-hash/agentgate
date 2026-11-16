package com.agentgate.config;

import com.agentgate.dto.Result;
import com.agentgate.enums.ErrorCode;
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
public class WebExceptionAdvice {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        HttpStatus status = resolveStatus(e.getCode());
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SYSTEM_ERROR));
    }

    private HttpStatus resolveStatus(int code) {
        switch (code) {
            case 401:
                return HttpStatus.UNAUTHORIZED;
            case 403:
                return HttpStatus.FORBIDDEN;
            case 404:
                return HttpStatus.NOT_FOUND;
            case 413:
                return HttpStatus.PAYLOAD_TOO_LARGE;
            case 429:
            case 4290:
                return HttpStatus.TOO_MANY_REQUESTS;
            case 405:
                return HttpStatus.METHOD_NOT_ALLOWED;
            case 2001:
                return HttpStatus.GATEWAY_TIMEOUT;
            case 2002:
                return HttpStatus.BAD_GATEWAY;
            case 500:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case 1001:
            case 1002:
            case 1003:
            case 1004:
                return HttpStatus.UNAUTHORIZED;
            case 1005:
            case 1006:
            case 1007:
            case 1008:
            case 1009:
                return HttpStatus.FORBIDDEN;
            default:
                return HttpStatus.BAD_REQUEST;
        }
    }
}
