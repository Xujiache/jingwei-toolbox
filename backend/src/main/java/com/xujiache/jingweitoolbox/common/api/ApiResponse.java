package com.xujiache.jingweitoolbox.common.api;

import com.xujiache.jingweitoolbox.common.web.RequestIdFilter;
import org.slf4j.MDC;

/**
 * 成功响应封套，与 docs/api/openapi.json 中各 *Response 的 {code, message, data, requestId} 一致。
 */
public record ApiResponse<T>(String code, String message, T data, String requestId) {

    public static final String OK = "OK";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(OK, "success", data, MDC.get(RequestIdFilter.MDC_KEY));
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }
}
