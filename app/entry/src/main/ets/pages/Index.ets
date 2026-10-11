import { AccountPanel } from '../components/AccountPanel';
import { OnlineToolCatalog, ToolSelectionHandler } from '../components/OnlineToolCatalog';
import { ToolExecutionPanel } from '../components/ToolExecutionPanel';
import { ToolDefinition } from '../foundation/AppContract';
import { HdsTabs, HdsTabsController, HdsTabsFloatingStyle, hdsMaterial } from '@kit.UIDesignKit';

@Entry
@Component
struct Index {
  @State private selectedIndex: number = 0;
  @State private wideLayout: boolean = false;
  @State private selectedTool: ToolDefinition | null = null;
  private tabsController: HdsTabsController = new HdsTabsController();
  private floatingTabsStyle: HdsTabsFloatingStyle = {
    barWidth: {
      smallWidth: 220,
      mediumWidth: 280,
      largeWidth: 320
    },
    barSideMargin: 24,
    barBottomMargin: 28,
    gradientMask: {
      maskColor: '#66F1F3F5',
      maskHeight: 92
    },
    systemMaterialEffect: {
      materialType: hdsMaterial.MaterialType.IMMERSIVE,
      materialLevel: hdsMaterial.MaterialLevel.ADAPTIVE
    },
    barOpacity: 0.96
  };

  private selectTool(tool: ToolDefinition): void {
    this.selectedTool = tool;
  }

  private selectTab(index: number): void {
    this.selectedIndex = index;
    if (!this.wideLayout) {
      this.tabsController.changeIndex(index);
    }
  }

  @Builder
  private homeContent() {
    if (this.selectedTool === null) {
      OnlineToolCatalog({ selection: new ToolSelectionHandler((tool: ToolDefinition) => this.selectTool(tool)) })
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
    HdsTabs({ controller: this.tabsController, index: this.selectedIndex }) {
      TabContent() {
        this.homeContent()
      }
      .tabBar(BottomTabBarStyle.of($r('app.media.ic_home'), $r('app.string.tab_home'))
        .labelStyle({
          selectedColor: $r('app.color.nav_selected'),
          unselectedColor: $r('app.color.nav_unselected')
        })
        .iconStyle({
          selectedColor: $r('app.color.nav_selected'),
          unselectedColor: $r('app.color.nav_unselected')
        }))

      TabContent() {
        AccountPanel()
      }
      .tabBar(BottomTabBarStyle.of($r('app.media.ic_person'), $r('app.string.tab_account'))
        .labelStyle({
          selectedColor: $r('app.color.nav_selected'),
          unselectedColor: $r('app.color.nav_unselected')
        })
        .iconStyle({
          selectedColor: $r('app.color.nav_selected'),
          unselectedColor: $r('app.color.nav_unselected')
        }))
    }
    .barMode(BarMode.Fixed)
    .barOverlap(true)
    .vertical(false)
    .barPosition(BarPosition.End)
    .barFloatingStyle(this.floatingTabsStyle)
    .onChange((index: number) => {
      this.selectedIndex = index;
    })
    .layoutWeight(1)
    .width('100%')
    .height('100%')
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
      this.selectTab(index);
    })
  }
}
