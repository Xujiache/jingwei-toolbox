# 经纬智能工具箱

面向 HarmonyOS 手机和平板的可扩展工具箱。

## 当前阶段

项目已完成平台接口接入，当前交付的是**平台能力可运行版本**，并包含 `text-echo@1.0.0` 编译型示例工具；后续业务工具按同一契约逐项接入：

- `app/`：HarmonyOS Stage 模型客户端骨架
- 独立后端仓库 `jingwei-toolbox-backend/`：Java 21 + Spring Boot 4.1.1，统一会话、版本化工具执行、Schema、权益额度、幂等和审计
- `admin/`：Vue 3 管理后台（登录、令牌刷新、工具草稿、Schema、策略和版本发布/回滚）
- `docs/`：项目规划、接口契约、前端联调和工具契约
- `infra/`：本地 MySQL 8 开发环境
- `scripts/`：契约和基础校验脚本

## 开发环境

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper（位于独立后端仓库 `jingwei-toolbox-backend/mvnw.cmd`）
- Node.js 20+、pnpm 8+
- DevEco Studio（HarmonyOS 客户端构建必须）
- Docker Desktop（可选，用于启动本地 MySQL/Redis；无容器或数据库凭据时不能进行真实后端业务验收）

## 快速启动

### 1. 本地 MySQL

```powershell
Copy-Item .\infra\.env.example .\infra\.env
docker compose --env-file .\infra\.env -f .\infra\docker-compose.yml up -d
```

### 2. Spring Boot 后端

```powershell
Set-Location ..\jingwei-toolbox-backend
Copy-Item .\.env.example .\.env
# Spring Boot 不会自动读取 .env；仅在当前 PowerShell 会话导入本地变量
Get-Content .\.env | ForEach-Object {
  if ($_ -match '^\s*([^#=]+?)\s*=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
  }
}
.\mvnw.cmd spring-boot:run
```

后端默认端口：`8080`。健康检查：`http://localhost:8080/actuator/health`。

### 3. 管理后台

```powershell
pnpm --dir .\admin install --frozen-lockfile
pnpm --dir .\admin dev
```

先按 `admin/.env.example` 配置 `VITE_API_URL=http://localhost:8080/api/v1`，不要把 Token 或密码写入环境示例文件。

### 4. HarmonyOS 客户端

使用 DevEco Studio 打开 `app/`，安装依赖后选择 `entry` 模块构建。Hvigor 不接受包含中文的工程路径，命令行构建请按 `app/README.md` 使用 ASCII 路径派生副本；当前已用本机 DevEco Hvigor 完成 `assembleHap` 编译验收，签名和真机网络仍需设备环境验证。

## 校验

```powershell
python ..\jingwei-toolbox-backend\scripts\verify-contract.py
Set-Location ..\jingwei-toolbox-backend; .\mvnw.cmd -B test
pnpm --dir .\admin build
```

接口唯一基线为 `docs/api/openapi.json`。具体工具业务必须先更新 `docs/tools/{toolId}` 契约，再实现同版本编译型 Java 执行器、HarmonyOS 页面和管理后台配置。独立后端仓库有自己的 CI（OpenAPI + Maven tests）；根仓库 CI 负责客户端、后台和契约基线。
