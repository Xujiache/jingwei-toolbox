package com.xujiache.jingweitoolbox.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xujiache.jingweitoolbox.common.web.RequestIdFilter;
import java.util.Map;
import org.slf4j.MDC;

/**
 * 错误响应封套 {code, message, details?, requestId}，details 仅在存在时输出。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Map<String, Object> details, String requestId) {

    public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, Object> details) {
        return new ErrorResponse(errorCode.name(), message, details, MDC.get(RequestIdFilter.MDC_KEY));
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return of(errorCode, message, null);
    }
}
