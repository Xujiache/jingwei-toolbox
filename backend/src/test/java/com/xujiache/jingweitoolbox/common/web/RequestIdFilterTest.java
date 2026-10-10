package com.xujiache.jingweitoolbox.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xujiache.jingweitoolbox.common.api.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class RequestIdFilterTest {

    @RestController
    static class ProbeController {

        @GetMapping("/probe/echo")
        public ApiResponse<String> echo() {
            return ApiResponse.success("ok");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void 合法的入站请求ID被透传() throws Exception {
        MvcResult result = mockMvc.perform(get("/probe/echo").header(RequestIdFilter.HEADER, "trace-abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("trace-abc-123"))
                .andReturn();

        assertThat(result.getResponse().getHeader(RequestIdFilter.HEADER)).isEqualTo("trace-abc-123");
    }

    @Test
    void 入站缺失时生成UUID() throws Exception {
        MvcResult result = mockMvc.perform(get("/probe/echo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andReturn();

        String requestId = result.getResponse().getHeader(RequestIdFilter.HEADER);
        assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void 超长入站请求ID被替换为新UUID() throws Exception {
        String tooLong = "x".repeat(100);

        MvcResult result = mockMvc.perform(get("/probe/echo").header(RequestIdFilter.HEADER, tooLong))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = result.getResponse().getHeader(RequestIdFilter.HEADER);
        assertThat(requestId).isNotEqualTo(tooLong);
        assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
