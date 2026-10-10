# 后端骨架

## 技术栈

- Java 21
- Spring Boot 4.1.1
- Spring MVC
- Spring JDBC
- MySQL Connector/J
- Flyway（默认关闭，首个业务迁移确认后启用）
- Actuator

## 本地运行

```powershell
Copy-Item .env.example .env
..\infra\scripts\start-mysql.ps1
# Spring Boot 不会自动读取 .env；仅在当前 PowerShell 会话导入本地变量
Get-Content .\.env | ForEach-Object {
  if ($_ -match '^\s*([^#=]+?)\s*=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
  }
}
.\mvnw.cmd spring-boot:run
```

也可以先从根目录启动 `infra/docker-compose.yml`，再运行 Maven Wrapper。

## 配置

环境变量和默认开发连接信息见 `.env.example`。真实密码不得写入 Git；生产环境必须通过部署平台的密钥管理注入。

## 目录约定

```text
src/main/java/com/xujiache/jingweitoolbox/
├─ config/       # Spring 配置
├─ platform/     # 统一响应、异常、请求追踪等平台能力
├─ auth/         # 认证和会话（后续实现）
├─ tool/         # 工具注册、策略和执行编排（后续实现）
└─ JingweiToolboxBackendApplication.java
src/main/resources/
├─ application.yml
└─ db/migration/ # Flyway 迁移
```

当前没有业务 Controller、Service 或数据库表；生成的启动类和测试仅用于确认 Spring Boot 骨架可加载。
