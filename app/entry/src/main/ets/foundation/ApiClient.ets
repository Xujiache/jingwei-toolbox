import { http } from '@kit.NetworkKit';
import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import {
  API_BASE_URL,
  API_PATHS,
  ApiRequestBody,
  ApiResponse,
  AuthData,
  PrincipalData,
  PrincipalType,
  ToolListData,
  ToolExecuteData,
  ToolExecuteRequest
} from './AppContract';

export class ApiClientError extends Error {
  public readonly statusCode: number;
  public readonly errorCode: string;
  public readonly requestId: string;

  constructor(statusCode: number, errorCode: string, message: string, requestId: string = '') {
    super(message);
    this.name = 'ApiClientError';
    this.statusCode = statusCode;
    this.errorCode = errorCode;
    this.requestId = requestId;
  }
}

/**
 * HarmonyOS 客户端与独立 Spring Boot 后端的最小真实请求边界。
 * 令牌保存在应用私有 Preferences 中，不写入日志或其他可导出文件。
 */
export class ApiClient {
  private static accessToken: string = '';
  private static refreshToken: string = '';
  private static principalType: PrincipalType = PrincipalType.GUEST;
  private static sessionStore: preferences.Preferences | null = null;
  private static sessionStorePromise: Promise<void> | null = null;

  /**
   * Opens the app-private ArkData Preferences store and restores the last
   * session. Loading is deliberately lazy so the API client does not need to
   * depend on a global singleton or retain an ability context longer than
   * necessary.
   */
  static async initialize(context: common.Context): Promise<void> {
    if (ApiClient.sessionStore !== null) {
      return;
    }
    if (ApiClient.sessionStorePromise !== null) {
      await ApiClient.sessionStorePromise;
      return;
    }

    ApiClient.sessionStorePromise = (async (): Promise<void> => {
      try {
        const store = await preferences.getPreferences(context, { name: 'jingwei-session' });
        ApiClient.sessionStore = store;
        const accessValue = await store.get('accessToken', '');
        const refreshValue = await store.get('refreshToken', '');
        const principalValue = await store.get('principalType', PrincipalType.GUEST);
        ApiClient.accessToken = typeof accessValue === 'string' ? accessValue : '';
        ApiClient.refreshToken = typeof refreshValue === 'string' ? refreshValue : '';
        ApiClient.principalType = typeof principalValue === 'string'
          ? ApiClient.toPrincipalType(principalValue)
          : PrincipalType.GUEST;
      } catch (_) {
        // A storage failure must not prevent guest use. The in-memory session
        // remains empty and the next request will create a fresh guest session.
        ApiClient.sessionStore = null;
      } finally {
        ApiClient.sessionStorePromise = null;
      }
    })();
    await ApiClient.sessionStorePromise;
  }

  static async ensureGuestSession(): Promise<AuthData> {
    await ApiClient.ensureStorageReady();
    if (ApiClient.accessToken.length > 0) {
      return {
        accessToken: ApiClient.accessToken,
        refreshToken: ApiClient.refreshToken,
        tokenType: 'Bearer',
        expiresAt: '',
        principalType: ApiClient.principalType
      };
    }
    const response = await ApiClient.request<AuthData>(API_PATHS.guestSession, http.RequestMethod.POST);
    await ApiClient.saveSession(response.data);
    return response.data;
  }

  static async loginWithHuaweiAuthorizationCode(authorizationCode: string): Promise<AuthData> {
    const request: ApiRequestBody = { authorizationCode };
    const response = await ApiClient.request<AuthData>(API_PATHS.huaweiLogin, http.RequestMethod.POST, request);
    await ApiClient.saveSession(response.data);
    return response.data;
  }

  static async refresh(): Promise<AuthData> {
    if (ApiClient.refreshToken.length === 0) {
      throw new ApiClientError(401, 'UNAUTHENTICATED', '刷新令牌不存在');
    }
    const response = await ApiClient.request<AuthData>(API_PATHS.refresh, http.RequestMethod.POST, {
      refreshToken: ApiClient.refreshToken
    }, false);
    await ApiClient.saveSession(response.data);
    return response.data;
  }

  static async logout(): Promise<void> {
    if (ApiClient.accessToken.length > 0) {
      try {
        await ApiClient.request<void>(API_PATHS.logout, http.RequestMethod.POST, undefined, false);
      } finally {
        await ApiClient.clearSession();
      }
    } else {
      await ApiClient.clearSession();
    }
  }

  static async currentPrincipal(): Promise<PrincipalData> {
    const response = await ApiClient.request<PrincipalData>(API_PATHS.currentPrincipal, http.RequestMethod.GET);
    return response.data;
  }

  /**
   * Converts the server-owned avatar resource to an Image-compatible URI.
   * Deployments should set APP_PUBLIC_BASE_URL so the API returns an absolute
   * URL; the relative-path fallback keeps local/LAN development working when
   * that setting is intentionally omitted.
   */
  static resolveResourceUri(uri: string): string {
    if (uri.length === 0 || uri.startsWith('http://') || uri.startsWith('https://')) {
      return uri;
    }
    const apiMarker = '/api/v1';
    const markerIndex = API_BASE_URL.indexOf(apiMarker);
    if (markerIndex <= 0) {
      return uri;
    }
    return API_BASE_URL.substring(0, markerIndex) + (uri.startsWith('/') ? uri : `/${uri}`);
  }

  static async listTools(page: number = 1, pageSize: number = 20): Promise<ToolListData> {
    const query = `?page=${page}&pageSize=${pageSize}&status=PUBLISHED`;
    const response = await ApiClient.request<ToolListData>(`${API_PATHS.tools}${query}`, http.RequestMethod.GET);
    return response.data;
  }

  /**
   * Executes a published synchronous JSON tool through the platform boundary.
   * The server remains authoritative for authentication, schema validation,
   * quota consumption and idempotency; the client only supplies the input and
   * request key generated by ToolExecutionClient.
   */
  static async executeTool(toolId: string, request: ToolExecuteRequest): Promise<ToolExecuteData> {
    if (toolId.length === 0) {
      throw new ApiClientError(400, 'TOOL_NOT_FOUND', '工具标识不能为空');
    }
    const path = `${API_PATHS.tools}/${encodeURIComponent(toolId)}/execute`;
    const response = await ApiClient.request<ToolExecuteData>(path, http.RequestMethod.POST, {
      toolVersion: request.toolVersion,
      input: request.input,
      clientRequestId: request.clientRequestId
    });
    return response.data;
  }

  static get currentPrincipalType(): string {
    return ApiClient.principalType;
  }

  static get hasSession(): boolean {
    return ApiClient.accessToken.length > 0;
  }

  private static async saveSession(auth: AuthData): Promise<void> {
    ApiClient.accessToken = auth.accessToken;
    ApiClient.refreshToken = auth.refreshToken ?? '';
    ApiClient.principalType = auth.principalType;
    const store = ApiClient.sessionStore;
    if (store === null) {
      return;
    }
    try {
      await store.put('accessToken', ApiClient.accessToken);
      await store.put('refreshToken', ApiClient.refreshToken);
      await store.put('principalType', ApiClient.principalType);
      await store.flush();
    } catch (_) {
      // Keep this launch usable even if persistence is temporarily unavailable.
    }
  }

  private static async clearSession(): Promise<void> {
    ApiClient.accessToken = '';
    ApiClient.refreshToken = '';
    ApiClient.principalType = PrincipalType.GUEST;
    const store = ApiClient.sessionStore;
    if (store === null) {
      return;
    }
    try {
      await store.delete('accessToken');
      await store.delete('refreshToken');
      await store.delete('principalType');
      await store.flush();
    } catch (_) {
      // The in-memory session is cleared even when the disk flush fails.
    }
  }

  private static async ensureStorageReady(): Promise<void> {
    if (ApiClient.sessionStore !== null) {
      return;
    }
    if (ApiClient.sessionStorePromise !== null) {
      await ApiClient.sessionStorePromise;
    }
  }

  private static toPrincipalType(value: string): PrincipalType {
    if (value === PrincipalType.USER) {
      return PrincipalType.USER;
    }
    if (value === PrincipalType.ADMIN) {
      return PrincipalType.ADMIN;
    }
    return PrincipalType.GUEST;
  }

  private static async request<T>(
    path: string,
    method: http.RequestMethod,
    body?: ApiRequestBody,
    allowRefresh: boolean = true
  ): Promise<ApiResponse<T>> {
    const request = http.createHttp();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };
    if (ApiClient.accessToken.length > 0) {
      headers.Authorization = `Bearer ${ApiClient.accessToken}`;
    }

    try {
      const response = await request.request(`${API_BASE_URL}${path}`, {
        method,
        header: headers,
        extraData: body === undefined ? undefined : JSON.stringify(body),
        connectTimeout: 10000,
        readTimeout: 15000
      });
      if (response.responseCode === 204) {
        return {
          code: 'OK',
          message: '',
          data: undefined as T,
          requestId: ''
        };
      }
      const payload = JSON.parse(String(response.result)) as ApiResponse<T>;
      if (response.responseCode === 401 && allowRefresh && ApiClient.refreshToken.length > 0) {
        await ApiClient.refresh();
        return ApiClient.request<T>(path, method, body, false);
      }
      if (response.responseCode < 200 || response.responseCode >= 300 || payload.code !== 'OK') {
        throw new ApiClientError(
          response.responseCode,
          payload.code,
          payload.message,
          payload.requestId
        );
      }
      return payload;
    } catch (error) {
      if (error instanceof ApiClientError) {
        throw error;
      }
      throw new ApiClientError(0, 'NETWORK_ERROR', '无法连接服务，请检查后端地址和网络');
    } finally {
      request.destroy();
    }
  }
}
