package com.xujiache.jingweitoolbox.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void V1迁移创建全部七张平台表() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA", String.class);

        assertThat(tables).contains(
                "users", "auth_sessions", "tools", "tool_versions",
                "tool_policies", "tool_usage_counters", "audit_logs");
    }

    @Test
    void V2种子管理员存在且具备ADMIN角色() {
        Integer admins = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE identifier = 'admin' AND role = 'ADMIN' AND password_hash IS NOT NULL",
                Integer.class);

        assertThat(admins).isEqualTo(1);
    }
}
