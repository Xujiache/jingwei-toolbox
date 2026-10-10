package com.xujiache.jingweitoolbox.tool;

import com.xujiache.jingweitoolbox.common.api.ApiResponse;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolData;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolListData;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolPatchRequest;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolPolicyPatch;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolUpsertRequest;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.UsageData;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台的工具目录与策略接口，路径与 docs/api/openapi.json 一致。
 */
@RestController
@RequestMapping("/api/v1/admin/tools")
public class AdminToolController {

    private final ToolService toolService;

    public AdminToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    public ApiResponse<ToolListData> list(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(toolService.listForAdmin(page, pageSize, status));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ToolData>> create(@Valid @RequestBody ToolUpsertRequest request,
            @AuthenticationPrincipal Jwt principal) {
        ToolData created = toolService.createTool(request, principal.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PatchMapping("/{toolId}")
    public ApiResponse<ToolData> patch(@PathVariable String toolId,
            @Valid @RequestBody ToolPatchRequest request,
            @AuthenticationPrincipal Jwt principal) {
        return ApiResponse.success(toolService.patchTool(toolId, request, principal.getSubject()));
    }

    @PatchMapping("/{toolId}/policy")
    public ApiResponse<ToolData> patchPolicy(@PathVariable String toolId,
            @Valid @RequestBody ToolPolicyPatch request,
            @AuthenticationPrincipal Jwt principal) {
        return ApiResponse.success(toolService.patchPolicy(toolId, request, principal.getSubject()));
    }

    @GetMapping("/{toolId}/usage")
    public ApiResponse<UsageData> usage(@PathVariable String toolId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        Instant end = to == null ? Instant.now() : to.toInstant();
        Instant start = from == null ? end.minusSeconds(24 * 3600) : from.toInstant();
        return ApiResponse.success(toolService.usage(toolId, start, end));
    }
}
