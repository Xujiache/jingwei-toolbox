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
    roles: principal.scopes
  }))
}
