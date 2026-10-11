import { ApiClientError } from '../foundation/ApiClient';
import { ToolDefinition } from '../foundation/AppContract';
import { ToolExecutionClient } from '../foundation/ToolExecutionClient';

/**
 * Minimal platform execution surface.  A real tool can replace this with a
 * purpose-built page while continuing to call ToolExecutionClient.
 */
@Component
export struct ToolExecutionPanel {
  @Prop tool: ToolDefinition;
  @State private inputText: string = '{}';
  @State private resultText: string = '';
  @State private message: string = '输入 JSON 后执行工具';
  @State private loading: boolean = false;

  private async execute(): Promise<void> {
    if (this.loading) {
      return;
    }
    let input: Record<string, Object>;
    try {
      const parsed = JSON.parse(this.inputText) as Object;
      if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
        this.message = '输入必须是 JSON 对象';
        return;
      }
      input = parsed as Record<string, Object>;
    } catch (_) {
      this.message = '输入不是有效的 JSON';
      return;
    }

    this.loading = true;
    this.resultText = '';
    this.message = '正在执行…';
    try {
      const data = await ToolExecutionClient.execute(this.tool.toolId, input, this.tool.currentVersion);
      this.resultText = JSON.stringify(data.result);
      const remaining = data.usage.remaining === null ? '不限' : `${data.usage.remaining}`;
      this.message = `执行成功，剩余次数：${remaining}`;
    } catch (error) {
      this.message = ToolExecutionClient.errorMessage(error as Error);
      if (error instanceof ApiClientError && error.errorCode === 'UNAUTHENTICATED') {
        this.resultText = '请前往“我的”完成华为账号登录后重试。';
      }
    } finally {
      this.loading = false;
    }
  }

  build() {
    Column({ space: 14 }) {
      Row({ space: 12 }) {
        Column({ space: 4 }) {
          Text(this.tool.displayName)
            .fontSize(22)
            .fontWeight(FontWeight.Bold)
          Text(`${this.tool.toolId} · ${this.tool.currentVersion}`)
            .fontSize(12)
            .fontColor($r('app.color.secondary_text'))
        }
        .alignItems(HorizontalAlign.Start)
      }
      .width('100%')

      Text(this.tool.description ?? '暂无描述')
        .fontSize(14)
        .fontColor('#475467')
        .width('100%')

      Text('工具输入（JSON）')
        .fontSize(15)
        .fontWeight(FontWeight.Medium)
        .width('100%')
      TextInput({ placeholder: '{"key":"value"}', text: this.inputText })
        .width('100%')
        .height(100)
        .textAlign(TextAlign.Start)
        .backgroundColor(Color.White)
        .borderRadius(12)
        .onChange((value: string) => {
          this.inputText = value;
        })

      Button(this.loading ? '执行中…' : '执行工具')
        .width('100%')
        .height(46)
        .enabled(!this.loading)
        .onClick(() => {
          this.execute();
        })

      Text(this.message)
        .fontSize(14)
        .fontColor($r('app.color.secondary_text'))
        .width('100%')
      if (this.resultText.length > 0) {
        Text(this.resultText)
          .fontSize(13)
          .fontColor('#1D2939')
          .width('100%')
          .padding(12)
          .backgroundColor(Color.White)
          .borderRadius(12)
      }
    }
    .width('100%')
    .height('100%')
    .padding({ left: 20, right: 20, top: 20, bottom: 32 })
    .backgroundColor($r('app.color.app_background'))
  }
}
