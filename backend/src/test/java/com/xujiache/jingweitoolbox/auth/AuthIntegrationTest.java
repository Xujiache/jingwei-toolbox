package com.xujiache.jingweitoolbox.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 认证全流程集成测试：游客会话、登录、刷新、注销与权限边界。
 * 运行于 H2（test profile），与 CI 口径一致。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String SEED_ADMIN_IDENTIFIER = "admin";
    private static final String SEED_ADMIN_PASSWORD = "admin-dev-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 游客会话可创建并访问当前身份() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/auth/guest-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.principalType").value("GUEST"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andReturn();

        String token = readToken(created);

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.principalType").value("GUEST"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.scopes").isArray());
    }

    @Test
    void 无令牌访问受保护接口返回401错误封套() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void 游客令牌访问admin接口返回403() throws Exception {
        String token = readToken(mockMvc.perform(post("/api/v1/auth/guest-session")).andReturn());

        mockMvc.perform(get("/api/v1/admin/tools").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 种子管理员可登录并访问admin接口() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(get("/api/v1/admin/tools").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void 错误密码返回401且不区分账号与密码错误() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 登录缺少密码返回400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details.fields[0].field").value("password"));
    }

    @Test
    void 刷新后新访问令牌可用且旧令牌立即失效() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + SEED_ADMIN_IDENTIFIER + "\",\"password\":\"" + SEED_ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginData = readData(login);
        String oldAccess = loginData.get("accessToken").asText();
        String refreshToken = loginData.get("refreshToken").asText();

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.principalType").value("ADMIN"))
                .andReturn();
        String newAccess = readToken(refreshed);

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.principalType").value("ADMIN"))
                .andExpect(jsonPath("$.data.userId").isNotEmpty());

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + oldAccess))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 注销后访问令牌与刷新令牌同时失效() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + SEED_ADMIN_IDENTIFIER + "\",\"password\":\"" + SEED_ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loginData = readData(login);
        String access = loginData.get("accessToken").asText();
        String refreshToken = loginData.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + access))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 篡改的令牌返回401() throws Exception {
        String token = adminAccessToken();
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private String adminAccessToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + SEED_ADMIN_IDENTIFIER + "\",\"password\":\"" + SEED_ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return readToken(login);
    }

    private String readToken(MvcResult result) throws Exception {
        return readData(result).get("accessToken").asText();
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }
}
