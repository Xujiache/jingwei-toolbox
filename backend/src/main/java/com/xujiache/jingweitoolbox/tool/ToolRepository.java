package com.xujiache.jingweitoolbox.tool;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * tools / tool_policies / tool_usage_counters 表访问。
 */
@Repository
public class ToolRepository {

    public record ToolRecord(long id, String toolId, String displayName, String description, String status,
            String currentVersion, String inputSchemaJson, String outputSchemaJson, long aggregateVersion,
            boolean enabled, boolean loginRequired, Integer guestLimit, Integer guestLimitWindowSeconds) {
    }

    private final JdbcClient jdbc;
    private final JdbcTemplate template;

    public ToolRepository(JdbcClient jdbc, JdbcTemplate template) {
        this.jdbc = jdbc;
        this.template = template;
    }

    private static final String SELECT_TOOL = """
            SELECT t.id, t.tool_id, t.display_name, t.description, t.status, t.current_version,
                   t.input_schema, t.output_schema, t.aggregate_version,
                   p.enabled, p.login_required, p.guest_limit, p.guest_limit_window_seconds
            FROM tools t JOIN tool_policies p ON p.tool_id = t.tool_id
            """;

    public long insertTool(String toolId, String displayName, String description, String status,
            String currentVersion, String inputSchemaJson, String outputSchemaJson) {
        KeyHolder keys = new GeneratedKeyHolder();
        template.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO tools (tool_id, display_name, description, status, current_version, input_schema, output_schema) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, toolId);
            ps.setString(2, displayName);
            ps.setString(3, description);
            ps.setString(4, status);
            ps.setString(5, currentVersion);
            ps.setString(6, inputSchemaJson);
            ps.setString(7, outputSchemaJson);
            return ps;
        }, keys);
        return extractId(keys);
    }

    public void insertPolicyDefaults(String toolId) {
        jdbc.sql("INSERT INTO tool_policies (tool_id, enabled, login_required, guest_limit, guest_limit_window_seconds) "
                        + "VALUES (:toolId, TRUE, FALSE, NULL, NULL)")
                .param("toolId", toolId)
                .update();
    }

    public void insertToolVersionSnapshot(String toolId, String version, String inputSchemaJson, String outputSchemaJson) {
        jdbc.sql("INSERT INTO tool_versions (tool_id, version, input_schema, output_schema) "
                        + "VALUES (:toolId, :version, :inputSchema, :outputSchema)")
                .param("toolId", toolId)
                .param("version", version)
                .param("inputSchema", inputSchemaJson)
                .param("outputSchema", outputSchemaJson)
                .update();
    }

    public boolean existsByToolId(String toolId) {
        Integer count = jdbc.sql("SELECT COUNT(*) FROM tools WHERE tool_id = :toolId")
                .param("toolId", toolId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    public Optional<ToolRecord> findByToolId(String toolId) {
        return jdbc.sql(SELECT_TOOL + " WHERE t.tool_id = :toolId")
                .param("toolId", toolId)
                .query((rs, rowNum) -> mapRow(rs))
                .optional();
    }

    public List<ToolRecord> list(String status, int pageSize, long offset) {
        StringBuilder sql = new StringBuilder(SELECT_TOOL);
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append(" WHERE t.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY t.id LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);
        return template.query(sql.toString(), (rs, rowNum) -> mapRow(rs), params.toArray());
    }

    public long count(String status) {
        if (status == null) {
            Integer total = jdbc.sql("SELECT COUNT(*) FROM tools").query(Integer.class).single();
            return total == null ? 0 : total;
        }
        Integer total = jdbc.sql("SELECT COUNT(*) FROM tools WHERE status = :status")
                .param("status", status)
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }

    /**
     * 乐观锁更新工具基础信息，返回受影响行数（0 表示版本冲突或工具不存在）。
     */
    public int updateTool(String toolId, ToolRecord existing, String displayName, String description,
            String currentVersion, String status, String inputSchemaJson, String outputSchemaJson,
            long expectedVersion) {
        StringBuilder sql = new StringBuilder("UPDATE tools SET ");
        List<Object> params = new ArrayList<>();
        if (displayName != null) {
            sql.append("display_name = ?, ");
            params.add(displayName);
        } else {
            sql.append("display_name = ?, ");
            params.add(existing.displayName());
        }
        if (description != null) {
            sql.append("description = ?, ");
            params.add(description);
        } else {
            sql.append("description = ?, ");
            params.add(existing.description());
        }
        if (currentVersion != null) {
            sql.append("current_version = ?, ");
            params.add(currentVersion);
            sql.append("input_schema = ?, ");
            params.add(inputSchemaJson != null ? inputSchemaJson : existing.inputSchemaJson());
            sql.append("output_schema = ?, ");
            params.add(outputSchemaJson != null ? outputSchemaJson : existing.outputSchemaJson());
        } else if (inputSchemaJson != null || outputSchemaJson != null) {
            sql.append("input_schema = ?, ");
            params.add(inputSchemaJson != null ? inputSchemaJson : existing.inputSchemaJson());
            sql.append("output_schema = ?, ");
            params.add(outputSchemaJson != null ? outputSchemaJson : existing.outputSchemaJson());
        }
        if (status != null) {
            sql.append("status = ?, ");
            params.add(status);
        }
        sql.append("updated_at = CURRENT_TIMESTAMP, aggregate_version = aggregate_version + 1 "
                + "WHERE tool_id = ? AND aggregate_version = ?");
        params.add(toolId);
        params.add(expectedVersion);
        return template.update(sql.toString(), params.toArray());
    }

    /**
     * 乐观锁抢占聚合版本，返回受影响行数（0 = 版本冲突或工具不存在）。
     */
    public int bumpAggregateVersion(String toolId, long expectedVersion) {
        return template.update(
                "UPDATE tools SET updated_at = CURRENT_TIMESTAMP, aggregate_version = aggregate_version + 1 "
                        + "WHERE tool_id = ? AND aggregate_version = ?",
                toolId, expectedVersion);
    }

    public int updatePolicy(String toolId, ToolRecord existing, Boolean enabled, Boolean loginRequired,
            Integer guestLimit, Integer guestLimitWindowSeconds) {
        return template.update(
                "UPDATE tool_policies SET enabled = ?, login_required = ?, guest_limit = ?, "
                        + "guest_limit_window_seconds = ?, updated_at = CURRENT_TIMESTAMP WHERE tool_id = ?",
                enabled != null ? enabled : existing.enabled(),
                loginRequired != null ? loginRequired : existing.loginRequired(),
                guestLimit != null ? guestLimit : existing.guestLimit(),
                guestLimitWindowSeconds != null ? guestLimitWindowSeconds : existing.guestLimitWindowSeconds(),
                toolId);
    }

    public long sumUsage(String toolId, Instant from, Instant to, boolean guestOnly) {
        String sql = "SELECT COALESCE(SUM(used_count), 0) FROM tool_usage_counters "
                + "WHERE tool_id = :toolId AND window_start >= :from AND window_start < :to "
                + (guestOnly ? "AND subject_id LIKE 'guest-%'" : "AND subject_id NOT LIKE 'guest-%'");
        Long sum = jdbc.sql(sql)
                .param("toolId", toolId)
                .param("from", Timestamp.from(from))
                .param("to", Timestamp.from(to))
                .query(Long.class)
                .single();
        return sum == null ? 0 : sum;
    }

    public void insertAuditLog(String actorId, String action, String target, String detailsJson, String requestId) {
        jdbc.sql("INSERT INTO audit_logs (actor_id, action, target, details, request_id) "
                        + "VALUES (:actorId, :action, :target, :details, :requestId)")
                .param("actorId", actorId)
                .param("action", action)
                .param("target", target)
                .param("details", detailsJson)
                .param("requestId", requestId)
                .update();
    }

    private ToolRecord mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ToolRecord(
                rs.getLong("id"),
                rs.getString("tool_id"),
                rs.getString("display_name"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getString("current_version"),
                rs.getString("input_schema"),
                rs.getString("output_schema"),
                rs.getLong("aggregate_version"),
                rs.getBoolean("enabled"),
                rs.getBoolean("login_required"),
                (Integer) rs.getObject("guest_limit"),
                (Integer) rs.getObject("guest_limit_window_seconds"));
    }

    private long extractId(KeyHolder keys) {
        // H2 返回 id 列加默认值列，MySQL 驱动返回名为 generated_key 的单列，两种都要兼容
        return keys.getKeys().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("id"))
                .map(java.util.Map.Entry::getValue)
                .map(Number.class::cast)
                .map(Number::longValue)
                .findFirst()
                .orElseGet(() -> keys.getKeys().values().stream()
                        .map(Number.class::cast)
                        .map(Number::longValue)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("tools 插入未返回主键")));
    }
}
