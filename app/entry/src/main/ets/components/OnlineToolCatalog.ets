import { ApiClient, ApiClientError } from '../foundation/ApiClient';
import { ToolDefinition } from '../foundation/AppContract';

@Component
export struct OnlineToolCatalog {
  @Prop onToolSelected: (tool: ToolDefinition) => void = (_tool: ToolDefinition) => {};
  @State private tools: ToolDefinition[] = [];
  @State private query: string = '';
  @State private loading: boolean = false;
  @State private hasError: boolean = false;
  @State private message: string = '正在加载工具目录…';
  @State private columns: number = 1;

  aboutToAppear(): void {
    this.loadTools();
  }

  private async loadTools(): Promise<void> {
    this.loading = true;
    this.hasError = false;
    this.message = '正在加载工具目录…';
    try {
      const hostContext = this.getUIContext().getHostContext();
      if (hostContext === undefined) {
        throw new ApiClientError(0, 'CONTEXT_UNAVAILABLE', '应用上下文不可用');
      }
      await ApiClient.initialize(hostContext);
      await ApiClient.ensureGuestSession();
      const result = await ApiClient.listTools();
      this.tools = result.items;
      this.message = result.items.length === 0 ? '当前没有已发布工具' : `已加载 ${result.items.length} 个工具`;
    } catch (error) {
      this.hasError = true;
      this.message = error instanceof ApiClientError ? error.message : '工具目录加载失败';
    } finally {
      this.loading = false;
    }
  }

  private filteredTools(): ToolDefinition[] {
    const value = this.query.trim().toLowerCase();
    if (value.length === 0) {
      return this.tools;
    }
    return this.tools.filter((tool: ToolDefinition) => {
      return tool.displayName.toLowerCase().includes(value)
        || (tool.description ?? '').toLowerCase().includes(value);
    });
  }

  build() {
    GridRow({
      columns: 12,
      breakpoints: { value: ['600vp', '840vp'] }
    }) {
      GridCol({ span: 12 }) {
        Column({ space: 12 }) {
          Row() {
            Column({ space: 4 }) {
              Text('工具发现')
                .fontSize(22)
                .fontWeight(FontWeight.Bold)
              Text(this.message)
                .fontSize(13)
                .fontColor($r('app.color.secondary_text'))
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)

            Button(this.loading ? '加载中…' : '刷新')
              .enabled(!this.loading)
              .onClick(() => {
                this.loadTools();
              })
          }
          .width('100%')
          .padding({ left: 20, right: 20, top: 18 })

          TextInput({ placeholder: '搜索工具', text: this.query })
            .onChange((value: string) => {
              this.query = value;
            })
            .width('calc(100% - 40vp)')
            .height(44)
            .margin({ left: 20, right: 20 })
            .backgroundColor(Color.White)
            .borderRadius(12)

          if (this.hasError && !this.loading) {
            Column({ space: 8 }) {
              Text('工具目录加载失败')
                .fontSize(18)
                .fontWeight(FontWeight.Medium)
              Text(this.message)
                .fontSize(14)
                .fontColor($r('app.color.secondary_text'))
                .textAlign(TextAlign.Center)
            }
            .layoutWeight(1)
            .width('100%')
            .justifyContent(FlexAlign.Center)
          } else if (this.tools.length === 0 && !this.loading) {
            Column({ space: 8 }) {
              Text('暂无已发布工具')
                .fontSize(18)
                .fontWeight(FontWeight.Medium)
              Text('工具由管理后台发布，客户端不会展示本地假数据。')
                .fontSize(14)
                .fontColor($r('app.color.secondary_text'))
            }
            .layoutWeight(1)
            .width('100%')
            .justifyContent(FlexAlign.Center)
          } else if (this.filteredTools().length === 0 && !this.loading) {
            Column({ space: 8 }) {
              Text('没有匹配的工具')
                .fontSize(18)
                .fontWeight(FontWeight.Medium)
              Text('换个关键词试试。')
                .fontSize(14)
                .fontColor($r('app.color.secondary_text'))
            }
            .layoutWeight(1)
            .width('100%')
            .justifyContent(FlexAlign.Center)
          } else {
            Grid() {
              ForEach(this.filteredTools(), (tool: ToolDefinition) => {
                GridItem() {
                  Column({ space: 8 }) {
                    Row() {
                      Text(tool.displayName)
                        .fontSize(18)
                        .fontWeight(FontWeight.Medium)
                        .layoutWeight(1)
                      Text(tool.currentVersion)
                        .fontSize(12)
                        .fontColor($r('app.color.secondary_text'))
                    }
                    Text(tool.description ?? '暂无描述')
                      .fontSize(14)
                      .fontColor('#475467')
                      .maxLines(3)
                    Row({ space: 8 }) {
                      Text(tool.policy.loginRequired ? '需要登录' : '允许游客')
                        .fontSize(12)
                        .fontColor(tool.policy.loginRequired ? '#B54708' : '#027A48')
                      Text(tool.policy.guestLimit === null ? '游客不限额' : `游客 ${tool.policy.guestLimit} 次`)
                        .fontSize(12)
                        .fontColor($r('app.color.secondary_text'))
                    }
                  }
                  .width('100%')
                  .padding(16)
                  .backgroundColor(Color.White)
                  .borderRadius(16)
                  .onClick(() => {
                    this.onToolSelected(tool);
                  })
                }
              }, (tool: ToolDefinition) => tool.toolId)
            }
            .columnsTemplate(this.columns === 1 ? '1fr' : this.columns === 2 ? '1fr 1fr' : '1fr 1fr 1fr')
            .columnsGap(12)
            .rowsGap(12)
            .layoutWeight(1)
            .width('100%')
            .padding({ left: 20, right: 20, bottom: 20 })
          }
        }
        .width('100%')
        .height('100%')
      }
    }
    .width('100%')
    .height('100%')
    .onBreakpointChange((breakpoint: string) => {
      this.columns = breakpoint === 'xxl' ? 3 : breakpoint === 'md' || breakpoint === 'lg' || breakpoint === 'xl' ? 2 : 1;
    })
  }
}
