/**
 * 示例参考路由 - 运维管理
 * 此文件仅供参考，不在菜单中显示
 */
import { AppRouteRecord } from '@/types/router'

export const safeguardRoutes: AppRouteRecord = {
  path: '/safeguard',
  name: 'Safeguard',
  component: '/index/index',
  meta: {
    title: 'menus.safeguard.title',
    icon: 'ri:shield-check-line',
    keepAlive: false
  },
  children: [
    {
      path: 'server',
      name: 'SafeguardServer',
      component: '/safeguard/server',
      meta: {
        title: 'menus.safeguard.server',
        icon: 'ri:hard-drive-3-line',
        keepAlive: true
      }
    }
  ]
}
