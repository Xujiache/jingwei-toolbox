package com.xujiache.jingweitoolbox.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * 认证接口的请求/响应数据结构，字段与 docs/api/openapi.json 一致。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(min = 1, max = 128) String identifier,
            @NotBlank @Size(min = 1, max = 256) String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuthData(
            String accessToken,
            String refreshToken,
            String tokenType,
            Instant expiresAt,
            String principalType) {
    }

    public record PrincipalData(
            String principalType,
            String userId,
            String displayName,
            List<String> scopes) {
    }
}
