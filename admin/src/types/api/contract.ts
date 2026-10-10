/**
 * 与 docs/api/openapi.json 对齐的前端平台契约。
 *
 * 具体工具的 input/output 由工具版本契约扩展；本文件只描述通用平台结构。
 */

export type PrincipalType = 'GUEST' | 'USER' | 'ADMIN'

export type ToolStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED' | 'DEPRECATED'

export interface ApiResponse<T> {
  code: 'OK'
  message: string
  data: T
  requestId: string
}

export interface ApiErrorResponse {
  code: string
  message: string
  details?: Record<string, unknown>
  requestId: string
}

export interface LoginRequest {
  identifier: string
  password: string
}

export interface RefreshRequest {
  refreshToken: string
}

export interface AuthData {
  accessToken: string
  refreshToken?: string
  tokenType: 'Bearer'
  expiresAt: string
  principalType: PrincipalType
}

export interface PrincipalData {
  principalType: PrincipalType
  userId: string | null
  displayName: string | null
  scopes: string[]
}

export interface ToolPolicy {
  enabled: boolean
  loginRequired: boolean
  guestLimit: number | null
  guestLimitWindowSeconds: number | null
}

export interface ToolDefinition {
  toolId: string
  displayName: string
  description?: string
  status: ToolStatus
  currentVersion: string
  inputSchema?: Record<string, unknown>
  outputSchema?: Record<string, unknown>
  policy: ToolPolicy
}

export interface ToolListData {
  items: ToolDefinition[]
  page: number
  pageSize: number
  total: number
}

export interface ToolListQuery {
  page?: number
  pageSize?: number
  status?: ToolStatus
}

export interface ToolExecuteRequest {
  toolVersion?: string
  input: Record<string, unknown>
  clientRequestId: string
}

export interface UsageData {
  limit: number | null
  used: number
  remaining: number | null
  resetAt: string | null
}

export interface ToolExecuteData {
  toolId: string
  toolVersion: string
  result: Record<string, unknown>
  usage: UsageData
}

export interface ToolUpsertRequest {
  toolId: string
  displayName: string
  description?: string
  currentVersion: string
  status: ToolStatus
  inputSchema?: Record<string, unknown>
  outputSchema?: Record<string, unknown>
}

export interface ToolPatchRequest {
  displayName?: string
  description?: string
  currentVersion?: string
  status?: ToolStatus
  inputSchema?: Record<string, unknown>
  outputSchema?: Record<string, unknown>
  expectedVersion: number
}

export interface ToolPolicyPatch {
  enabled?: boolean
  loginRequired?: boolean
  guestLimit?: number | null
  guestLimitWindowSeconds?: number | null
  expectedVersion: number
}

export interface ToolUsageData {
  toolId: string
  from: string
  to: string
  totalExecutions: number
  guestExecutions?: number
  userExecutions?: number
}
