package com.xujiache.jingweitoolbox.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证相关配置，均可通过环境变量覆盖（见 application.yml 的 app.auth 段）。
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays,
        long guestSessionTtlHours) {

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes().length < 32) {
            throw new IllegalArgumentException("JWT 密钥长度不足：HS256 需要至少 32 字节");
        }
    }
}
