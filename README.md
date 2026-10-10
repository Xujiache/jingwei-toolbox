# 经纬智能工具箱

面向 HarmonyOS 手机和平板的可扩展工具箱。

## 当前阶段

项目已经完成基础工程初始化，当前交付的是**可启动空壳**，不包含具体工具业务：

- `app/`：HarmonyOS Stage 模型客户端骨架
- `backend/`：Java 21 + Spring Boot 4.1.1 后端骨架
- `admin/`：Vue 3 管理后台模板
- `docs/`：项目规划、接口契约和两人协作规则
- `infra/`：本地 MySQL 8 开发环境
- `scripts/`：契约和基础校验脚本

## 开发环境

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper（位于 `backend/mvnw.cmd`）
- Node.js 20+、pnpm 8+
- DevEco Studio（HarmonyOS 客户端构建必须）
- Docker Desktop（可选，用于启动本地 MySQL）

## 快速启动

### 1. 本地 MySQL

```powershell
Copy-Item .\infra\.env.example .\infra\.env
docker compose --env-file .\infra\.env -f .\infra\docker-compose.yml up -d
```

### 2. Spring Boot 后端

```powershell
Copy-Item .\backend\.env.example .\backend\.env
# Spring Boot 不会自动读取 .env；仅在当前 PowerShell 会话导入本地变量
Get-Content .\backend\.env | ForEach-Object {
  if ($_ -match '^\s*([^#=]+?)\s*=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
  }
}
.\backend\mvnw.cmd -f .\backend\pom.xml spring-boot:run
```

后端默认端口：`8080`。健康检查：`http://localhost:8080/actuator/health`。

### 3. 管理后台

```powershell
pnpm --dir .\admin install --frozen-lockfile
pnpm --dir .\admin dev
```

### 4. HarmonyOS 客户端

使用 DevEco Studio 打开 `app/`，安装依赖后选择 `entry` 模块构建。当前机器未检测到 DevEco/hdc，因此客户端构建需要在安装了对应工具链的环境验证。

## 校验

```powershell
python .\scripts\verify-contract.py
.\backend\mvnw.cmd -f .\backend\pom.xml test
pnpm --dir .\admin build
```

接口唯一基线为 `docs/api/openapi.json`。具体工具业务必须先更新契约，再实现后端、HarmonyOS 客户端和管理后台。
