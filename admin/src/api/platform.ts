/**
 * 平台 API 请求封装。
 *
 * 路径相对于 VITE_API_URL（默认 `/api/v1`），字段以 OpenAPI 契约为准。
 * 这里不实现具体工具页面，只提供客户端和管理后台共用的请求边界。
 */
import request from '@/utils/http'
import type {
  ApiResponse,
  AuthData,
  LoginRequest,
  PrincipalData,
  RegisterRequest,
  RefreshRequest,
  ToolDefinition,
  ToolExecuteData,
  ToolExecuteRequest,
  ToolListData,
  ToolListQuery,
  ToolPatchRequest,
  ToolPolicyPatch,
  ToolUsageData,
  ToolUpsertRequest,
  ToolVersionCreateRequest,
  ToolVersionDefinition,
  ToolAccessPolicy,
  ToolAccessPolicyPatch,
  ToolPrincipalType
} from '@/types/api/contract'

const toolPath = (toolId: string) => `/tools/${encodeURIComponent(toolId)}`
const adminToolPath = (toolId: string) => `/admin/tools/${encodeURIComponent(toolId)}`

export function createGuestSession() {
  return request.post<AuthData>({ url: '/auth/guest-session' })
}

export function login(params: LoginRequest) {
  return request.post<AuthData>({ url: '/auth/login', data: params })
}

export function refreshSession(params: RefreshRequest) {
  return request.post<AuthData>({ url: '/auth/refresh', data: params })
}

export function logout() {
  return request.post<void>({ url: '/auth/logout' })
}

export function getCurrentPrincipal() {
  return request.get<PrincipalData>({ url: '/me' })
}

export function listTools(params?: ToolListQuery) {
  return request.get<ToolListData>({ url: '/tools', params })
}

export function getTool(toolId: string) {
  return request.get<ToolDefinition>({ url: toolPath(toolId) })
}

export function executeTool(toolId: string, data: ToolExecuteRequest) {
  return request.post<ToolExecuteData>({ url: `${toolPath(toolId)}/execute`, data })
}

export function adminListTools(params?: ToolListQuery) {
  return request.get<ToolListData>({ url: '/admin/tools', params })
}

export function adminCreateTool(data: ToolUpsertRequest) {
  return request.post<ToolDefinition>({ url: '/admin/tools', data })
}

export function adminUpdateTool(toolId: string, data: ToolPatchRequest) {
  return request.request<ToolDefinition>({
    url: adminToolPath(toolId),
    method: 'PATCH',
    data
  })
}

export function adminUpdateToolPolicy(toolId: string, data: ToolPolicyPatch) {
  return request.request<ToolDefinition>({
    url: `${adminToolPath(toolId)}/policy`,
    method: 'PATCH',
    data
  })
}

export function adminGetToolUsage(toolId: string, params: { from?: string; to?: string } = {}) {
  return request.get<ToolUsageData>({ url: `${adminToolPath(toolId)}/usage`, params })
}

export function adminListToolVersions(toolId: string) {
  return request.get<ToolVersionDefinition[]>({ url: `${adminToolPath(toolId)}/versions` })
}

export function adminCreateToolVersion(toolId: string, data: ToolVersionCreateRequest) {
  return request.post<ToolVersionDefinition>({ url: `${adminToolPath(toolId)}/versions`, data })
}

export function adminTestToolVersion(toolId: string, version: string) {
  return request.post<ToolVersionDefinition>({
    url: `${adminToolPath(toolId)}/versions/${encodeURIComponent(version)}/testing`
  })
}

export function adminPublishToolVersion(
  toolId: string,
  version: string
) {
  return request.post<ToolVersionDefinition>({
    url: `${adminToolPath(toolId)}/versions/${encodeURIComponent(version)}/publish`
  })
}

export function adminRollbackTool(toolId: string, version: string) {
  return request.post<ToolVersionDefinition>({ url: `${adminToolPath(toolId)}/versions/${encodeURIComponent(version)}/rollback` })
}

export function adminGetToolAccessPolicy(toolId: string, principalType: ToolPrincipalType) {
  return request.get<ToolAccessPolicy>({ url: `${adminToolPath(toolId)}/access-policy/${principalType}` })
}

export function adminUpdateToolAccessPolicy(toolId: string, principalType: ToolPrincipalType, data: ToolAccessPolicyPatch) {
  return request.request<ToolAccessPolicy>({
    url: `${adminToolPath(toolId)}/access-policy/${principalType}`,
    method: 'PATCH',
    data
  })
}

export function register(params: RegisterRequest) {
  return request.post<AuthData>({ url: '/auth/register', data: params })
}

export type PlatformResponse<T> = ApiResponse<T>
