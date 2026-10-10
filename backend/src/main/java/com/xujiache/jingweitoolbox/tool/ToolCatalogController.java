package com.xujiache.jingweitoolbox.tool;

import com.xujiache.jingweitoolbox.common.api.ApiResponse;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolData;
import com.xujiache.jingweitoolbox.tool.dto.ToolDtos.ToolListData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开工具目录（无需认证，供鸿蒙客户端使用）。只暴露已发布且启用的工具。
 */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolCatalogController {

    private final ToolService toolService;

    public ToolCatalogController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    public ApiResponse<ToolListData> list(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(toolService.listPublished(page, pageSize));
    }

    @GetMapping("/{toolId}")
    public ApiResponse<ToolData> detail(@PathVariable String toolId) {
        return ApiResponse.success(toolService.getPublishedTool(toolId));
    }
}
