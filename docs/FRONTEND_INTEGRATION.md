# 前端与独立后端联调说明

## 运行配置

### Vue 3 管理后台

复制 `admin/.env.example` 为本地环境文件，并将 `VITE_API_URL` 设置为独立 Spring Boot 服务的 `/api/v1` 根地址：

```powershell
pnpm --dir .\admin install
pnpm --dir .\admin dev
```

管理后台不保存后端密码或 Token 到源码。登录后访问令牌由 Pinia 持久化状态保存，401 时使用轮换刷新令牌重试一次；刷新失败会清理会话并回到登录页。
后端 `CORS_ALLOWED_ORIGINS` 必须包含实际管理后台 origin（模板默认端口为 `5173`；如果本地 `.env` 改为其他端口，也要同步加入）。

### HarmonyOS 客户端

编辑 `app/entry/src/main/ets/foundation/AppContract.ets` 的 `API_BASE_URL`：

- 本地模拟器默认使用 `http://10.0.2.2:8080/api/v1`；
- 真机使用开发机局域网地址或 HTTPS 地址；
- 正式发布必须通过发布配置替换，不能把令牌、密码或生产密钥写入源码。

客户端首次打开首页时创建真实游客会话，再请求 `/me` 和 `GET /tools?status=PUBLISHED`。客户端不再提交账号密码；“我的”页使用 Account Kit `AuthenticationController` 创建 `AuthorizationWithHuaweiIDRequest`，请求 `profile` scope（头像和昵称）与 `serviceauthcode` permission（一次性授权码），并校验随机 `state`。客户端只提交授权码到 `/auth/huawei`，服务端使用部署环境中的 Client Secret 换取身份。客户端不读取或信任 `openID/unionID`、头像或昵称，也不保存华为 Access Token。

华为登录已在 AGC/华为开发者控制台配置包名、App ID、公开 OAuth 2.0 Client ID，并写入客户端模块元数据。更换 AGC 应用时需同步替换 Client ID；Client Secret 只配置在后端部署环境，不能写入客户端、仓库或日志。

华为登录时序：Account Kit `AuthenticationController` 展示官方授权页并取得一次性 `authorizationCode` → 客户端只调用 `/auth/huawei` → 后端用部署密钥向华为换取用户身份 → 以 `unionID` 为主、`openID` 回退维护身份映射 → 后端签发本系统令牌 → 客户端调用 `/me` 展示昵称/头像。授权码和华为令牌不落库，游客会话也不自动绑定。

## 已接入接口

| 场景 | 方法 | 路径 |
|---|---|---|
| 游客会话 | POST | `/api/v1/auth/guest-session` |
| 登录 | POST | `/api/v1/auth/login` |
| 华为账号登录/注册 | POST | `/api/v1/auth/huawei` |
| 刷新 | POST | `/api/v1/auth/refresh` |
| 注销 | POST | `/api/v1/auth/logout` |
| 当前身份 | GET | `/api/v1/me` |
| 服务端头像资源 | GET | `/api/v1/media/avatars/{storageKey}` |
| 客户端工具目录 | GET | `/api/v1/tools` |
| 工具执行 | POST | `/api/v1/tools/{toolId}/execute` |
| 后台工具目录 | GET | `/api/v1/admin/tools` |
| 更新工具策略 | PATCH | `/api/v1/admin/tools/{toolId}/policy` |
| 访问主体策略 | PATCH | `/api/v1/admin/tools/{toolId}/access-policy/{principalType}` |
| 工具版本列表 | GET | `/api/v1/admin/tools/{toolId}/versions` |
| 创建工具版本 | POST | `/api/v1/admin/tools/{toolId}/versions` |
| 提交版本测试 | POST | `/api/v1/admin/tools/{toolId}/versions/{version}/testing` |
| 发布版本 | POST | `/api/v1/admin/tools/{toolId}/versions/{version}/publish` |
| 回滚稳定版本 | POST | `/api/v1/admin/tools/{toolId}/versions/{version}/rollback` |
| 工具用量 | GET | `/api/v1/admin/tools/{toolId}/usage` |

响应统一使用 `{ code, message, data, requestId }`。华为授权码无效或过期返回 `401 UNAUTHENTICATED`；华为上游不可用或后端未配置 Client Secret 返回 `503 SERVICE_UNAVAILABLE`，不透传上游响应。策略更新必须带服务端返回的 `version` 作为 `expectedVersion`；收到版本冲突时后台重新读取目录，不覆盖其他管理员的修改。

`/me.displayName` 和 `/me.avatarUri` 只使用服务端确认后的资料。后端从华为官方 `displayName`、`headPictureURL` 获取昵称和头像，头像保存到服务端本地目录后由 `/api/v1/media/avatars/{storageKey}` 提供；头像缺失或下载失败时客户端必须显示默认头像。部署建议设置 `APP_PUBLIC_BASE_URL` 使 `avatarUri` 为绝对 URL；客户端同时兼容 API 相对路径并按 `API_BASE_URL` 解析，不把华为临时头像地址长期写入客户端。

HarmonyOS 工具页面统一通过 `ToolExecutionClient.execute(toolId, input, toolVersion)` 调用，自动建立游客会话、生成 `clientRequestId`、复用令牌刷新并映射平台错误；复杂工具可以用自己的页面，不能绕过该执行入口。当前示例 `text-echo@1.0.0` 使用编译型 Java 执行器并由 Flyway V5 种子发布。

## 验收边界

- 管理后台：`pnpm --dir .\admin build` 必须通过；登录和策略保存需要连接可用的 MySQL/Redis 后端进行真实验收。
- HarmonyOS：在 ASCII 路径的派生副本中执行 DevEco 安装目录的 `hvigorw.bat assembleHap --no-daemon`；需要 DevEco 模拟器/真机安装后再验证网络地址、游客额度、首页搜索、手机底部导航、平板侧边导航和华为授权状态。
- 未注册执行器的工具仍返回 `501 TOOL_NOT_IMPLEMENTED`；`text-echo` 可用于完整执行、Schema、额度和幂等验收。
