# 经纬智能工具箱开发文档

## 文档状态

- 版本：`0.1.0-draft`
- 状态：项目初始化阶段，可作为两位开发者的共同基线
- 适用范围：HarmonyOS 客户端、Java + Spring Boot 后端、Vue 3 管理后台
- 当前限制：具体工具业务尚未确定，因此本文档只定义可复用的平台能力和协作契约，不虚构具体工具功能

## 阅读顺序

1. [项目开发规划](./PROJECT_PLAN.md)：产品边界、架构、阶段目标和验收门禁
2. [两人开发分工与工作流](./TEAM_WORKFLOW.md)：职责、分支、交接和评审规则
3. [接口契约规范](./API_CONTRACT.md)：接口唯一来源、认证、错误、额度和兼容策略
4. [OpenAPI 接口基线](./api/openapi.json)：机器可读的公共接口契约

## 维护规则

- `api/openapi.json` 是接口字段、路径、状态码和认证方式的唯一基线。
- Markdown 文档用于解释背景和流程；与 OpenAPI 冲突时，必须先修订 OpenAPI，再同步 Markdown。
- 任何接口变更必须同时更新后端控制器/测试、前端类型或请求封装、管理后台调用和本目录文档。
- 文档中的“计划”“待确认”不能当作已实现功能；交付报告必须区分计划、实现、验证和未验证内容。
- 每次发布记录文档版本、API 版本、数据库迁移版本和回滚方式。

## 推荐目录

```text
docs/
├─ README.md
├─ PROJECT_PLAN.md
├─ TEAM_WORKFLOW.md
├─ API_CONTRACT.md
└─ api/
   └─ openapi.json
```
