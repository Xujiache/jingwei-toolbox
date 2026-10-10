/**
 * 示例参考路由 - 结果页
 * 此文件仅供参考，不在菜单中显示
 */
import { AppRouteRecord } from '@/types/router'

export const resultRoutes: AppRouteRecord = {
  path: '/result',
  name: 'Result',
  component: '/index/index',
  meta: {
    title: 'menus.result.title',
    icon: 'ri:checkbox-circle-line'
  },
  children: [
    {
      path: 'success',
      name: 'ResultSuccess',
      component: '/result/success',
      meta: {
        title: 'menus.result.success',
        icon: 'ri:checkbox-circle-line',
        keepAlive: true
      }
    },
    {
      path: 'fail',
      name: 'ResultFail',
      component: '/result/fail',
      meta: {
        title: 'menus.result.fail',
        icon: 'ri:close-circle-line',
        keepAlive: true
      }
    }
  ]
}
