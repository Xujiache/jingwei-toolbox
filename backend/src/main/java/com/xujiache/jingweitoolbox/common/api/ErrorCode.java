package com.xujiache.jingweitoolbox.common.api;

import org.springframework.http.HttpStatus;

/**
 * 接口错误码，与 docs/API_CONTRACT.md 的错误码表一一对应。
 * 错误码是对外稳定契约，不允许随意改名或改变语义。
 */
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    TOOL_NOT_FOUND(HttpStatus.NOT_FOUND),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT),
    RESOURCE_VERSION_CONFLICT(HttpStatus.CONFLICT),
    TOOL_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    DEPENDENCY_ERROR(HttpStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),

    // 契约表之外的路由级基础设施错误码，仅用于未匹配路由和未支持方法
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
