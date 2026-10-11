/**
 * HarmonyOS 客户端的平台契约边界。
 *
 * 具体工具的 input/output 不在客户端骨架中提前虚构，后续按 OpenAPI 和工具版本契约扩展。
 */
export const API_VERSION: string = 'v1';
/**
 * 当前开发联调服务器地址；该地址不包含凭据。生产构建必须替换为 HTTPS 发布地址。
 */
export const API_BASE_URL: string = 'http://149.88.73.215:18080/api/v1';

export interface ApiPaths {
  guestSession: string;
  login: string;
  huaweiLogin: string;
  refresh: string;
  logout: string;
  currentPrincipal: string;
  tools: string;
  adminTools: string;
}

export const API_PATHS: ApiPaths = {
  guestSession: '/auth/guest-session',
  login: '/auth/login',
  huaweiLogin: '/auth/huawei',
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

export interface ApiRequestBody {
  refreshToken?: string;
  authorizationCode?: string;
  toolVersion?: string;
  input?: Record<string, Object>;
  clientRequestId?: string;
}

export interface HuaweiLoginRequest {
  authorizationCode: string;
}

export interface AuthData {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresAt: string;
  principalType: PrincipalType;
}

export interface PrincipalData {
  principalType: PrincipalType;
  userId: string | null;
  displayName: string | null;
  avatarUri: string | null;
  scopes: string[];
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
  inputSchema?: Record<string, Object>;
  outputSchema?: Record<string, Object>;
  policy: ToolPolicy;
  version: number;
  updatedAt: string;
}

export interface ToolListData {
  items: ToolDefinition[];
  page: number;
  pageSize: number;
  total: number;
}

/**
 * Standard synchronous tool execution request.  A tool page owns the
 * concrete input shape; the platform only carries an object and the
 * idempotency key.
 */
export interface ToolExecuteRequest {
  toolVersion?: string;
  input: Record<string, Object>;
  clientRequestId: string;
}

export interface ToolUsageData {
  limit: number | null;
  used: number;
  remaining: number | null;
  resetAt: string | null;
}

export interface ToolExecuteData {
  toolId: string;
  toolVersion: string;
  result: Record<string, Object>;
  usage: ToolUsageData;
}

export interface ApiClientContract {
  get<T>(path: string): Promise<ApiResponse<T>>;
  post<T>(path: string, body?: ApiRequestBody): Promise<ApiResponse<T>>;
}
