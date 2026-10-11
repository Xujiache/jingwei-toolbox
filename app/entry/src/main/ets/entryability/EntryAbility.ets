import { UIAbility } from '@kit.AbilityKit';
import { window } from '@kit.ArkUI';
import { ApiClient } from '../foundation/ApiClient';

export default class EntryAbility extends UIAbility {
  onWindowStageCreate(windowStage: window.WindowStage): void {
    // Initialize the app-private session store with the UIAbility context
    // before components start their independent API requests.  Component
    // host contexts can be unavailable during first render; the ability
    // context is the stable application context required by ArkData.
    ApiClient.initialize(this.context).finally(() => {
      windowStage.loadContent('pages/Index');
    });
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
