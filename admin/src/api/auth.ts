import { getCurrentPrincipal, login as loginWithContract } from '@/api/platform'

/**
 * 登录
 * @param params 登录参数
 * @returns 登录响应
 */
export function fetchLogin(params: Api.Auth.LoginParams) {
  return loginWithContract({
    identifier: params.userName,
    password: params.password
  }).then(({ accessToken, refreshToken }) => ({
    token: accessToken,
    refreshToken: refreshToken || ''
  }))
}

/**
 * 获取用户信息
 * @returns 用户信息
 */
export function fetchGetUserInfo() {
  return getCurrentPrincipal().then((principal) => ({
    userId: principal.userId,
    userName: principal.displayName || principal.principalType,
    email: '',
    buttons: [],
    // 管理后台原有路由使用 R_ADMIN/R_SUPER 标记，后端契约使用 ADMIN principal。
    // 只把真实的 ADMIN 身份映射为 R_ADMIN，不能由前端输入伪造权限。
    roles: principal.principalType === 'ADMIN' ? ['R_ADMIN', ...principal.scopes] : principal.scopes
  }))
}
