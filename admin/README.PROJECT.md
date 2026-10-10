# 管理后台项目接入说明

当前目录来自 Vue 3 管理后台模板，已完成项目名称和包名初始化，但模板中的示例 API、示例页面和示例权限不能视为经纬智能工具箱业务。

## 环境变量

```powershell
Copy-Item .env.example .env
```

- `VITE_API_URL`：后端 `/api/v1` 根地址
- `VITE_WITH_CREDENTIALS`：是否发送 Cookie，默认 `false`

## 接口接入规则

- 业务 API 必须以根目录 `docs/api/openapi.json` 为准。
- 模板现有示例请求在业务接口接入前必须逐个迁移，不能直接把示例路径提交为生产接口。
- 认证头、响应字段和错误码必须与 `docs/API_CONTRACT.md` 一致。
