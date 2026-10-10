/**
 * 前端运行时配置。
 *
 * `VITE_API_URL` 应指向后端的 `/api/v1` 根地址，例如：
 * `http://localhost:8080/api/v1`。
 * 这里只保存公开配置，不放置 Token、密码或其他敏感信息。
 */
export const runtimeConfig = Object.freeze({
  apiBaseUrl: import.meta.env.VITE_API_URL || '/api/v1',
  withCredentials: import.meta.env.VITE_WITH_CREDENTIALS === 'true',
  appVersion: import.meta.env.VITE_VERSION || '0.0.1'
})
