# DevEco 26 预览器问题与真机验收记录

更新日期：2026-10-11。按用户决定，保留 Windows 预览器问题，后续以 HarmonyOS 真机验收；未修改代理、TUN、防火墙或全局端口配置。

## 已完成

- DevEco Studio 从 `26.0.0.621` 重装到 `26.0.0.851`，卸载、安装进程均返回 `0`；安装程序数字签名有效。
- 项目原有修改保留；28 个签名文件的数量、大小和修改时间在重装前后相同，未读取其内容。
- 修正 OHPM 根清单：移除错误声明在应用依赖中的 Hvigor 工具包，源工程与英文构建副本的 `ohpm install --all` 均成功。
- 恢复英文构建副本的真实搜索输入框和 SVG 导航图标，不使用删减 UI 掩盖预览错误。
- `PreviewBuild` 成功，但不代表预览器已显示页面。
- `assembleHap` 的 `CompileArkTS`、`PackageHap`、`PackingCheck`、`SignHap` 均成功，退出码 `0`。

## 保留的问题

IDE 日志显示 `COMPILE_SUCCEED`，随后 `START_SERVER_FAILED`，并提示：

```text
Node service error detected. Reinstall DevEco Studio to fix the error.
Websocket error. Exception: Connect timed out
```

当时 `127.0.0.1:30000` 存在 Previewer Node 监听，但本地 TCP 连接超时。独立最小 Node 服务在 `30001`、`30002` 同样失败，而仅更换端口后的 `45003` 成功。失败发生在 IDE 连接预览服务阶段，不能据此归因于首页控件或后端请求。

Clash TUN 当时启用，日志能证明其处理过其他回环地址，但尚未做关闭 TUN 的对照，**不能认定 TUN 是根因**。再次重装、删控件或更改全局动态端口范围缺乏证据，不继续尝试。

## 本机证据与回退

- 旧安装完整备份：`D:\DevEco-Reinstall-20261011\old-install-26.0.0.621`
- 新 IDE 日志：`D:\DevEco-Reinstall-20261011\ide-log-851\idea.log`
- 隔离缓存配置：`D:\DevEco-Reinstall-20261011\deveco851.properties`
- 修改前清单与说明备份：`D:\DevEco-Reinstall-20261011\config-before-sync-fix`
- 当前英文构建副本：`C:\Users\Administrator\Desktop\jingwei-toolbox-app`

这些路径仅用于本机排查。原始日志、签名目录和构建产物不得提交到公开仓库。需要回退本次清单修改时使用对应备份逐文件恢复，不覆盖原有业务改动。

## 当前真机交付检查

产物：`C:\Users\Administrator\Desktop\jingwei-toolbox-app\entry\build\default\outputs\default\entry-default-signed.hap`

- 当前联调构建时间：2026-10-11 13:24 UTC。
- 当前联调构建 SHA-256：`156EC5F226DF3FBD4CC8637944ED4BB69C6CD8D9F2F84AD353F6C93FA378A0B2`
- 包名：`com.xujiache.jingweitoolbox`；入口：`EntryAbility`；设备类型：`phone, tablet`。
- HAP 声明最低与目标 API 12。当前登录流程使用 Account Kit `AuthenticationController` + `AuthorizationWithHuaweiIDRequest`（`profile` scope、`serviceauthcode` permission 和 `state` 校验），相关 API 在当前 SDK/API 12 构建中可用；这是 SDK 定义核验，不替代设备实测。
- 清单请求权限仅为 `ohos.permission.INTERNET`。
- ZIP 文件名检查未发现 `.env`、`.p12`、`.pem`、`.key`、`.keystore`；此检查不等于完整内容泄密扫描。
- 保留编译器对 `EntryAbility.ets` 异常处理的警告，不将警告掩盖为零问题。
- 已将该 HAP 安装到 HDC 设备 `3QH0224B22005385`，安装命令返回 `install bundle successfully`。启动命令已发送，但设备屏幕处于锁定状态，HDC 返回 `10106102`，因此尚未完成真机启动和登录验收。
- 临时服务器 `http://149.88.73.215:18080` 的 `/actuator/health` 返回 HTTP 200；游客会话返回 HTTP 200，工具目录返回 HTTP 200（当前数据总数为 0）；华为登录在未配置服务端 Client ID/Secret 时按设计返回 HTTP 503 `SERVICE_UNAVAILABLE`。
- 本次部署使用服务器独立目录和 MySQL 数据库 `jingwei_toolbox`、Redis DB 5；没有覆盖服务器已有的 `yulin-youxian`、Nginx 或其他容器。服务器部署密钥只保存在 root 权限环境文件中，未写入仓库或客户端。

构建命令（在英文构建副本执行）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Huawei\DevEco Studio 26\jbr'
$env:DEVECO_SDK_HOME = 'C:\Program Files\Huawei\DevEco Studio 26\sdk'
& 'C:\Program Files\Huawei\DevEco Studio 26\tools\node\node.exe' `
  'C:\Program Files\Huawei\DevEco Studio 26\tools\hvigor\bin\hvigorw.js' `
  --mode module -p module=entry@default -p product=default `
  -p buildMode=debug assembleHap --no-daemon
```

## 后续真机必须验证

1. 解锁已授权的签名设备，重新启动已安装 HAP，确认系统版本支持所用 API；实际检查首页。
2. 首页展示“首页 / 我的”导航与搜索、刷新，无图片轮播；手机和平板分别检查布局。
3. 当前临时服务器地址为 `http://149.88.73.215:18080/api/v1`，已从开发机验证可达；仍需从真机验证网络可达性。生产使用 HTTPS。
4. 验证游客会话与真实工具目录；断网和后端不可用时应显示错误状态、不崩溃，不以空目录或 Mock 代替业务成功。
5. 验证华为账号授权、服务端换票与创建/复用账号、退出；取消授权与失败后仍可使用游客功能。
6. 签名构建成功不代表设备已在调试 Profile 中获授权，也不代表 AGC 登录能力、服务端凭证或后端联通已验收。

## 代码审计发现的待验证状态风险

本轮没有扩展修改业务代码，以下不能当作已解决：

- `AccountPanel.logout()`：网络失败时 `ApiClient.logout()` 仍会清除本地会话，但页面跳到异常分支，不执行随后身份刷新，界面可能暂时残留已登录状态。
- `Index.phoneContent()`：`Tabs` 未绑定 `selectedIndex`；从宽屏“我的”切换至手机布局时，需要检查选中标签、标题和内容一致性。
- `ApiClient.ensureGuestSession()`：没有合并进行中的创建请求；若两个页面同时初始化，可能发出重复游客会话请求。

这些是源代码审计风险，尚未通过设备复现。后续真机验收必须覆盖失败退出、布局切换、首次初始化并发场景；不能仅以 HAP 构建成功承诺真机不存在问题。
