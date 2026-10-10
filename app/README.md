# HarmonyOS 客户端骨架

## 当前状态

这是 Stage 模型的手机/平板客户端基础框架，包含入口 Ability、工具箱/账号双 Tab 外壳、平台契约类型和资源配置，不包含具体工具业务。

## 打开和构建

使用 DevEco Studio 打开本目录，安装 `oh-package.json5` 依赖后构建 `entry` 模块。

当前工程按 API 12 / `5.0.0(12)` 兼容配置初始化；如果实际 DevEco SDK 不同，必须在确认后同步修改 `build-profile.json5` 和文档，不能只改本地配置。

## 目录

```text
AppScope/                  # 应用级 app.json5
entry/                     # HAP 模块
└─ src/main/
   ├─ ets/entryability/    # 入口 Ability
   ├─ ets/foundation/      # API 契约和平台边界
   ├─ ets/pages/           # 页面外壳
   ├─ resources/           # 字符串、颜色、页面配置
   └─ module.json5
```

## 前端边界

- `ets/foundation/AppContract.ets` 与 `docs/api/openapi.json` 的通用响应、路径、认证和工具策略字段保持一致。
- `API_BASE_URL` 只作为本地开发默认值；正式环境必须通过发布配置注入，不能把 Token、密码或生产地址写入源码。
- 工具列表、工具表单和工具执行页面必须在接口契约确定后按工具版本逐项增加，不能在骨架中放置示例工具或假数据。
