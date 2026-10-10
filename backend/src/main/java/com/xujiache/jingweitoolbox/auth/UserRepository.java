package com.xujiache.jingweitoolbox.auth;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * users 表访问。
 */
@Repository
public class UserRepository {

    public record UserRow(long id, String identifier, String passwordHash, String displayName,
            String role, String status) {
    }

    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRow> findByIdentifier(String identifier) {
        return jdbc.sql("SELECT id, identifier, password_hash, display_name, role, status FROM users WHERE identifier = :identifier")
                .param("identifier", identifier)
                .query((rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("identifier"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getString("role"),
                        rs.getString("status")))
                .optional();
    }
}
