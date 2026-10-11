import { util } from '@kit.ArkTS';
import { ApiClient, ApiClientError } from './ApiClient';
import { ToolExecuteData } from './AppContract';

/** Stable UI states shared by every tool page. */
export type ToolExecutionStatus = 'IDLE' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export interface ToolExecutionState {
  status: ToolExecutionStatus;
  data: ToolExecuteData | null;
  errorCode: string;
  message: string;
}

/**
 * One client-side entry point for all synchronous JSON tools.  Concrete tool
 * pages remain free to choose their own UI and input model, while auth,
 * request IDs and platform error mapping stay identical.
 */
export class ToolExecutionClient {
  static async execute(
    toolId: string,
    input: Record<string, Object>,
    toolVersion: string = ''
  ): Promise<ToolExecuteData> {
    if (toolId.length === 0) {
      throw new ApiClientError(400, 'TOOL_NOT_FOUND', '工具标识不能为空');
    }
    const requestId = util.generateRandomUUID();
    await ApiClient.ensureGuestSession();
    return ApiClient.executeTool(toolId, {
      toolVersion: toolVersion.length === 0 ? undefined : toolVersion,
      input,
      clientRequestId: requestId
    });
  }

  static errorMessage(error: Error): string {
    if (!(error instanceof ApiClientError)) {
      return '工具执行失败，请稍后重试';
    }
    switch (error.errorCode) {
      case 'UNAUTHENTICATED':
        return '该工具需要登录后使用';
      case 'TOOL_QUOTA_EXCEEDED':
        return '当前额度已用完，请稍后再试';
      case 'TOOL_INPUT_INVALID':
      case 'BAD_REQUEST':
        return error.message || '工具输入不符合要求';
      case 'TOOL_NOT_FOUND':
      case 'TOOL_VERSION_NOT_FOUND':
        return '工具版本不可用，请刷新工具目录';
      case 'DEPENDENCY_ERROR':
      case 'SERVICE_UNAVAILABLE':
        return '工具服务暂不可用，请稍后重试';
      default:
        return error.message || '工具执行失败，请稍后重试';
    }
  }
}
