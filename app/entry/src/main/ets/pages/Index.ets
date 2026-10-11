import { AccountPanel } from '../components/AccountPanel';
import { OnlineToolCatalog } from '../components/OnlineToolCatalog';
import { ToolExecutionPanel } from '../components/ToolExecutionPanel';
import { ToolDefinition } from '../foundation/AppContract';

@Entry
@Component
struct Index {
  @State private selectedIndex: number = 0;
  @State private wideLayout: boolean = false;
  @State private selectedTool: ToolDefinition | null = null;

  private selectTool(tool: ToolDefinition): void {
    this.selectedTool = tool;
  }

  @Builder
  private homeContent() {
    if (this.selectedTool === null) {
      OnlineToolCatalog({ onToolSelected: (tool: ToolDefinition) => this.selectTool(tool) })
    } else {
      Column({ space: 8 }) {
        Row() {
          Button('返回工具目录')
            .onClick(() => {
              this.selectedTool = null;
            })
        }
        .width('100%')
        .padding({ left: 20, right: 20, top: 12 })
        ToolExecutionPanel({ tool: this.selectedTool as ToolDefinition })
      }
      .width('100%')
      .height('100%')
    }
  }

  build() {
    Column() {
      Row() {
        Text(this.selectedIndex === 0 ? '首页' : '我的')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
        Blank()
        Text('经纬智能工具箱')
          .fontSize(13)
          .fontColor($r('app.color.secondary_text'))
      }
      .width('100%')
      .padding({ left: 20, right: 20, top: 20, bottom: 12 })

      Divider()

      GridRow({
        columns: 12,
        breakpoints: { value: ['600vp', '840vp'] }
      }) {
        GridCol({ span: 12 }) {
          if (this.wideLayout) {
            this.wideContent()
          } else {
            this.phoneContent()
          }
        }
      }
      .layoutWeight(1)
      .width('100%')
      .onBreakpointChange((breakpoint: string) => {
        this.wideLayout = breakpoint === 'md' || breakpoint === 'lg' || breakpoint === 'xl' || breakpoint === 'xxl';
      })
    }
    .width('100%')
    .height('100%')
    .backgroundColor($r('app.color.app_background'))
  }

  @Builder
  private phoneContent() {
    Tabs({ barPosition: BarPosition.End }) {
      TabContent() {
        this.homeContent()
      }
      .tabBar({ icon: $r('app.media.ic_home'), text: $r('app.string.tab_home') })

      TabContent() {
        AccountPanel()
      }
      .tabBar({ icon: $r('app.media.ic_person'), text: $r('app.string.tab_account') })
    }
    .barMode(BarMode.Fixed)
    .onChange((index: number) => {
      this.selectedIndex = index;
    })
    .layoutWeight(1)
    .width('100%')
  }

  @Builder
  private wideContent() {
    Row() {
      Column({ space: 18 }) {
        this.sideNavItem(0, '首页', $r('app.media.ic_home'))
        this.sideNavItem(1, '我的', $r('app.media.ic_person'))
        Blank()
      }
      .width(168)
      .height('100%')
      .padding({ left: 16, right: 16, top: 20, bottom: 20 })
      .backgroundColor(Color.White)

      Divider()
        .vertical(true)

      Column() {
        if (this.selectedIndex === 0) {
          this.homeContent()
        } else {
          AccountPanel()
        }
      }
      .layoutWeight(1)
      .height('100%')
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  private sideNavItem(index: number, title: string, icon: Resource) {
    Row({ space: 12 }) {
      Image(icon)
        .width(24)
        .height(24)
      Text(title)
        .fontSize(16)
        .fontWeight(this.selectedIndex === index ? FontWeight.Medium : FontWeight.Regular)
        .fontColor(this.selectedIndex === index ? '#2563EB' : '#667085')
    }
    .width('100%')
    .padding({ left: 12, right: 12, top: 12, bottom: 12 })
    .backgroundColor(this.selectedIndex === index ? '#EAF2FF' : Color.Transparent)
    .borderRadius(12)
    .onClick(() => {
      this.selectedIndex = index;
    })
  }
}
