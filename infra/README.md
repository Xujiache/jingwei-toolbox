# 本地基础设施

当前只提供 MySQL 8 开发容器，不代表生产部署方案。

## 启动

```powershell
Copy-Item .env.example .env
docker compose --env-file .env -f docker-compose.yml up -d
```

## 停止

```powershell
docker compose --env-file .env -f docker-compose.yml stop
```

默认数据卷名为 `jingwei-toolbox-mysql-data`。除非明确需要重置本地数据，不要执行带 `-v` 的清理命令。
