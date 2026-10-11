# HarmonyOS 客户端

当前客户端包含首页工具目录和“我的”页：

- 首页直接读取 Spring Boot 后端发布的真实工具目录，支持搜索、刷新、加载/空数据/错误状态和游客策略展示。
- “我的”页保留游客会话，并使用 Account Kit `AuthenticationController` 的授权码流程（`profile` scope + `serviceauthcode` permission）完成华为账号登录/注册和退出。
- 手机使用底部“首页/我的”导航；宽屏设备根据 GridRow 断点切换为侧边导航，工具卡片在平板上使用两列或三列布局。
- 客户端不再包含图片轮播或账号密码表单。

## 打开和构建

使用 DevEco Studio 打开本目录，安装 `oh-package.json5` 依赖后构建 `entry` 模块。

`oh-package.json5` 仅声明 OHPM 应用依赖，不声明 `@ohos/hvigor` 或 `@ohos/hvigor-ohos-plugin`。本工程使用 DevEco Studio 自带的 Hvigor 构建工具链；构建扩展依赖应放入 `hvigor/hvigor-config.json5`，避免 IDE 同步时向 OHPM 仓库错误请求构建工具包。

当前工程使用 HDS Floating Tabs，兼容与目标 API 为 `6.1.0(23)`；该版本是 `HdsTabsFloatingStyle`、`barFloatingStyle` 和系统沉浸式材质参数的最低构建版本。真机系统需要支持 API 23 或更高版本。

仓库已提供 `hvigor/hvigor-config.json5`。如果工作区路径包含中文，DevEco Hvigor 会拒绝构建路径；可将 `app/` 临时复制到只含英文字符的目录后，使用 DevEco 安装目录中的 `hvigorw.bat assembleHap --no-daemon` 构建，产物不回写源码目录。

模块元数据中的 `client_id` 已配置为 AGC 为当前应用分配的公开 OAuth 2.0 Client ID；更换 AGC 应用时需同步替换。Client Secret、数据库密码、JWT Secret 和后端 Token 只放在部署环境。

## 目录

```text
AppScope/                  # 应用级 app.json5
entry/                     # HAP 模块
└─ src/main/
   ├─ ets/entryability/    # 入口 Ability
   ├─ ets/components/      # 首页工具目录和账号组件
   ├─ ets/foundation/      # API 契约和 HTTP 客户端
   ├─ ets/pages/           # 页面
   ├─ resources/           # 字符串、颜色、页面配置
   └─ module.json5
```

## 独立后端联调

- API 根地址配置在 `entry/src/main/ets/foundation/AppContract.ets` 的 `API_BASE_URL`。
- 当前联调值为 `http://149.88.73.215:18080/api/v1`，对应本次临时部署；模拟器或其他环境必须按实际可达地址替换，生产构建必须使用 HTTPS。
- 客户端会真实调用游客会话、华为授权码登录、刷新、注销、当前身份和已发布工具目录接口；服务端返回错误时展示错误消息，不生成本地假工具数据。
- 应用需要 `ohos.permission.INTERNET`，已在 `entry/src/main/module.json5` 声明。

## 华为账号登录配置

HarmonyOS 端只申请 `ohos.permission.INTERNET`。客户端通过 Account Kit `AuthorizationWithHuaweiIDRequest` 请求 `profile`（头像和昵称）及 `serviceauthcode`（一次性授权码），由系统展示官方授权页；客户端只将一次性授权码发送到 `/api/v1/auth/huawei`，不提交或信任设备侧头像、昵称、OpenID、UnionID。后端配置 `HUAWEI_ACCOUNT_CLIENT_ID`、`HUAWEI_ACCOUNT_CLIENT_SECRET` 及可选的 OAuth 地址和超时，详见 [`docs/FRONTEND_INTEGRATION.md`](../docs/FRONTEND_INTEGRATION.md)。服务端确认后的 `avatarUri` 指向应用自己的头像资源接口；若未设置 `APP_PUBLIC_BASE_URL`，客户端会将 API 相对路径解析为可加载的完整地址。头像不可用时显示默认头像。
