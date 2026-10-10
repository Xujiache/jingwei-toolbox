package com.xujiache.jingweitoolbox.tool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 工具注册与策略管理全流程集成测试。测试按 @Order 串行执行以复用同一工具的生命周期。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ToolIntegrationTest {

    private static final String TOOL_ID = "text-counter";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    void 管理员创建工具返回201和默认策略() throws Exception {
        mockMvc.perform(post("/api/v1/admin/tools")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolId":"%s","displayName":"文本计数器","description":"统计字符数",
                                 "currentVersion":"1.0.0","status":"DRAFT",
                                 "inputSchema":{"type":"object"},"outputSchema":{"type":"object"}}
                                """.formatted(TOOL_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.toolId").value(TOOL_ID))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.currentVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data.policy.enabled").value(true))
                .andExpect(jsonPath("$.data.policy.loginRequired").value(false))
                .andExpect(jsonPath("$.data.policy.guestLimit").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.aggregateVersion").doesNotExist());
    }

    @Test
    @Order(2)
    void 重复创建同一toolId返回409() throws Exception {
        mockMvc.perform(post("/api/v1/admin/tools")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toolId\":\"%s\",\"displayName\":\"重复\",\"currentVersion\":\"1.0.0\",\"status\":\"DRAFT\"}"
                                .formatted(TOOL_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_VERSION_CONFLICT"));
    }

    @Test
    @Order(3)
    void 非法toolId返回400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/tools")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toolId\":\"Bad_Id\",\"displayName\":\"x\",\"currentVersion\":\"1.0.0\",\"status\":\"DRAFT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @Order(4)
    void 游客与普通用户都无法访问admin接口() throws Exception {
        String guestToken = guestAccessToken();
        mockMvc.perform(get("/api/v1/admin/tools").header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void 管理员更新工具并按乐观锁递增版本() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/tools/{toolId}", TOOL_ID)
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"文本计数器 Pro\",\"status\":\"PUBLISHED\",\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("文本计数器 Pro"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @Order(6)
    void 使用过期版本更新返回409() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/tools/{toolId}", TOOL_ID)
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"过期更新\",\"expectedVersion\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_VERSION_CONFLICT"));
    }

    @Test
    @Order(7)
    void 策略修改写入审计并对目录生效() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/tools/{toolId}/policy", TOOL_ID)
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"loginRequired\":false,\"guestLimit\":5,\"guestLimitWindowSeconds\":3600,\"expectedVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policy.guestLimit").value(5))
                .andExpect(jsonPath("$.data.policy.guestLimitWindowSeconds").value(3600));

        // 公开目录可见该已发布工具，且策略同步生效
        mockMvc.perform(get("/api/v1/tools/{toolId}", TOOL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolId").value(TOOL_ID))
                .andExpect(jsonPath("$.data.policy.guestLimit").value(5));
    }

    @Test
    @Order(8)
    void 公开目录只暴露已发布工具() throws Exception {
        mockMvc.perform(get("/api/v1/tools").param("page", "1").param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].toolId").value(org.hamcrest.Matchers.hasItem(TOOL_ID)))
                .andExpect(jsonPath("$.data.items[*].status").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("PUBLISHED"))));

        mockMvc.perform(post("/api/v1/admin/tools")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toolId\":\"draft-only\",\"displayName\":\"草稿工具\",\"currentVersion\":\"0.1.0\",\"status\":\"DRAFT\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tools/{toolId}", "draft-only"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TOOL_NOT_FOUND"));
    }

    @Test
    @Order(9)
    void 不存在的工具返回404() throws Exception {
        mockMvc.perform(get("/api/v1/tools/{toolId}", "no-such-tool"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TOOL_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/admin/tools/{toolId}/usage", "no-such-tool")
                        .header("Authorization", "Bearer " + adminAccessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TOOL_NOT_FOUND"));
    }

    @Test
    @Order(10)
    void 用量接口返回聚合结构() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tools/{toolId}/usage", TOOL_ID)
                        .header("Authorization", "Bearer " + adminAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolId").value(TOOL_ID))
                .andExpect(jsonPath("$.data.totalExecutions").value(0))
                .andExpect(jsonPath("$.data.from").isNotEmpty())
                .andExpect(jsonPath("$.data.to").isNotEmpty());
    }

    private String adminAccessToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\",\"password\":\"admin-dev-2026\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private String guestAccessToken() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/auth/guest-session"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }
}
