package com.xujiache.jingweitoolbox.tool;

/**
 * 工具生命周期状态，取值与 docs/api/openapi.json 的 ToolStatus 一致。
 */
public enum ToolStatus {
    DRAFT,
    PUBLISHED,
    DISABLED,
    DEPRECATED
}
