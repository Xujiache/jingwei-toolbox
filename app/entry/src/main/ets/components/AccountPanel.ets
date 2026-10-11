import { authentication } from '@kit.AccountKit';
import { util } from '@kit.ArkTS';
import { ApiClient, ApiClientError } from '../foundation/ApiClient';
import { PrincipalType } from '../foundation/AppContract';

@Component
export struct AccountPanel {
  @State private principalType: PrincipalType = PrincipalType.GUEST;
  @State private displayName: string = '';
  @State private avatarUri: string = '';
  @State private message: string = '正在创建游客会话…';
  @State private loading: boolean = false;

  aboutToAppear(): void {
    this.loadPrincipal();
  }

  /**
   * Starts the Account Kit authorization-code flow with the profile scope.
   * The authorization response is deliberately reduced to a one-time code;
   * nickname/avatar/IDs returned by the device are never trusted by the app.
   */
  private async authorizeHuawei(): Promise<void> {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.message = '正在打开华为账号授权…';

    try {
      const authRequest = new authentication.HuaweiIDProvider().createAuthorizationWithHuaweiIDRequest();
      // profile is the least-privileged Account Kit scope that includes the
      // Huawei ID nickname and avatar. No phone scope is requested.
      authRequest.scopes = ['profile'];
      authRequest.permissions = ['serviceauthcode'];
      authRequest.forceAuthorization = true;
      authRequest.state = util.generateRandomUUID();

      const controller = new authentication.AuthenticationController(this.getUIContext().getHostContext());
      const response = await controller.executeRequest(authRequest) as authentication.AuthorizationWithHuaweiIDResponse;
      if (response.state !== authRequest.state) {
        this.message = '华为账号授权校验失败，请重试或继续以游客身份使用';
        return;
      }

      const authorizationCode = response.data?.authorizationCode;
      if (authorizationCode === undefined || authorizationCode.length === 0) {
        this.message = '未取得有效的华为授权码，请重试或继续以游客身份使用';
        return;
      }
      await this.loginWithHuawei(authorizationCode);
    } catch (error) {
      // Keep authorization failures on the guest session. Account Kit errors
      // can represent cancellation, a missing Huawei ID, network failure or
      // an unapproved scope; none should crash the page or reveal raw details.
      this.message = '华为账号授权未完成，请重试或继续以游客身份使用';
    } finally {
      this.loading = false;
    }
  }

  private async loadPrincipal(): Promise<void> {
    try {
      const hostContext = this.getUIContext().getHostContext();
      if (hostContext === undefined) {
        throw new ApiClientError(0, 'CONTEXT_UNAVAILABLE', '应用上下文不可用');
      }
      await ApiClient.initialize(hostContext);
      await ApiClient.ensureGuestSession();
      const principal = await ApiClient.currentPrincipal();
      this.principalType = principal.principalType;
      this.displayName = principal.displayName ?? '';
      this.avatarUri = ApiClient.resolveResourceUri(principal.avatarUri ?? '');
      this.message = principal.principalType === PrincipalType.GUEST ? '当前为游客模式' : '华为账号已登录';
    } catch (error) {
      this.message = error instanceof ApiClientError ? error.message : '会话状态加载失败';
    }
  }

  private async loginWithHuawei(authorizationCode: string): Promise<void> {
    this.loading = true;
    this.message = '正在验证华为账号…';
    try {
      const auth = await ApiClient.loginWithHuaweiAuthorizationCode(authorizationCode);
      this.principalType = auth.principalType;
      const principal = await ApiClient.currentPrincipal();
      this.displayName = principal.displayName ?? '';
      this.avatarUri = ApiClient.resolveResourceUri(principal.avatarUri ?? '');
      this.message = '华为账号登录成功';
    } catch (error) {
      this.message = error instanceof ApiClientError ? error.message : '华为账号登录失败，请稍后重试';
    } finally {
      this.loading = false;
    }
  }

  private async logout(): Promise<void> {
    this.loading = true;
    try {
      await ApiClient.logout();
      this.displayName = '';
      this.avatarUri = '';
      await this.loadPrincipal();
    } catch (error) {
      this.message = error instanceof ApiClientError ? error.message : '退出登录失败';
    } finally {
      this.loading = false;
    }
  }

  build() {
    Scroll() {
      Column({ space: 16 }) {
        Column({ space: 6 }) {
          Text('账户状态')
            .fontSize(22)
            .fontWeight(FontWeight.Bold)
          Text(this.message)
            .fontSize(14)
            .fontColor($r('app.color.secondary_text'))
        }
        .alignItems(HorizontalAlign.Start)
        .width('100%')

        Column({ space: 12 }) {
          if (this.principalType === PrincipalType.USER || this.principalType === PrincipalType.ADMIN) {
            if (this.avatarUri.length > 0) {
              Image(this.avatarUri)
                .width(64)
                .height(64)
                .borderRadius(32)
                .objectFit(ImageFit.Cover)
            } else {
              Text((this.displayName || '华').substring(0, 1))
                .fontSize(28)
                .fontColor(Color.White)
                .width(64)
                .height(64)
                .textAlign(TextAlign.Center)
                .backgroundColor('#2563EB')
                .borderRadius(32)
            }
            Text(this.displayName || '华为用户')
              .fontSize(20)
              .fontWeight(FontWeight.Medium)
            Text('已登录华为账号')
              .fontSize(14)
              .fontColor($r('app.color.secondary_text'))
          } else {
            Text('游客模式')
              .fontSize(20)
              .fontWeight(FontWeight.Medium)
            Text('无需登录即可浏览工具目录；需要登录的工具会在使用时提示。')
              .fontSize(14)
              .fontColor($r('app.color.secondary_text'))
              .lineHeight(22)
          }
        }
        .width('100%')
        .padding(20)
        .backgroundColor(Color.White)
        .borderRadius(16)

        if (this.principalType === PrincipalType.GUEST) {
          Column({ space: 10 }) {
            Text('华为账号一键登录/注册')
              .fontSize(17)
              .fontWeight(FontWeight.Medium)
            Text('使用系统官方授权流程，不申请手机号权限。')
              .fontSize(13)
              .fontColor($r('app.color.secondary_text'))
            Button(this.loading ? '正在授权…' : '华为账号一键登录')
              .width('100%')
              .height(48)
              .enabled(!this.loading)
              .fontColor(Color.White)
              .backgroundColor('#C8102E')
              .borderRadius(12)
              .onClick(() => {
                this.authorizeHuawei();
              })
          }
          .width('100%')
          .padding(20)
          .backgroundColor(Color.White)
          .borderRadius(16)
        } else {
          Button(this.loading ? '退出中…' : '退出登录')
            .width('100%')
            .enabled(!this.loading)
            .onClick(() => {
              this.logout();
            })
        }
      }
      .width('100%')
      .padding({ left: 24, right: 24, top: 24, bottom: 32 })
    }
    .scrollBar(BarState.Off)
    .width('100%')
    .height('100%')
    .backgroundColor($r('app.color.app_background'))
  }
}
