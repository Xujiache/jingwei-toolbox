# 项目开发规划

## 1. 项目概况

### 已确认

- 正式名称：经纬智能工具箱
- 用户：个人用户
- 客户端：HarmonyOS 手机和平板
- 客户端技术：HarmonyOS 原生开发框架、ArkTS、ArkUI
- 后端：Java + Spring Boot
- 数据库：MySQL 8
- 管理后台：Vue 3（当前基础模板位于 `admin/`）
- GitHub：`https://github.com/Xujiache/jingwei-toolbox`
- 分支：`main` 发布、`develop` 集成、`feature/*` 功能、`fix/*` 修复

### 当前不预先确定

- 第一批具体工具及其业务规则
- Spring Boot 具体版本、缓存、消息队列和部署平台
- HarmonyOS API/SDK 版本、应用包名和正式签名
- 生产域名、数据库连接参数和监控平台

这些项目不能用假数据或临时接口提前“占位完成”。确定后必须补充到本目录和环境配置说明。

## 2. 目标与非目标

### 目标

1. 提供可持续增加工具的统一工具箱客户端。
2. 支持游客使用和可选登录。
3. 管理后台可以配置工具启用状态、登录要求、游客次数上限和限额周期。
4. 后端统一负责认证、授权、游客额度、工具版本和审计。
5. 让 HarmonyOS 客户端和管理后台使用同一份接口契约，避免字段漂移。

### 非目标

- 当前阶段不实现任何尚未确认的具体工具业务。
- 不把后台模板中的示例页面当成已完成业务。
- 不提前引入没有实际依赖的缓存、队列、微服务或多租户抽象。
- 不通过前端隐藏按钮代替后端权限检查。

## 3. 总体架构

```text
HarmonyOS App ───────┐
                     ├─ HTTPS /api/v1 ── Java + Spring Boot ── MySQL 8
Vue 3 Admin ─────────┘                         │
                                               ├─ 认证与会话
                                               ├─ 工具目录与版本
                                               ├─ 权限与游客额度
                                               ├─ 工具执行适配层
                                               └─ 审计与运行日志
```

### 模块边界

```text
app/       HarmonyOS 页面、状态、网络层、工具容器和客户端错误展示
backend/   Spring Boot API、领域服务、数据库迁移、认证和工具执行编排
admin/     Vue 3 后台页面、权限路由、工具策略配置和运营视图
docs/      需求、架构、接口契约、验收和变更记录
scripts/   可重复的构建、检查和契约校验脚本
```

## 4. 通用工具模块标准

每个工具必须具备稳定的 `toolId`，并通过注册信息描述自身，不允许客户端和后台各写一份互不一致的工具清单。

### 工具元数据

- `toolId`：永久稳定、只允许小写字母、数字和短横线
- `displayName`、`description`
- `status`：`DRAFT`、`PUBLISHED`、`DISABLED`、`DEPRECATED`
- `currentVersion`：语义化版本或项目约定版本
- `inputSchema`、`outputSchema`
- `loginRequired`
- `guestLimit`、`guestLimitWindowSeconds`
- 客户端展示信息和兼容的最小 API 版本

### 工具生命周期

```text
草稿 → 契约评审 → 后端实现 → 客户端/后台接入 → 集成验收 → 发布
                                      ↓
                                禁用/废弃/回滚
```

### 工具执行约束

- 登录判断和额度扣减必须由后端在一次受保护的业务流程中完成。
- 额度扣减必须幂等；重复的 `clientRequestId` 不得重复消耗次数。
- 工具执行失败时明确区分输入错误、权限失败、额度耗尽和外部依赖失败。
- 工具版本升级不得静默改变旧版本的输入输出含义；不兼容变更必须增加版本或迁移策略。

## 5. 推荐数据模型

以下是首期平台模型，不代表已经创建数据库表：

| 模型 | 作用 | 关键字段 |
|---|---|---|
| `users` | 用户账号和状态 | `id`、`username`、`status`、`created_at` |
| `auth_sessions` | 登录/游客会话 | `id`、`user_id`、`session_type`、`expires_at`、`revoked_at` |
| `tools` | 工具注册信息 | `tool_id`、`status`、`current_version` |
| `tool_versions` | 工具版本和契约 | `tool_id`、`version`、`input_schema`、`output_schema` |
| `tool_policies` | 登录和游客额度 | `tool_id`、`login_required`、`guest_limit`、`window_seconds` |
| `tool_usage_counters` | 额度统计和幂等记录 | `tool_id`、`subject_id`、`window_start`、`request_id`、`used_count` |
| `audit_logs` | 后台策略和发布审计 | `actor_id`、`action`、`target`、`created_at` |

表结构必须通过可回滚迁移管理，不直接修改生产数据库。

## 6. 分阶段计划

### 阶段 0：契约和骨架

- 确认本目录文档、OpenAPI、错误码和认证方式。
- 确认 Java、Spring Boot、MySQL 8、HarmonyOS SDK 和 Node/pnpm 工具链版本。
- 创建 `backend/`、`app/` 的最小工程骨架，但不实现具体工具。

### 阶段 1：平台基础能力

- 后端：健康检查、配置、数据库迁移、统一响应、异常处理、日志和认证会话。
- 客户端：网络层、统一响应解析、登录/游客会话、工具列表页面骨架。
- 管理后台：登录接入、工具目录、策略编辑页面骨架。

### 阶段 2：权限和额度闭环

- 后端实现工具策略、游客额度、幂等扣减和审计。
- 客户端展示登录要求、剩余次数和明确错误状态。
- 管理后台可以修改策略并通过接口验证真实生效。

### 阶段 3：第一个真实工具

- 由产品先提供具体工具的输入、输出、限制和验收样例。
- 先更新 OpenAPI 和工具契约，再由后端、客户端、后台并行实现。
- 完成一条真实链路：用户输入 → 后端校验 → 工具执行 → 额度变化 → 客户端展示。

### 阶段 4：持续新增工具

- 每个工具独立版本、独立契约、独立测试和独立发布记录。
- 新工具不能修改已有工具的语义；公共能力变更需要兼容性评审。

## 7. 验收门禁

每个阶段必须留下可复现证据：

- 静态检查和类型检查结果
- 后端单元/集成测试结果
- OpenAPI 契约校验结果
- 管理后台构建结果
- HarmonyOS 构建、安装和真机运行结果
- 游客、登录、权限拒绝、额度耗尽和重复请求结果
- 数据库迁移和回滚结果

编译成功、页面打开或 HTTP 200 不能单独代表业务完成。
