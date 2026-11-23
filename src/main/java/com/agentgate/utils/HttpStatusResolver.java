package com.agentgate.utils;

import org.springframework.http.HttpStatus;

/** 将业务错误码统一映射为 HTTP 状态码，避免异常处理入口维护不同副本。 */
public final class HttpStatusResolver {

    private HttpStatusResolver() {
    }

    /** 业务错误码与 HTTP 状态码不要求数值相同，未知错误默认按参数错误处理。 */
    public static HttpStatus resolve(int code) {
        switch (code) {
            case 401: case 1001: case 1002: case 1003: case 1004:
                return HttpStatus.UNAUTHORIZED;
            case 403: case 1005: case 1006: case 1007: case 1008: case 1009:
                return HttpStatus.FORBIDDEN;
            case 404: return HttpStatus.NOT_FOUND;
            case 405: return HttpStatus.METHOD_NOT_ALLOWED;
            case 413: return HttpStatus.PAYLOAD_TOO_LARGE;
            case 429: case 4290: return HttpStatus.TOO_MANY_REQUESTS;
            case 2001: return HttpStatus.GATEWAY_TIMEOUT;
            case 2002: return HttpStatus.BAD_GATEWAY;
            case 500: return HttpStatus.INTERNAL_SERVER_ERROR;
            default: return HttpStatus.BAD_REQUEST;
        }
    }
}
