# lnb Admin

基于 [Art Design Pro](https://github.com/Daymychen/art-design-pro) 修改的精简版后台管理系统。

## 原版项目

- 原版仓库：https://github.com/Daymychen/art-design-pro
- 原版文档：https://www.artd.pro/docs
- 原版演示：https://www.artd.pro

## 精简内容

本版本在原版基础上进行了以下精简：

- 菜单只保留：仪表盘工作台、系统管理（用户管理、角色管理）
- 移除登录页滑块验证和角色下拉
- 移除顶部导航栏的快速入口、通知、ArtBot 聊天
- 简化用户菜单，只保留用户名和注销
- 移除工作台底部关于项目栏

其他示例页面已移至 `src/views/_examples/` 文件夹供参考，不会被打包到生产环境。

## 技术栈

- Vue 3 + TypeScript + Vite
- Element Plus + Tailwind CSS
- Pinia + Vue Router

## 快速开始

```bash
# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev

# 生产环境构建
pnpm build
```

## 默认账号

- 用户名：Super
- 密码：123456

## License

[MIT](./LICENSE)
