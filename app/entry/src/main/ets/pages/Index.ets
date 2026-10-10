import { API_VERSION } from '../foundation/AppContract';

@Entry
@Component
struct Index {
  build() {
    Column() {
      Row() {
        Text($r('app.string.app_name'))
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
        Blank()
        Text(`API ${API_VERSION}`)
          .fontSize(12)
          .fontColor($r('app.color.secondary_text'))
      }
      .width('100%')
      .padding({ left: 20, right: 20, top: 20, bottom: 12 })

      Divider()

      Tabs({ barPosition: BarPosition.End }) {
        TabContent() {
          ToolWorkspace()
        }
        .tabBar($r('app.string.tab_tools'))

        TabContent() {
          AccountWorkspace()
        }
        .tabBar($r('app.string.tab_account'))
      }
      .barMode(BarMode.Fixed)
      .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
    .backgroundColor($r('app.color.app_background'))
  }
}

@Component
struct ToolWorkspace {
  build() {
    Column() {
      Text($r('app.string.tools_title'))
        .fontSize(20)
        .fontWeight(FontWeight.Medium)
      Text($r('app.string.tools_empty_desc'))
        .fontSize(14)
        .fontColor($r('app.color.secondary_text'))
        .margin({ top: 12 })
      Text($r('app.string.foundation_notice'))
        .fontSize(11)
        .fontColor($r('app.color.secondary_text'))
        .margin({ top: 24 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .padding({ left: 24, right: 24 })
  }
}

@Component
struct AccountWorkspace {
  build() {
    Column() {
      Text($r('app.string.account_title'))
        .fontSize(20)
        .fontWeight(FontWeight.Medium)
      Text($r('app.string.account_policy_desc'))
        .fontSize(14)
        .fontColor($r('app.color.secondary_text'))
        .margin({ top: 12 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .padding({ left: 24, right: 24 })
  }
}
