@Entry
@Component
struct Index {
  build() {
    Column() {
      Text($r('app.string.app_name'))
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
      Text($r('app.string.app_placeholder'))
        .fontSize(16)
        .margin({ top: 12 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
