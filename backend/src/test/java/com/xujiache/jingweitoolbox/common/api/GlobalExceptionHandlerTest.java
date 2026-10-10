package com.xujiache.jingweitoolbox.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xujiache.jingweitoolbox.common.web.RequestIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class GlobalExceptionHandlerTest {

    @RestController
    static class ProbeController {

        @GetMapping("/probe/business-error")
        public ApiResponse<Void> businessError() {
            throw new ApiException(ErrorCode.TOOL_QUOTA_EXCEEDED, "游客额度已用尽", Map.of("toolId", "demo-tool"));
        }

        @GetMapping("/probe/unexpected")
        public ApiResponse<Void> unexpected() {
            throw new IllegalStateException("数据库密码是 boom-secret");
        }

        @GetMapping("/probe/missing-param")
        public ApiResponse<Void> missingParam(@RequestParam String q) {
            return ApiResponse.success();
        }

        @PostMapping("/probe/validated")
        public ApiResponse<Void> validated(@Valid @RequestBody SampleBody body) {
            return ApiResponse.success();
        }
    }

    record SampleBody(@NotBlank String name) {
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void 业务异常映射为契约错误码和HTTP状态() throws Exception {
        mockMvc.perform(get("/probe/business-error"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOOL_QUOTA_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("游客额度已用尽"))
                .andExpect(jsonPath("$.details.toolId").value("demo-tool"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void 未处理异常返回500且不泄露内部信息() throws Exception {
        mockMvc.perform(get("/probe/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }

    @Test
    void 参数校验失败返回400和字段明细() throws Exception {
        mockMvc.perform(post("/probe/validated")
                        .contentType("application/json")
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details.fields[0].field").value("name"));
    }

    @Test
    void 非法JSON返回400() throws Exception {
        mockMvc.perform(post("/probe/validated")
                        .contentType("application/json")
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 缺失必需参数返回400() throws Exception {
        mockMvc.perform(get("/probe/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 未匹配路由返回404错误封套() {
        ResponseEntity<ErrorResponse> response =
                new GlobalExceptionHandler().handleNoResource(new NoResourceFoundException(HttpMethod.GET, "/nope", ""));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void 不支持的方法返回405错误封套() {
        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleMethodNotSupported(new HttpRequestMethodNotSupportedException("DELETE"));

        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
    }
}
