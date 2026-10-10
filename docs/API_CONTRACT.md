# 接口契约规范

## 1. 唯一来源

`docs/api/openapi.json` 是公共接口的唯一机器可读基线，覆盖 HarmonyOS 客户端和 Vue 3 管理后台。

- 后端实现必须与该文件一致。
- 前端请求封装、TypeScript 类型和测试必须根据该文件生成或核对。
- Markdown、截图、聊天记录和旧接口代码不能覆盖 OpenAPI 的当前定义。
- 任何接口 PR 必须同时修改 OpenAPI 和对应测试。

## 2. 基本约定

| 项目 | 约定 |
|---|---|
| Base path | `/api/v1` |
| 编码 | UTF-8 JSON |
| 时间 | ISO-8601 UTC，例如 `2026-10-10T18:00:00Z` |
| ID | 对外使用字符串；内部数据库类型由后端决定 |
| 成功响应 | `{ code, message, data, requestId }` |
| 失败响应 | `{ code, message, details, requestId }` |
| 认证 | `Authorization: Bearer <accessToken>` |
| 请求追踪 | 服务端响应必须返回 `requestId` |
| 幂等 | 执行类请求使用 `clientRequestId` |

## 3. 认证和游客会话

- 未登录用户通过 `POST /api/v1/auth/guest-session` 获得短期游客访问令牌。
- 登录用户通过 `POST /api/v1/auth/login` 获得访问令牌和刷新令牌。
- 客户端和后台都不得自行判断“是否需要登录”后绕过服务端；最终判断由后端工具策略执行。
- 游客令牌只能访问公开工具和策略允许的操作，不能访问后台接口。
- 注销必须使当前会话失效；刷新令牌必须支持撤销和过期检查。

## 4. 工具执行和额度

### 统一执行入口

```text
POST /api/v1/tools/{toolId}/execute
```

请求必须携带：

- `input`：工具输入对象，具体字段由工具版本契约定义
- `clientRequestId`：客户端生成的幂等 ID
- `toolVersion`：可选；不传使用当前发布版本

后端处理顺序：

```text
认证/游客身份 → 工具状态 → 登录要求 → 输入校验 → 幂等检查
→ 原子额度判断/扣减 → 工具执行 → 记录使用量 → 返回结果
```

额度耗尽返回 `429 TOOL_QUOTA_EXCEEDED`；重复的 `clientRequestId` 必须返回原执行结果或明确的幂等响应，不能重复扣减。

## 5. 错误码

错误码是稳定契约，文案可本地化但错误码不能随意改变：

| HTTP | code | 含义 |
|---:|---|---|
| 400 | `BAD_REQUEST` | 请求结构或参数非法 |
| 401 | `UNAUTHENTICATED` | 未提供或已失效的认证 |
| 403 | `FORBIDDEN` | 已认证但没有权限 |
| 404 | `TOOL_NOT_FOUND` | 工具不存在 |
| 409 | `IDEMPOTENCY_CONFLICT` | 幂等 ID 与原请求不一致 |
| 409 | `RESOURCE_VERSION_CONFLICT` | 修改基于过期版本 |
| 429 | `TOOL_QUOTA_EXCEEDED` | 游客或用户额度耗尽 |
| 500 | `INTERNAL_ERROR` | 未预期服务端错误 |
| 502 | `DEPENDENCY_ERROR` | 外部依赖失败 |
| 503 | `SERVICE_UNAVAILABLE` | 服务暂不可用 |

## 6. 后台策略修改

后台修改工具策略必须：

- 要求管理员认证和明确权限。
- 使用 `PATCH /api/v1/admin/tools/{toolId}/policy`。
- 支持并发版本字段，避免覆盖其他管理员的更新。
- 写入审计日志，记录操作者、旧值、新值和请求 ID。
- 修改成功后，客户端下一次读取工具详情即可看到新策略；不依赖重启客户端生效。

## 7. 兼容性规则

- 新增可选字段属于兼容变更。
- 删除字段、改变字段类型、改变必填性、改变错误码语义或改变额度计算属于不兼容变更。
- 不兼容变更必须升级 `/api/vN` 或工具版本，并保留迁移和回滚方案。
- OpenAPI 的 `operationId`、`toolId` 和错误码一旦发布不得复用。

## 8. 接口验收

接口合并前至少验证：

1. 未认证访问受保护接口返回 `401`。
2. 游客调用允许工具成功，并返回剩余次数。
3. 游客调用登录必需工具返回明确的登录错误。
4. 额度达到上限后返回 `429 TOOL_QUOTA_EXCEEDED`。
5. 重复 `clientRequestId` 不重复扣减。
6. 管理员策略修改在工具详情和实际执行中同时生效。
7. 客户端和后台使用的请求/响应字段与 OpenAPI 一致。
