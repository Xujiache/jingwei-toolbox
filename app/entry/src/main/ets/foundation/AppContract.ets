/**
 * HarmonyOS 客户端的平台契约边界。
 *
 * 具体工具的 input/output 不在客户端骨架中提前虚构，后续按 OpenAPI 和工具版本契约扩展。
 */
export const API_VERSION: string = 'v1';
export const API_BASE_URL: string = 'http://localhost:8080/api/v1';

export const API_PATHS = {
  guestSession: '/auth/guest-session',
  login: '/auth/login',
  refresh: '/auth/refresh',
  logout: '/auth/logout',
  currentPrincipal: '/me',
  tools: '/tools',
  adminTools: '/admin/tools'
};

export enum PrincipalType {
  GUEST = 'GUEST',
  USER = 'USER',
  ADMIN = 'ADMIN'
}

export enum ToolStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  DISABLED = 'DISABLED',
  DEPRECATED = 'DEPRECATED'
}

export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  requestId: string;
}

export interface AuthData {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresAt: string;
  principalType: PrincipalType;
}

export interface ToolPolicy {
  enabled: boolean;
  loginRequired: boolean;
  guestLimit: number | null;
  guestLimitWindowSeconds: number | null;
}

export interface ToolDefinition {
  toolId: string;
  displayName: string;
  description?: string;
  status: ToolStatus;
  currentVersion: string;
  policy: ToolPolicy;
}

export interface ApiClientContract {
  get<T>(path: string): Promise<ApiResponse<T>>;
  post<T>(path: string, body?: Record<string, Object>): Promise<ApiResponse<T>>;
}
