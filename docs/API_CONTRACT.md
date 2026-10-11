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
| 分页 | `page` 从 1 开始，`pageSize` 为 1–100 |

## 3. 认证和游客会话

- 未登录用户通过 `POST /api/v1/auth/guest-session` 获得短期游客访问令牌。
- 用户通过 `POST /api/v1/auth/register` 公开注册；注册成功后直接返回登录态。
- 管理后台/旧本地账号仍可通过 `POST /api/v1/auth/login` 登录；HarmonyOS 客户端不再提交账号密码。
- HarmonyOS 客户端通过 Account Kit 取得一次性 `authorizationCode`，调用 `POST /api/v1/auth/huawei` 获得访问令牌和刷新令牌。服务端以 `unionID` 为主标识、`openID` 为同一应用回退标识，华为账号不设置本地密码。
- 客户端和后台都不得自行判断“是否需要登录”后绕过服务端；最终判断由后端工具策略执行。
- 游客令牌只能访问公开工具和策略允许的操作，不能访问后台接口。
- 注销必须使当前会话失效；刷新令牌必须支持撤销和过期检查。
- 访问令牌默认 15 分钟、刷新令牌默认 30 天；实际值由环境变量配置。

### 华为登录时序

```text
HarmonyOS Account Kit
        │ Account Kit profile + serviceauthcode 授权
        │ 一次性 authorizationCode
        ▼
客户端 POST /api/v1/auth/huawei
        ▼
后端使用部署环境 Client ID + Client Secret 调用华为 Token API
        ▼
后端使用短期 Access Token 调用 UserInfo API
        ▼
以 unionID 为主键、openID 为回退键查找或创建 user_identities
        ▼
平台 AuthService 签发本系统 accessToken/refreshToken
        ▼
客户端只保存本系统会话，展示 /api/v1/me 返回的昵称和头像
```

授权码、华为 Access Token/Refresh Token 只在本次请求内存中使用，不写入日志或数据库；游客会话不会自动绑定到新建的华为账号。

华为登录成功后，服务端读取官方 `displayName` 和 `headPictureURL`。头像仅接受 HTTPS 华为域名，下载后保存到 `HUAWEI_AVATAR_STORAGE_DIR`，并通过以下公开资源地址返回：

```http
GET /api/v1/media/avatars/{storageKey}
```

头像下载失败不阻断登录；`/api/v1/me` 的 `avatarUri` 为空时客户端显示默认头像。头像仅允许 JPEG、PNG、GIF 或 WebP，默认最大 2 MiB。服务端不会记录授权码、上游 Token 或头像原始 URL。

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

- `guestLimit=null` 表示该工具游客不限额；每个工具的 `guestLimit` 和窗口都可由管理员实时调整。
- 第一阶段不伪造工具结果。未注册具体 `ToolExecutor` 的工具执行返回 `501 TOOL_NOT_IMPLEMENTED`，不会扣减额度。

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
| 409 | `TOOL_CONCURRENT_EXECUTION` | 当前主体已有同一工具执行任务 |
| 429 | `TOOL_QUOTA_EXCEEDED` | 游客或用户额度耗尽 |
| 500 | `INTERNAL_ERROR` | 未预期服务端错误 |
| 502 | `DEPENDENCY_ERROR` | 外部依赖失败 |
| 503 | `SERVICE_UNAVAILABLE` | 服务暂不可用 |
| 501 | `TOOL_NOT_IMPLEMENTED` | 工具元数据已存在但执行器尚未接入 |

华为登录还使用 `HUAWEI_IDENTITY_CONFLICT`（409）表示同一华为身份与多条内部映射冲突。上游超时、5xx 或缺少 Client Secret 统一返回 503，响应不得包含 Client Secret、授权码、Access Token 或上游原文。

## 6. 后台策略修改

后台修改工具策略必须：

- 要求管理员认证和明确权限。
- 使用 `PATCH /api/v1/admin/tools/{toolId}/policy`。
- 支持并发版本字段，避免覆盖其他管理员的更新。
- 写入审计日志，记录操作者、旧值、新值和请求 ID。
- 修改成功后，客户端下一次读取工具详情即可看到新策略；不依赖重启客户端生效。

工具主体策略还可通过 `PATCH /api/v1/admin/tools/{toolId}/access-policy/{principalType}` 独立修改，`principalType` 取 `GUEST`、`USER`、`MEMBER` 或 `AD_REWARDED`，请求带策略行的 `expectedVersion`。会员和广告权益目前只保存服务端策略模型，客户端不能自行声明获得权益。
将 `clearQuota=true` 可明确清除该主体的额度限制；省略该字段时，未提供的额度字段保持原值。

## 7. 工具版本生命周期

管理员通过以下接口维护版本：

- `GET/POST /api/v1/admin/tools/{toolId}/versions`
- `POST /api/v1/admin/tools/{toolId}/versions/{version}/testing`
- `POST /api/v1/admin/tools/{toolId}/versions/{version}/publish`
- `POST /api/v1/admin/tools/{toolId}/versions/{version}/rollback`

版本状态只能按 `DRAFT → TESTING → PUBLISHED` 推进；已发布版本可回滚为稳定版本，历史记录保留为 `DEPRECATED`。未发布版本执行接口返回 `TOOL_VERSION_NOT_FOUND`，客户端省略 `toolVersion` 时使用稳定版本指针。发布前必须有同版本编译型 `ToolExecutor` Bean，平台不执行上传脚本。

## 8. 兼容性规则

- 新增可选字段属于兼容变更。
- 删除字段、改变字段类型、改变必填性、改变错误码语义或改变额度计算属于不兼容变更。
- 不兼容变更必须升级 `/api/vN` 或工具版本，并保留迁移和回滚方案。
- OpenAPI 的 `operationId`、`toolId` 和错误码一旦发布不得复用。

## 9. 接口验收

接口合并前至少验证：

1. 未认证访问受保护接口返回 `401`。
2. 游客调用允许工具成功，并返回剩余次数。
3. 游客调用登录必需工具返回明确的登录错误。
4. 额度达到上限后返回 `429 TOOL_QUOTA_EXCEEDED`。
5. 重复 `clientRequestId` 不重复扣减。
6. 管理员策略修改在工具详情和实际执行中同时生效。
7. 客户端和后台使用的请求/响应字段与 OpenAPI 一致。

## 10. 运行依赖

- MySQL 8：保存用户、刷新会话、工具元数据、幂等记录、使用量和审计日志；启动时由 Flyway 执行迁移。
- Redis 7：保存访问令牌撤销标记和游客工具窗口额度；Redis 不可用时相关请求 fail-closed 返回 `503`。
- 具体工具以实现 `ToolExecutor` 接口的后端模块注册，平台层不动态执行上传脚本。

## 11. 华为配置和迁移回滚

部署环境需要注入以下变量（真实值不写入仓库）：

- `HUAWEI_ACCOUNT_CLIENT_ID`：AGC 中当前包名应用的公开 Client ID。
- `HUAWEI_ACCOUNT_CLIENT_SECRET`：服务端密钥，使用部署密钥管理器注入。
- `HUAWEI_ACCOUNT_TOKEN_URL`、`HUAWEI_ACCOUNT_USER_INFO_URL`：华为 OAuth 地址，默认值见 `application.yml`。
- `HUAWEI_ACCOUNT_REDIRECT_URI`：按 AGC 配置填写；无值时不发送该字段。
- `HUAWEI_ACCOUNT_TIMEOUT`：上游请求超时，默认 `5s`。
- `APP_PUBLIC_BASE_URL`：应用对外访问基址（只填 origin，例如 `https://api.example.com`，不要包含 `/api/v1`）；设置后 `/api/v1/me.avatarUri` 返回绝对 URL，未设置时返回 API 相对路径。
- `HUAWEI_AVATAR_STORAGE_DIR`：头像本地存储目录，默认 `./data/avatars`。
- `HUAWEI_AVATAR_MAX_BYTES`：头像最大字节数，默认 `2097152`。
- `HUAWEI_AVATAR_ALLOWED_HOSTS`：允许下载头像的主机后缀，默认 `account.cloud.huawei.com,.huawei.com,.hicloud.com`。

客户端 `entry/src/main/module.json5` 只放 App ID 和公开 Client ID 占位符，Client Secret 不得进入 HAP。

`V2__huawei_identity.sql` 只做向前兼容的加列和新表创建，保留旧管理员/本地账号。Flyway 不执行自动 down migration；发布前应备份 MySQL，若需回滚先停用华为登录并回滚应用，再由 DBA 根据备份恢复新增 schema，确认没有依赖华为用户映射后才能清理 `user_identities` 和 `users.auth_provider`。

`V3__huawei_avatar_storage.sql` 为身份映射增加头像对象键、媒体类型、摘要和更新时间；回滚时先停止华为登录并保留头像目录与数据库列，确认无客户端仍在使用后再由 DBA 手工清理。

`V4__tool_platform.sql` 创建工具版本、发布指针、主体访问策略和权益表，并从现有 `tools` 数据初始化兼容记录；`V5__seed_text_echo_tool.sql` 注册 `text-echo@1.0.0` 示例工具。上线前备份 MySQL，按 Flyway 顺序向前迁移；回滚应用时保留 V4/V5 表和历史版本，不执行手工删表。
