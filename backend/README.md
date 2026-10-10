# 经纬智能工具箱后端

Java 21 + Spring Boot 4.1.1 的平台后端：认证会话、工具注册、策略管理与审计。
接口以根目录 `docs/api/openapi.json` 为唯一契约，规范见 `docs/API_CONTRACT.md`。

## 技术栈

- Java 21、Spring Boot 4.1.1（Spring MVC + Spring Security + OAuth2 Resource Server）
- Spring JDBC（JdbcClient/JdbcTemplate，无 JPA）
- Flyway + MySQL 8.4（测试环境使用 H2 MySQL 兼容模式，CI 无需数据库）
- Actuator（health/info）

## 阶段 1 已实现接口

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/api/v1/auth/guest-session` | 无 | 创建游客会话（默认 24h） |
| POST | `/api/v1/auth/login` | 无 | 登录，返回访问/刷新令牌 |
| POST | `/api/v1/auth/refresh` | 无 | 刷新访问令牌（会话撤销后拒绝） |
| POST | `/api/v1/auth/logout` | Bearer | 注销当前会话，返回 204 |
| GET | `/api/v1/me` | Bearer | 当前身份（GUEST/USER/ADMIN） |
| GET/POST | `/api/v1/admin/tools` | ADMIN | 工具列表 / 创建（创建返回 201） |
| PATCH | `/api/v1/admin/tools/{toolId}` | ADMIN | 更新工具（`expectedVersion` 乐观锁） |
| PATCH | `/api/v1/admin/tools/{toolId}/policy` | ADMIN | 更新策略，同事务写审计日志 |
| GET | `/api/v1/admin/tools/{toolId}/usage` | ADMIN | 用量聚合（`from`/`to` 缺省最近 24h） |
| GET | `/api/v1/tools`、`/api/v1/tools/{toolId}` | 无 | 公开目录，仅暴露已发布且启用的工具 |

统一响应封套 `{code:"OK", message, data, requestId}`；错误为
`{code:<错误码>, message, details?, requestId}`，错误码见 `docs/API_CONTRACT.md`。
每个响应带 `X-Request-Id` 头。

**会话撤销实时生效**：注销或刷新令牌后，旧访问令牌立即被拒绝（每个已认证请求都会核对
`auth_sessions` 状态）。游客会话不支持刷新。

**用量口径**：`usage` 接口聚合 `tool_usage_counters`；执行入口（`/execute`）和额度扣减属
阶段 2，当前计数恒为 0。

## 本地运行（无 Docker，便携版 MySQL）

当前仓库默认使用 `infra/docker-compose.yml`（Docker）。若无 Docker，可使用便携版 MySQL：

1. MySQL 8.4 解压到任意目录，`datadir` 初始化后监听 `127.0.0.1:3306`
2. 建库建用户（与 `.env.example` 一致）：

```sql
CREATE DATABASE jingwei_toolbox CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'jingwei'@'localhost' IDENTIFIED BY '<本地密码>';
GRANT ALL PRIVILEGES ON jingwei_toolbox.* TO 'jingwei'@'localhost';
```

3. `Copy-Item .env.example .env` 修改 `DB_PASSWORD`；Flyway 首次运行需 `FLYWAY_ENABLED=true`
4. Spring Boot 不会自动读取 `.env`，启动前把变量导入会话：

```powershell
Get-Content .\.env | ForEach-Object {
  if ($_ -match '^\s*([^#=]+?)\s*=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
  }
}
.\mvnw.cmd spring-boot:run
```

5. 验证：`GET http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`

## 数据库迁移

- `V1__init.sql`：users、auth_sessions、tools、tool_versions、tool_policies、tool_usage_counters、audit_logs
- `V2__seed_admin.sql`：种子管理员 `admin`，**初始密码 `admin-dev-2026`，仅限本地开发，
  任何共享环境必须在首次登录后修改**
- 迁移由 Flyway 管理，禁止手工改生产库；回滚按各迁移文件头部注释逆序执行
- 测试（H2）与生产（MySQL）共用同一份迁移 SQL，改迁移时两个库都要验证

## 认证配置

| 环境变量 | 默认 | 说明 |
|---|---|---|
| `JWT_SECRET` | dev 占位值 | HS256 签名密钥，生产必须 ≥32 字节随机值 |
| `JWT_ACCESS_TTL_MINUTES` | 120 | 访问令牌有效期（分钟） |
| `JWT_REFRESH_TTL_DAYS` | 14 | 刷新令牌有效期（天），同时是会话上限 |
| `GUEST_SESSION_TTL_HOURS` | 24 | 游客会话有效期（小时） |

## 测试与验收

```powershell
.\mvnw test                        # 全部测试，H2 内存库，与 CI 同口径
python ../scripts/verify-contract.py   # OpenAPI 契约校验
```

集成测试覆盖：游客/登录/刷新/注销/撤销、游客访问 admin 被拒、错误密码 401、
工具创建-发布-策略-乐观锁冲突 409、公开目录只暴露 PUBLISHED、用量聚合结构。

## 目录约定

```text
src/main/java/com/xujiache/jingweitoolbox/
├─ common/api/    # 统一响应封套、错误码、全局异常处理
├─ common/web/    # RequestId 过滤器
├─ auth/          # 认证会话（controller/service/repository/security）
├─ tool/          # 工具注册、策略、公开目录
└─ JingweiToolboxBackendApplication.java
src/main/resources/db/migration/   # Flyway 迁移
```

## 阶段 2 待实现

`POST /api/v1/tools/{toolId}/execute` 执行入口、游客/用户额度原子扣减、
`clientRequestId` 幂等与 `429 TOOL_QUOTA_EXCEEDED`。表结构已预留。
