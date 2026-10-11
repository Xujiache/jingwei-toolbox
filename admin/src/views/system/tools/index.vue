<template>
  <div class="tool-management art-full-height">
    <ElCard shadow="never" class="art-table-card">
      <template #header>
        <div class="flex items-center justify-between">
          <div>
            <div class="text-base font-semibold">工具目录与发布管理</div>
            <div class="mt-1 text-sm text-gray-500">工具元数据、输入输出 Schema、访问策略和版本发布均由服务端校验并记录审计。</div>
          </div>
          <div class="flex items-center gap-2">
            <ElButton :loading="loading" @click="loadTools">刷新</ElButton>
            <ElButton type="primary" @click="openCreateDialog">创建草稿</ElButton>
          </div>
        </div>
      </template>
      <ElAlert v-if="errorMessage" class="mb-4" type="error" :closable="false">{{ errorMessage }}</ElAlert>
      <ElTable v-loading="loading" :data="tools" row-key="toolId" border>
        <ElTableColumn prop="toolId" label="工具 ID" min-width="180" />
        <ElTableColumn prop="displayName" label="名称" min-width="180" />
        <ElTableColumn label="状态" width="120"><template #default="{ row }"><ElTag :type="statusTag(row.status)">{{ row.status }}</ElTag></template></ElTableColumn>
        <ElTableColumn prop="currentVersion" label="当前版本" width="130" />
        <ElTableColumn label="启用" width="90"><template #default="{ row }"><ElTag :type="row.policy.enabled ? 'success' : 'info'">{{ row.policy.enabled ? '是' : '否' }}</ElTag></template></ElTableColumn>
        <ElTableColumn label="登录要求" width="110"><template #default="{ row }">{{ row.policy.loginRequired ? '必须登录' : '允许游客' }}</template></ElTableColumn>
        <ElTableColumn label="游客额度" min-width="160"><template #default="{ row }"><span v-if="row.policy.guestLimit === null">不限次数</span><span v-else>{{ row.policy.guestLimit }} 次 / {{ row.policy.guestLimitWindowSeconds ?? '-' }} 秒</span></template></ElTableColumn>
        <ElTableColumn label="操作" fixed="right" min-width="270"><template #default="{ row }"><ElButton link type="primary" @click="openMetadataDialog(row)">元数据</ElButton><ElButton link type="primary" @click="openPolicyDialog(row)">策略</ElButton><ElButton link type="primary" @click="openVersionsDialog(row)">版本</ElButton></template></ElTableColumn>
      </ElTable>
    </ElCard>

    <ElDialog v-model="metadataVisible" :title="metadataMode === 'create' ? '创建工具草稿' : '编辑工具元数据'" width="680px" destroy-on-close>
      <ElForm label-width="132px">
        <ElFormItem label="工具 ID" required><ElInput v-model="metadataDraft.toolId" :disabled="metadataMode === 'edit'" placeholder="例如 text-format" /></ElFormItem>
        <ElFormItem label="显示名称" required><ElInput v-model="metadataDraft.displayName" maxlength="128" show-word-limit /></ElFormItem>
        <ElFormItem label="描述"><ElInput v-model="metadataDraft.description" type="textarea" maxlength="2000" show-word-limit /></ElFormItem>
        <ElFormItem label="当前版本" required><ElInput v-model="metadataDraft.currentVersion" placeholder="1.0.0" /></ElFormItem>
        <ElFormItem label="状态" required><ElSelect v-model="metadataDraft.status" class="w-full"><ElOption v-for="status in statuses" :key="status" :label="status" :value="status" /></ElSelect></ElFormItem>
        <ElFormItem label="输入 Schema"><ElInput v-model="metadataDraft.inputSchemaText" type="textarea" :rows="5" placeholder="{}" /></ElFormItem>
        <ElFormItem label="输出 Schema"><ElInput v-model="metadataDraft.outputSchemaText" type="textarea" :rows="5" placeholder="{}" /></ElFormItem>
      </ElForm>
      <template #footer><ElButton @click="metadataVisible = false">取消</ElButton><ElButton type="primary" :loading="saving" @click="saveMetadata">保存</ElButton></template>
    </ElDialog>

    <ElDialog v-model="policyVisible" title="编辑工具访问策略" width="520px" destroy-on-close>
      <ElForm label-width="132px">
        <ElFormItem label="工具"><span>{{ policyDraft.toolId }}</span></ElFormItem>
        <ElFormItem label="权益主体"><ElSelect v-model="policyDraft.principalType" class="w-full" @change="loadAccessPolicy(policyDraft.principalType)"><ElOption v-for="principal in principalTypes" :key="principal" :label="principal" :value="principal" /></ElSelect></ElFormItem>
        <ElFormItem label="启用工具"><ElSwitch v-model="policyDraft.enabled" /></ElFormItem>
        <ElFormItem label="必须登录"><ElSwitch v-model="policyDraft.loginRequired" /></ElFormItem>
        <ElFormItem label="额度"><div class="flex items-center gap-3"><ElCheckbox v-model="policyDraft.unlimited">不限次数</ElCheckbox><ElInputNumber v-if="!policyDraft.unlimited" v-model="policyDraft.quotaLimit" :min="0" :precision="0" controls-position="right" /></div></ElFormItem>
        <ElFormItem v-if="!policyDraft.unlimited" label="额度窗口（秒）"><ElInputNumber v-model="policyDraft.quotaWindowSeconds" :min="1" :precision="0" controls-position="right" /></ElFormItem>
        <ElFormItem label="消耗次数"><ElSwitch v-model="policyDraft.consumesQuota" /></ElFormItem>
        <ElFormItem label="允许并发"><ElSwitch v-model="policyDraft.allowConcurrent" /></ElFormItem>
      </ElForm>
      <template #footer><ElButton @click="policyVisible = false">取消</ElButton><ElButton type="primary" :loading="saving" @click="savePolicy">保存策略</ElButton></template>
    </ElDialog>

    <ElDialog v-model="versionsVisible" title="工具版本与发布" width="820px" destroy-on-close>
      <div v-if="selectedTool" class="mb-4 flex items-center justify-between"><div><div class="font-semibold">{{ selectedTool.displayName }}</div><div class="text-xs text-gray-500">稳定版本：{{ selectedTool.currentVersion }}</div></div><ElButton type="primary" @click="openVersionCreate">创建版本</ElButton></div>
      <ElAlert v-if="versionsError" class="mb-3" type="warning" :closable="false">{{ versionsError }}</ElAlert>
      <ElTable v-loading="versionsLoading" :data="versions" row-key="toolVersion" border>
        <ElTableColumn prop="toolVersion" label="版本" width="130" />
        <ElTableColumn prop="status" label="状态" width="130"><template #default="{ row }"><ElTag :type="versionStatusTag(row.status)">{{ row.status }}</ElTag></template></ElTableColumn>
        <ElTableColumn label="执行器键" min-width="150"><template #default="{ row }"><span class="font-mono text-xs">{{ row.executorKey }}</span></template></ElTableColumn>
        <ElTableColumn prop="createdAt" label="创建时间" min-width="180" />
        <ElTableColumn label="操作" fixed="right" min-width="250"><template #default="{ row }"><ElButton v-if="row.status === 'DRAFT'" link type="warning" @click="testVersion(row)">提交测试</ElButton><ElButton v-if="row.status === 'TESTING'" link type="primary" @click="publishVersion(row)">发布</ElButton><ElButton v-if="row.status === 'PUBLISHED' || row.status === 'DEPRECATED'" link type="warning" @click="rollbackVersion(row)">回滚</ElButton></template></ElTableColumn>
      </ElTable>
    </ElDialog>

    <ElDialog v-model="versionCreateVisible" title="创建工具版本" width="620px" destroy-on-close>
      <ElForm label-width="132px"><ElFormItem label="版本号" required><ElInput v-model="versionDraft.version" placeholder="1.0.0" /></ElFormItem><ElFormItem label="输入 Schema"><ElInput v-model="versionDraft.inputSchemaText" type="textarea" :rows="5" placeholder="{}" /></ElFormItem><ElFormItem label="输出 Schema"><ElInput v-model="versionDraft.outputSchemaText" type="textarea" :rows="5" placeholder="{}" /></ElFormItem></ElForm>
      <template #footer><ElButton @click="versionCreateVisible = false">取消</ElButton><ElButton type="primary" :loading="saving" @click="createVersion">保存版本</ElButton></template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
  import { adminCreateTool, adminCreateToolVersion, adminGetToolAccessPolicy, adminListToolVersions, adminListTools, adminPublishToolVersion, adminRollbackTool, adminTestToolVersion, adminUpdateTool, adminUpdateToolAccessPolicy } from '@/api/platform'
  import type { ToolAccessPolicyPatch, ToolDefinition, ToolPatchRequest, ToolPrincipalType, ToolStatus, ToolUpsertRequest, ToolVersionCreateRequest, ToolVersionDefinition, ToolVersionStatus } from '@/types/api/contract'
  import { HttpError } from '@/utils/http/error'

  defineOptions({ name: 'ToolManagement' })
  const statuses: ToolStatus[] = ['DRAFT', 'PUBLISHED', 'DISABLED', 'DEPRECATED']
  const principalTypes: ToolPrincipalType[] = ['GUEST', 'USER', 'MEMBER', 'AD_REWARDED']
  const tools = ref<ToolDefinition[]>([])
  const versions = ref<ToolVersionDefinition[]>([])
  const loading = ref(false)
  const versionsLoading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')
  const versionsError = ref('')
  const metadataVisible = ref(false)
  const metadataMode = ref<'create' | 'edit'>('create')
  const policyVisible = ref(false)
  const versionsVisible = ref(false)
  const versionCreateVisible = ref(false)
  const selectedTool = ref<ToolDefinition | null>(null)
  const metadataDraft = reactive({ toolId: '', displayName: '', description: '', currentVersion: '1.0.0', status: 'DRAFT' as ToolStatus, inputSchemaText: '{}', outputSchemaText: '{}', version: 0 })
  const policyDraft = reactive({ toolId: '', principalType: 'GUEST' as ToolPrincipalType, policyVersion: 0, enabled: true, loginRequired: false, unlimited: true, quotaLimit: null as number | null, quotaWindowSeconds: null as number | null, consumesQuota: true, allowConcurrent: true })
  const versionDraft = reactive({ version: '1.0.0', inputSchemaText: '{}', outputSchemaText: '{}' })

  const statusTag = (status: ToolStatus) => status === 'PUBLISHED' ? 'success' : status === 'DISABLED' ? 'danger' : status === 'DEPRECATED' ? 'warning' : 'info'
  const versionStatusTag = (status: ToolVersionStatus) => status === 'PUBLISHED' ? 'success' : status === 'TESTING' ? 'warning' : status === 'DISABLED' || status === 'DEPRECATED' ? 'danger' : 'info'
  const parseSchema = (value: string): Record<string, unknown> | undefined => {
    try {
      const parsed: unknown = JSON.parse(value || '{}')
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error('schema-object')
      return parsed as Record<string, unknown>
    } catch {
      ElMessage.error('Schema 必须是合法 JSON 对象')
      return undefined
    }
  }
  const schemaText = (schema?: Record<string, unknown>) => JSON.stringify(schema ?? {}, null, 2)

  const loadTools = async () => {
    loading.value = true
    errorMessage.value = ''
    try { tools.value = (await adminListTools({ page: 1, pageSize: 100 })).items } catch (error) { errorMessage.value = error instanceof HttpError ? error.message : '工具目录加载失败' } finally { loading.value = false }
  }
  const openCreateDialog = () => { metadataMode.value = 'create'; Object.assign(metadataDraft, { toolId: '', displayName: '', description: '', currentVersion: '1.0.0', status: 'DRAFT', inputSchemaText: '{}', outputSchemaText: '{}', version: 0 }); metadataVisible.value = true }
  const openMetadataDialog = (tool: ToolDefinition) => { metadataMode.value = 'edit'; Object.assign(metadataDraft, { toolId: tool.toolId, displayName: tool.displayName, description: tool.description ?? '', currentVersion: tool.currentVersion, status: tool.status, inputSchemaText: schemaText(tool.inputSchema), outputSchemaText: schemaText(tool.outputSchema), version: tool.version }); metadataVisible.value = true }
  const saveMetadata = async () => {
    const inputSchema = parseSchema(metadataDraft.inputSchemaText); const outputSchema = parseSchema(metadataDraft.outputSchemaText)
    if (!inputSchema || !outputSchema || !metadataDraft.toolId || !metadataDraft.displayName || !metadataDraft.currentVersion) return
    saving.value = true
    try {
      let updated: ToolDefinition
      if (metadataMode.value === 'create') {
        const payload: ToolUpsertRequest = { toolId: metadataDraft.toolId, displayName: metadataDraft.displayName, description: metadataDraft.description, currentVersion: metadataDraft.currentVersion, status: metadataDraft.status, inputSchema, outputSchema }
        updated = await adminCreateTool(payload)
      } else {
        const payload: ToolPatchRequest = { displayName: metadataDraft.displayName, description: metadataDraft.description, currentVersion: metadataDraft.currentVersion, status: metadataDraft.status, inputSchema, outputSchema, expectedVersion: metadataDraft.version }
        updated = await adminUpdateTool(metadataDraft.toolId, payload)
      }
      const index = tools.value.findIndex((item) => item.toolId === updated.toolId); if (index >= 0) tools.value[index] = updated; else tools.value.unshift(updated); metadataVisible.value = false; ElMessage.success('工具元数据已保存')
    } catch (error) { ElMessage.error(error instanceof HttpError ? error.message : '工具元数据保存失败'); await loadTools() } finally { saving.value = false }
  }
  const openPolicyDialog = async (tool: ToolDefinition) => { Object.assign(policyDraft, { toolId: tool.toolId, principalType: 'GUEST', policyVersion: 0, enabled: tool.policy.enabled, loginRequired: tool.policy.loginRequired, unlimited: tool.policy.guestLimit === null, quotaLimit: tool.policy.guestLimit, quotaWindowSeconds: tool.policy.guestLimitWindowSeconds, consumesQuota: true, allowConcurrent: true }); policyVisible.value = true; await loadAccessPolicy('GUEST') }
  const loadAccessPolicy = async (principalType: ToolPrincipalType) => {
    if (!policyDraft.toolId) return
    try {
      const policy = await adminGetToolAccessPolicy(policyDraft.toolId, principalType)
      Object.assign(policyDraft, { principalType: policy.principalType, policyVersion: policy.version, enabled: policy.enabled, loginRequired: policy.loginRequired, unlimited: policy.quotaLimit === null, quotaLimit: policy.quotaLimit, quotaWindowSeconds: policy.quotaWindowSeconds, consumesQuota: policy.consumesQuota, allowConcurrent: policy.allowConcurrent })
    } catch (error) { ElMessage.error(error instanceof HttpError ? error.message : '权益策略加载失败') }
  }
  const savePolicy = async () => {
    const payload: ToolAccessPolicyPatch = { enabled: policyDraft.enabled, loginRequired: policyDraft.loginRequired, quotaLimit: policyDraft.unlimited ? null : (policyDraft.quotaLimit ?? 0), quotaWindowSeconds: policyDraft.unlimited ? null : (policyDraft.quotaWindowSeconds ?? 3600), clearQuota: policyDraft.unlimited, consumesQuota: policyDraft.consumesQuota, allowConcurrent: policyDraft.allowConcurrent, expectedVersion: policyDraft.policyVersion }
    saving.value = true
    try { await adminUpdateToolAccessPolicy(policyDraft.toolId, policyDraft.principalType, payload); policyVisible.value = false; ElMessage.success('策略已保存'); await loadTools() } catch (error) { ElMessage.error(error instanceof HttpError ? error.message : '策略保存失败'); await loadTools() } finally { saving.value = false }
  }
  const openVersionsDialog = async (tool: ToolDefinition) => { selectedTool.value = tool; versionsVisible.value = true; await loadVersions(tool.toolId) }
  const loadVersions = async (toolId: string) => { versionsLoading.value = true; versionsError.value = ''; try { versions.value = await adminListToolVersions(toolId) } catch (error) { versions.value = []; versionsError.value = error instanceof HttpError ? error.message : '版本列表加载失败，请确认后端版本接口已部署' } finally { versionsLoading.value = false } }
  const openVersionCreate = () => { Object.assign(versionDraft, { version: selectedTool.value?.currentVersion ?? '1.0.0', inputSchemaText: '{}', outputSchemaText: '{}' }); versionCreateVisible.value = true }
  const createVersion = async () => {
    if (!selectedTool.value) return
    const inputSchema = parseSchema(versionDraft.inputSchemaText); const outputSchema = parseSchema(versionDraft.outputSchemaText); if (!inputSchema || !outputSchema || !versionDraft.version) return
    saving.value = true
    try { const payload: ToolVersionCreateRequest = { toolVersion: versionDraft.version, inputSchema, outputSchema }; await adminCreateToolVersion(selectedTool.value.toolId, payload); versionCreateVisible.value = false; await loadVersions(selectedTool.value.toolId); ElMessage.success('工具版本草稿已创建') } catch (error) { ElMessage.error(error instanceof HttpError ? error.message : '版本创建失败') } finally { saving.value = false }
  }
  const testVersion = async (version: ToolVersionDefinition) => {
    if (!selectedTool.value) return
    saving.value = true
    try { await adminTestToolVersion(selectedTool.value.toolId, version.toolVersion); ElMessage.success('版本已提交测试'); await loadVersions(selectedTool.value.toolId) } catch (error) { ElMessage.error(error instanceof HttpError ? error.message : '提交测试失败') } finally { saving.value = false }
  }
  const publishVersion = async (version: ToolVersionDefinition) => {
    if (!selectedTool.value) return
    saving.value = true
    try { await adminPublishToolVersion(selectedTool.value.toolId, version.toolVersion); ElMessage.success('版本已发布'); await loadTools(); const toolId = selectedTool.value.toolId; selectedTool.value = tools.value.find((item) => item.toolId === toolId) ?? selectedTool.value; await loadVersions(toolId) } catch (error) { ElMessage.error(error instanceof HttpError ? error.message : '版本发布失败') } finally { saving.value = false }
  }
  const rollbackVersion = async (version: ToolVersionDefinition) => {
    if (!selectedTool.value) return
    saving.value = true
    try { await adminRollbackTool(selectedTool.value.toolId, version.toolVersion); ElMessage.success('已切换稳定版本'); await loadTools(); const toolId = selectedTool.value.toolId; selectedTool.value = tools.value.find((item) => item.toolId === toolId) ?? selectedTool.value; await loadVersions(toolId) } catch (error) { ElMessage.error(error instanceof HttpError ? error.message : '版本回滚失败') } finally { saving.value = false }
  }
  onMounted(loadTools)
</script>
