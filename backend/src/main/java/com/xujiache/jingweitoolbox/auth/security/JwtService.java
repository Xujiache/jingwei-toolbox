package com.xujiache.jingweitoolbox.auth.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.xujiache.jingweitoolbox.auth.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * 基于 HS256 的 JWT 签发与解码。claim 约定：sub=用户或游客标识，sid=会话行 ID，
 * typ=GUEST/USER/ADMIN，name=展示名。签名与过期校验由解码器默认规则承担。
 */
@Service
public class JwtService {

    public static final String CLAIM_SESSION_ID = "sid";
    public static final String CLAIM_PRINCIPAL_TYPE = "typ";
    public static final String CLAIM_DISPLAY_NAME = "name";

    private final SecretKey secretKey;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final AuthProperties properties;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        this.secretKey = new SecretKeySpec(
                properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(
                new JWKSet(new OctetSequenceKey.Builder(secretKey).keyID("jingwei-hs256").build()));
        this.encoder = new NimbusJwtEncoder(jwkSource);
        this.decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * 签发访问令牌或刷新令牌。
     *
     * @param sessionId     会话行 ID（写入 sid claim）
     * @param subject       用户 ID 或游客标识
     * @param principalType GUEST/USER/ADMIN
     * @param displayName   展示名，游客为 null
     * @param ttl           有效期
     */
    public String issueToken(String sessionId, String subject, String principalType, String displayName, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim(CLAIM_SESSION_ID, sessionId)
                .claim(CLAIM_PRINCIPAL_TYPE, principalType)
                .issuedAt(now)
                .expiresAt(now.plus(ttl));
        if (displayName != null) {
            claims.claim(CLAIM_DISPLAY_NAME, displayName);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        try {
            return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        } catch (JwtEncodingException ex) {
            throw new IllegalStateException("JWT 签发失败", ex);
        }
    }

    /**
     * 解码并校验签名与过期时间，非法令牌抛出 JwtException。
     */
    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    public Duration accessTtl() {
        return Duration.ofMinutes(properties.accessTokenTtlMinutes());
    }

    public Duration refreshTtl() {
        return Duration.ofDays(properties.refreshTokenTtlDays());
    }

    public Duration guestTtl() {
        return Duration.ofHours(properties.guestSessionTtlHours());
    }

    /**
     * 令牌指纹，用于 auth_sessions.token_hash，避免数据库存储明文令牌。
     */
    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
