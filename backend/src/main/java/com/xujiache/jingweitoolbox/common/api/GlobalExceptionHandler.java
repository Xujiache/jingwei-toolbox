package com.xujiache.jingweitoolbox.common.api;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：所有错误以契约错误封套 {code, message, details?, requestId} 返回。
 * 500 的完整堆栈只进日志，不对外泄露。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return respond(ex.errorCode(), ex.getMessage(), ex.details());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("field", error.getField());
                    item.put("message", error.getDefaultMessage());
                    return item;
                })
                .toList();
        return respond(ErrorCode.BAD_REQUEST, "请求参数校验失败", Map.of("fields", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return respond(ErrorCode.BAD_REQUEST, "请求体缺失或 JSON 格式非法");
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ErrorResponse> handleBinding(ServletRequestBindingException ex) {
        return respond(ErrorCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        List<Map<String, Object>> violations = ex.getConstraintViolations().stream()
                .map(violation -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("field", violation.getPropertyPath().toString());
                    item.put("message", violation.getMessage());
                    return item;
                })
                .toList();
        return respond(ErrorCode.BAD_REQUEST, "请求参数校验失败", Map.of("fields", violations));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return respond(ErrorCode.NOT_FOUND, "请求路径不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, "不支持的请求方法");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("未处理异常", ex);
        return respond(ErrorCode.INTERNAL_ERROR, "服务器内部错误");
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, String message, Map<String, Object> details) {
        HttpStatus status = errorCode.httpStatus();
        ErrorResponse body = ErrorResponse.of(errorCode, message, details);
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, String message) {
        return respond(errorCode, message, null);
    }
}
