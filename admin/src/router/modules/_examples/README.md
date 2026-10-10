# 示例参考路由

此文件夹包含从主菜单中移除的路由配置，仅供开发参考。

## 包含的路由模块

- `template.ts` - 模板页面（卡片、横幅、图表、地图、聊天、日历、定价）
- `widgets.ts` - 小组件（图标、图片裁剪、Excel、视频、数字动画、富文本等）
- `examples.ts` - 功能示例（权限、标签页、表格、表单、WebSocket）
- `article.ts` - 文章管理（列表、详情、评论、发布）
- `result.ts` - 结果页（成功、失败）
- `exception.ts` - 异常页（403、404、500）
- `safeguard.ts` - 运维管理（服务器监控）
- `help.ts` - 帮助链接（文档、版本、更新日志）

## 如何启用

如需启用某个路由模块，在 `src/router/modules/index.ts` 中导入并添加到 `routeModules` 数组即可。

```typescript
import { templateRoutes } from './_examples/template'

export const routeModules: AppRouteRecord[] = [
  dashboardRoutes,
  systemRoutes,
  templateRoutes // 添加需要的路由
]
```
