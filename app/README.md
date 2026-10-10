# HarmonyOS 客户端骨架

## 当前状态

这是 Stage 模型的手机/平板客户端空壳，只包含入口 Ability、首页占位页面和工程构建配置，不包含具体工具业务。

## 打开和构建

使用 DevEco Studio 打开本目录，安装 `oh-package.json5` 依赖后构建 `entry` 模块。

当前工程按 API 12 / `5.0.0(12)` 兼容配置初始化；如果实际 DevEco SDK 不同，必须在确认后同步修改 `build-profile.json5` 和文档，不能只改本地配置。

## 目录

```text
AppScope/                  # 应用级 app.json5
entry/                     # HAP 模块
└─ src/main/
   ├─ ets/entryability/    # 入口 Ability
   ├─ ets/pages/           # 页面
   ├─ resources/           # 字符串、颜色、页面配置
   └─ module.json5
```
