package com.xujiache.jingweitoolbox.auth;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * auth_sessions 表访问。token_hash 存访问令牌的 SHA-256 指纹，不存明文。
 */
@Repository
public class SessionRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate template;

    public SessionRepository(JdbcClient jdbc, JdbcTemplate template) {
        this.jdbc = jdbc;
        this.template = template;
    }

    @Transactional
    public long create(Long userId, String sessionType, String tokenHash, Instant expiresAt) {
        KeyHolder keys = new GeneratedKeyHolder();
        template.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO auth_sessions (user_id, session_type, token_hash, expires_at) "
                            + "VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            if (userId == null) {
                ps.setNull(1, java.sql.Types.BIGINT);
            } else {
                ps.setLong(1, userId);
            }
            ps.setString(2, sessionType);
            ps.setString(3, tokenHash);
            ps.setTimestamp(4, Timestamp.from(expiresAt));
            return ps;
        }, keys);
        // H2 会把 created_at 等默认值列一并返回为生成键（取名为 id 的列），
        // MySQL 驱动则返回单列但列名为 generated_key，两种命名都要兼容
        Number key = keys.getKeys().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("id"))
                .map(java.util.Map.Entry::getValue)
                .map(Number.class::cast)
                .findFirst()
                .orElseGet(() -> keys.getKeys().values().stream()
                        .map(Number.class::cast)
                        .findFirst()
                        .orElse(null));
        if (key == null) {
            throw new IllegalStateException("auth_sessions 插入未返回主键");
        }
        return key.longValue();
    }

    @Transactional
    public void updateTokenHash(long sessionId, String tokenHash) {
        jdbc.sql("UPDATE auth_sessions SET token_hash = :tokenHash WHERE id = :id")
                .param("tokenHash", tokenHash)
                .param("id", sessionId)
                .update();
    }

    @Transactional
    public void revoke(long sessionId) {
        jdbc.sql("UPDATE auth_sessions SET revoked_at = CURRENT_TIMESTAMP "
                        + "WHERE id = :id AND revoked_at IS NULL")
                .param("id", sessionId)
                .update();
    }

    /**
     * 会话有效 = 存在、未撤销、未过期、指纹与当前访问令牌一致。
     */
    public boolean isActive(long sessionId, String tokenHash) {
        Integer active = jdbc.sql("SELECT COUNT(*) FROM auth_sessions "
                        + "WHERE id = :id AND token_hash = :tokenHash "
                        + "AND revoked_at IS NULL AND expires_at > :now")
                .param("id", sessionId)
                .param("tokenHash", tokenHash)
                .param("now", Timestamp.from(Instant.now()))
                .query(Integer.class)
                .single();
        return active != null && active == 1;
    }

    /**
     * 刷新流程用：仅检查会话本身未撤销、未过期，不比对访问令牌指纹。
     */
    public boolean isUsable(long sessionId) {
        Integer usable = jdbc.sql("SELECT COUNT(*) FROM auth_sessions "
                        + "WHERE id = :id AND revoked_at IS NULL AND expires_at > :now")
                .param("id", sessionId)
                .param("now", Timestamp.from(Instant.now()))
                .query(Integer.class)
                .single();
        return usable != null && usable == 1;
    }

    public Optional<Instant> expiresAt(long sessionId) {
        return jdbc.sql("SELECT expires_at FROM auth_sessions WHERE id = :id")
                .param("id", sessionId)
                .query((rs, rowNum) -> rs.getTimestamp("expires_at").toInstant())
                .optional();
    }
}
