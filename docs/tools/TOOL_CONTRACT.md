# 工具契约与接入规范

每个工具必须先完成契约，再实现后端执行器、HarmonyOS 页面和管理后台配置。工具契约是工具输入、输出、错误、权益和版本行为的唯一来源；不得只在某一端手写字段。

## 目录

```text
docs/tools/{toolId}/
├─ manifest.json
├─ input.schema.json
├─ output.schema.json
├─ errors.json
└─ test-cases.json
```

## 必填规则

- `toolId` 全局唯一，只允许小写字母、数字和短横线，发布后不可复用。
- `version` 使用 SemVer；版本的输入、输出和错误语义不可静默改变。
- `manifest.json` 声明展示信息、执行器键、超时、最低客户端版本和访问策略。
- `input.schema.json`、`output.schema.json` 使用 JSON Schema Draft 2020-12 子集。
- 不允许把 Token、密码、Client Secret 或真实用户数据写入契约和测试夹具。
- 执行器必须是编译进后端的 Java Bean；禁止后台上传并执行脚本。

`text-echo/` 是本仓库唯一的最小完整示例：契约、Java 执行器、Flyway 种子和测试必须一起演进；新工具复制模板后更换 `toolId`，不得复制已发布的业务标识。

## 生命周期

```text
DRAFT → TESTING → PUBLISHED → DISABLED / DEPRECATED
```

未发布版本不能被执行。客户端不传版本时使用当前稳定版本；回滚只切换稳定版本指针，不删除历史版本。

## 执行请求

```http
POST /api/v1/tools/{toolId}/execute
Authorization: Bearer <platform-token>
Content-Type: application/json
```

```json
{
  "toolVersion": "1.0.0",
  "input": {},
  "clientRequestId": "client-generated-idempotency-key"
}
```

平台统一完成会话、版本、Schema、权益、幂等、扣次、审计和错误映射；执行器只负责输入对象到输出对象的业务转换。

## 测试最低集合

- 合法输入和输出
- 空输入、类型错误、范围错误、额外字段
- 游客、普通登录用户和无权限访问
- 额度耗尽、并发扣次和重复 `clientRequestId`
- 外部依赖超时、错误和执行失败不扣次
- 未发布版本、版本回滚和旧客户端兼容
