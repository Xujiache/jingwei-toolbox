package com.xujiache.jingweitoolbox.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 工具目录与策略接口的数据结构，字段与 docs/api/openapi.json 一致。
 */
public final class ToolDtos {

    private ToolDtos() {
    }

    public record ToolUpsertRequest(
            @NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "toolId 只允许小写字母、数字和短横线") String toolId,
            @NotBlank String displayName,
            String description,
            @NotBlank String currentVersion,
            @NotNull String status,
            Map<String, Object> inputSchema,
            Map<String, Object> outputSchema) {
    }

    public record ToolPatchRequest(
            String displayName,
            String description,
            String currentVersion,
            String status,
            Map<String, Object> inputSchema,
            Map<String, Object> outputSchema,
            @NotNull @PositiveOrZero Long expectedVersion) {
    }

    public record ToolPolicyPatch(
            Boolean enabled,
            Boolean loginRequired,
            @PositiveOrZero Integer guestLimit,
            @Positive Integer guestLimitWindowSeconds,
            @NotNull @PositiveOrZero Long expectedVersion) {
    }

    // 契约要求 guestLimit / guestLimitWindowSeconds 可空但字段必须出现，因此不做 NON_NULL 省略
    public record ToolPolicyData(boolean enabled, boolean loginRequired,
            Integer guestLimit, Integer guestLimitWindowSeconds) {
    }

    public record ToolData(String toolId, String displayName, String description, String status,
            String currentVersion, Map<String, Object> inputSchema, Map<String, Object> outputSchema,
            ToolPolicyData policy) {
    }

    public record ToolListData(List<ToolData> items, int page, int pageSize, long total) {
    }

    public record UsageData(String toolId, Instant from, Instant to, long totalExecutions,
            long guestExecutions, long userExecutions) {
    }
}
