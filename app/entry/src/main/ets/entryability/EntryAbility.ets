import { UIAbility } from '@kit.AbilityKit';
import { window } from '@kit.ArkUI';

export default class EntryAbility extends UIAbility {
  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index');
  }

  onWindowStageDestroy(): void {
    // Framework lifecycle hook. Business cleanup will be added with real features.
  }

  onForeground(): void {
    // Framework lifecycle hook.
  }

  onBackground(): void {
    // Framework lifecycle hook.
  }
}
