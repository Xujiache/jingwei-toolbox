package com.xujiache.jingweitoolbox.tool;

import com.xujiache.jingweitoolbox.common.api.ApiException;
import com.xujiache.jingweitoolbox.common.api.ErrorCode;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolData;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolListData;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolPatchRequest;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolPolicyData;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolPolicyPatch;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolUpsertRequest;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.UsageData;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import com.xujiache.jingweitoolbox.common.web.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工具注册与策略管理。策略修改与审计日志同事务写入；所有更新基于
 * aggregate_version 乐观锁，冲突返回 409 RESOURCE_VERSION_CONFLICT。
 */
@Service
public class ToolService {

    private static final Logger log = LoggerFactory.getLogger(ToolService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final ToolRepository tools;
    private final ObjectMapper objectMapper;

    public ToolService(ToolRepository tools, ObjectMapper objectMapper) {
        this.tools = tools;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ToolData createTool(ToolUpsertRequest request, String actorId) {
        if (tools.existsByToolId(request.toolId())) {
            throw new ApiException(ErrorCode.RESOURCE_VERSION_CONFLICT, "toolId 已存在: " + request.toolId());
        }
        String inputJson = toJson(request.inputSchema());
        String outputJson = toJson(request.outputSchema());
        tools.insertTool(request.toolId(), request.displayName(), request.description(),
                request.status(), request.currentVersion(), inputJson, outputJson);
        tools.insertPolicyDefaults(request.toolId());
        tools.insertToolVersionSnapshot(request.toolId(), request.currentVersion(), inputJson, outputJson);
        writeAudit(actorId, "TOOL_CREATE", request.toolId(), Map.of("status", request.status()));
        return tools.findByToolId(request.toolId()).map(this::toData)
                .orElseThrow(() -> new IllegalStateException("工具创建后读取失败"));
    }

    public ToolData getToolForAdmin(String toolId) {
        return tools.findByToolId(toolId).map(this::toData)
                .orElseThrow(() -> toolNotFound(toolId));
    }

    public ToolData getPublishedTool(String toolId) {
        ToolRepository.ToolRecord record = tools.findByToolId(toolId)
                .orElseThrow(() -> toolNotFound(toolId));
        if (!ToolStatus.PUBLISHED.name().equals(record.status()) || !record.enabled()) {
            throw toolNotFound(toolId);
        }
        return toData(record);
    }

    public ToolListData listForAdmin(Integer page, Integer pageSize, String status) {
        return list(page, pageSize, status);
    }

    public ToolListData listPublished(Integer page, Integer pageSize) {
        return list(page, pageSize, ToolStatus.PUBLISHED.name());
    }

    @Transactional
    public ToolData patchTool(String toolId, ToolPatchRequest request, String actorId) {
        ToolRepository.ToolRecord existing = tools.findByToolId(toolId)
                .orElseThrow(() -> toolNotFound(toolId));
        int updated = tools.updateTool(toolId, existing, request.displayName(), request.description(),
                request.currentVersion(), request.status(),
                request.inputSchema() != null ? toJson(request.inputSchema()) : null,
                request.outputSchema() != null ? toJson(request.outputSchema()) : null,
                request.expectedVersion());
        if (updated == 0) {
            throw versionConflict(toolId, request.expectedVersion());
        }
        if (request.currentVersion() != null) {
            tools.insertToolVersionSnapshot(toolId, request.currentVersion(),
                    request.inputSchema() != null ? toJson(request.inputSchema()) : existing.inputSchemaJson(),
                    request.outputSchema() != null ? toJson(request.outputSchema()) : existing.outputSchemaJson());
        }
        writeAudit(actorId, "TOOL_UPDATE", toolId, Map.of("expectedVersion", request.expectedVersion()));
        return tools.findByToolId(toolId).map(this::toData)
                .orElseThrow(() -> toolNotFound(toolId));
    }

    @Transactional
    public ToolData patchPolicy(String toolId, ToolPolicyPatch request, String actorId) {
        ToolRepository.ToolRecord existing = tools.findByToolId(toolId)
                .orElseThrow(() -> toolNotFound(toolId));
        int bumped = tools.bumpAggregateVersion(toolId, request.expectedVersion());
        if (bumped == 0) {
            throw versionConflict(toolId, request.expectedVersion());
        }
        tools.updatePolicy(toolId, existing, request.enabled(), request.loginRequired(),
                request.guestLimit(), request.guestLimitWindowSeconds());
        Map<String, Object> auditDetails = new LinkedHashMap<>();
        auditDetails.put("old", toData(existing).policy());
        auditDetails.put("new", toPolicyData(tools.findByToolId(toolId).orElseThrow(() -> toolNotFound(toolId))));
        writeAudit(actorId, "TOOL_POLICY_UPDATE", toolId, auditDetails);
        return tools.findByToolId(toolId).map(this::toData)
                .orElseThrow(() -> toolNotFound(toolId));
    }

    public UsageData usage(String toolId, Instant from, Instant to) {
        tools.findByToolId(toolId).orElseThrow(() -> toolNotFound(toolId));
        long total = tools.sumUsage(toolId, from, to, false);
        long guest = tools.sumUsage(toolId, from, to, true);
        return new UsageData(toolId, from, to, total, guest, total - guest);
    }

    private ToolListData list(Integer page, Integer pageSize, String status) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, MAX_PAGE_SIZE);
        List<ToolData> items = tools.list(status, safeSize, (long) (safePage - 1) * safeSize)
                .stream().map(this::toData).toList();
        return new ToolListData(items, safePage, safeSize, tools.count(status));
    }

    private ToolData toData(ToolRepository.ToolRecord record) {
        return new ToolData(record.toolId(), record.displayName(), record.description(),
                record.status(), record.currentVersion(),
                fromJson(record.inputSchemaJson()), fromJson(record.outputSchemaJson()),
                toPolicyData(record));
    }

    private ToolPolicyData toPolicyData(ToolRepository.ToolRecord record) {
        return new ToolPolicyData(record.enabled(), record.loginRequired(),
                record.guestLimit(), record.guestLimitWindowSeconds());
    }

    private ApiException toolNotFound(String toolId) {
        return new ApiException(ErrorCode.TOOL_NOT_FOUND, "工具不存在: " + toolId);
    }

    private ApiException versionConflict(String toolId, long expectedVersion) {
        return new ApiException(ErrorCode.RESOURCE_VERSION_CONFLICT,
                "工具已被其他管理员修改，请刷新后重试: " + toolId + " (expectedVersion=" + expectedVersion + ")");
    }

    private void writeAudit(String actorId, String action, String target, Map<String, Object> details) {
        try {
            tools.insertAuditLog(actorId, action, target, toJson(details), MDC.get(RequestIdFilter.MDC_KEY));
        } catch (RuntimeException ex) {
            // 审计写失败不阻断业务主流程，但要留全栈日志
            log.error("审计日志写入失败 action={} target={}", action, target, ex);
        }
    }

    private String toJson(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        return objectMapper.writeValueAsString(value);
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readValue(json, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {
        });
    }
}
