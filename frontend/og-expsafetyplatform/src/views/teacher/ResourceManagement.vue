<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Teaching Resources</p>
        <h1>资源管理</h1>
        <p class="page-desc">维护实验指导书、课件视频、事故案例、设备说明和虚拟仿真实验入口。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增资源</el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="关键词、标签、知识点" @keyup.enter="loadResources" />
        <el-input v-model="filters.experimentId" clearable placeholder="实验ID" @keyup.enter="loadResources" />
        <el-select v-model="filters.resourceType" clearable placeholder="资源类型">
          <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.requiredFlag" clearable placeholder="学习属性">
          <el-option label="必学资源" :value="1" />
          <el-option label="拓展资源" :value="0" />
        </el-select>
        <el-select v-model="filters.invalidFlag" clearable placeholder="链接状态">
          <el-option label="正常" :value="0" />
          <el-option label="已失效" :value="1" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadResources">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="resources" stripe>
        <template #empty>
          <el-empty description="暂无教学资源" />
        </template>
        <el-table-column prop="title" label="标题" min-width="210">
          <template #default="{ row }">
            <div class="title-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.knowledgePoint || '未设置知识点' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="experimentId" label="实验ID" width="90" />
        <el-table-column label="类型" width="140">
          <template #default="{ row }">{{ typeLabel(row.resourceType) }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="tag in splitTags(row.tags)" :key="tag" size="small">{{ tag }}</el-tag>
            <span v-if="!row.tags" class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="必学" width="86">
          <template #default="{ row }">
            <el-tag :type="row.requiredFlag ? 'warning' : 'info'">{{ row.requiredFlag ? '必学' : '拓展' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="使用" width="150">
          <template #default="{ row }">
            <span class="metric">看 {{ row.viewCount || 0 }}</span>
            <span class="metric">下 {{ row.downloadCount || 0 }}</span>
            <span class="metric">藏 {{ row.favoriteCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.invalidFlag ? 'danger' : (row.status === 1 ? 'success' : 'info')">
              {{ row.invalidFlag ? '失效' : (row.status === 1 ? '开放' : '停用') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="preview(row)">预览</el-button>
            <el-button text :icon="DataAnalysis" @click="openStats(row)">统计</el-button>
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button text :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '开放' }}
            </el-button>
            <el-button text :type="row.invalidFlag ? 'success' : 'warning'" @click="toggleInvalid(row)">
              {{ row.invalidFlag ? '恢复' : '失效' }}
            </el-button>
            <el-button text type="danger" :icon="Delete" @click="removeResource(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="pageNum" layout="prev, pager, next" :page-size="10" :total="total" @current-change="loadResources" />
      </div>
    </section>

    <el-dialog v-model="formVisible" :title="editingResource ? '编辑资源' : '新增资源'" width="780px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <div class="form-grid">
          <el-form-item label="实验ID" prop="experimentId"><el-input v-model="form.experimentId" /></el-form-item>
          <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="资源类型" prop="resourceType">
            <el-select v-model="form.resourceType" filterable>
              <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识点"><el-input v-model="form.knowledgePoint" /></el-form-item>
          <el-form-item label="风险类型"><el-input v-model="form.riskType" /></el-form-item>
          <el-form-item label="标签"><el-input v-model="form.tags" placeholder="多个标签用逗号分隔" /></el-form-item>
          <el-form-item label="开放时间"><el-date-picker v-model="form.openTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选" /></el-form-item>
          <el-form-item label="关闭时间"><el-date-picker v-model="form.closeTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选" /></el-form-item>
          <el-form-item label="必学资源"><el-switch v-model="form.required" /></el-form-item>
          <el-form-item label="开放状态"><el-switch v-model="form.enabled" /></el-form-item>
          <el-form-item label="完成规则">
            <el-select v-model="form.completionRule">
              <el-option label="确认完成" value="CONFIRM" />
              <el-option label="按进度" value="PROGRESS" />
              <el-option label="按时长" value="TIME" />
              <el-option label="进度+时长" value="PROGRESS_TIME" />
            </el-select>
          </el-form-item>
          <el-form-item label="最低进度">
            <el-input-number v-model="form.minProgress" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="最短学习秒数">
            <el-input-number v-model="form.minStudySeconds" :min="0" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sort" :min="0" />
          </el-form-item>
        </div>
        <el-form-item label="外部链接">
          <el-input v-model="form.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="上传文件">
          <div class="upload-row">
            <el-upload :auto-upload="false" :show-file-list="false" :on-change="handleFilePicked">
              <el-button :icon="Upload">选择文件</el-button>
            </el-upload>
            <span>{{ form.originalFilename || form.filePath || '未选择文件' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="资源说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveResource">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statsVisible" title="资源使用统计" width="520px">
      <div v-if="stats" class="stats-grid">
        <div><strong>{{ stats.viewCount || 0 }}</strong><span>查看次数</span></div>
        <div><strong>{{ stats.downloadCount || 0 }}</strong><span>下载次数</span></div>
        <div><strong>{{ stats.favoriteCount || 0 }}</strong><span>收藏</span></div>
        <div><strong>{{ stats.completionRate || 0 }}%</strong><span>完成率</span></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataAnalysis, Delete, Edit, Plus, Search, Upload, View } from '@element-plus/icons-vue'
import {
  createResource,
  deleteResource,
  getResources,
  getResourceStats,
  markResourceInvalid,
  updateResource,
  updateResourceStatus,
  uploadResource,
} from '@/api/resource'

const resourceTypes = [
  { label: '实验指导书', value: 'GUIDE' },
  { label: '课程讲义', value: 'LECTURE' },
  { label: 'PPT课件', value: 'PPT' },
  { label: '教学视频', value: 'TEACHING_VIDEO' },
  { label: '微课', value: 'MICRO_COURSE' },
  { label: '仪器操作视频', value: 'INSTRUMENT_VIDEO' },
  { label: 'HSE安全培训视频', value: 'HSE_VIDEO' },
  { label: '设备说明书', value: 'DEVICE_MANUAL' },
  { label: '实验案例', value: 'EXPERIMENT_CASE' },
  { label: '事故案例', value: 'ACCIDENT_CASE' },
  { label: '应急处置流程', value: 'EMERGENCY_PROCESS' },
  { label: '实验报告模板', value: 'REPORT_TEMPLATE' },
  { label: '参考文献', value: 'REFERENCE' },
  { label: '外部课程链接', value: 'EXTERNAL_COURSE' },
  { label: '虚拟仿真实验', value: 'VIRTUAL_SIMULATION' },
  { label: '文档', value: 'DOCUMENT' },
  { label: '图片', value: 'IMAGE' },
  { label: '视频', value: 'VIDEO' },
  { label: '音频', value: 'AUDIO' },
  { label: '网页链接', value: 'LINK' },
  { label: '文件', value: 'FILE' },
]

const filters = reactive({ keyword: '', experimentId: '', resourceType: '', requiredFlag: '', invalidFlag: '' })
const form = reactive(defaultForm())
const rules = {
  experimentId: [{ required: true, message: '请填写实验ID', trigger: 'blur' }],
  title: [{ required: true, message: '请填写资源标题', trigger: 'blur' }],
  resourceType: [{ required: true, message: '请选择资源类型', trigger: 'change' }],
}
const formRef = ref(null)
const resources = ref([])
const editingResource = ref(null)
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const statsVisible = ref(false)
const stats = ref(null)

onMounted(loadResources)

async function loadResources() {
  loading.value = true
  try {
    const result = await getResources({
      pageNum: pageNum.value,
      pageSize: 10,
      keyword: filters.keyword || undefined,
      experimentId: filters.experimentId || undefined,
      resourceType: filters.resourceType || undefined,
      requiredFlag: filters.requiredFlag === '' ? undefined : filters.requiredFlag,
      invalidFlag: filters.invalidFlag === '' ? undefined : filters.invalidFlag,
    })
    resources.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

function defaultForm() {
  return {
    experimentId: '',
    title: '',
    resourceType: 'GUIDE',
    knowledgePoint: '',
    riskType: '',
    tags: '',
    url: '',
    filePath: '',
    fileSize: 0,
    originalFilename: '',
    contentType: '',
    required: false,
    enabled: true,
    completionRule: 'CONFIRM',
    minProgress: 100,
    minStudySeconds: 0,
    openTime: '',
    closeTime: '',
    sort: 0,
    description: '',
  }
}

function openCreate() {
  editingResource.value = null
  Object.assign(form, defaultForm())
  formVisible.value = true
}

function openEdit(row) {
  editingResource.value = row
  Object.assign(form, {
    experimentId: row.experimentId || '',
    title: row.title || '',
    resourceType: row.resourceType || 'GUIDE',
    knowledgePoint: row.knowledgePoint || '',
    riskType: row.riskType || '',
    tags: row.tags || '',
    url: row.url || '',
    filePath: row.filePath || '',
    fileSize: row.fileSize || 0,
    originalFilename: row.originalFilename || '',
    contentType: row.contentType || '',
    required: row.requiredFlag === 1,
    enabled: row.status === 1,
    completionRule: row.completionRule || 'CONFIRM',
    minProgress: row.minProgress ?? 100,
    minStudySeconds: row.minStudySeconds ?? 0,
    openTime: row.openTime || '',
    closeTime: row.closeTime || '',
    sort: row.sort || 0,
    description: row.description || '',
  })
  formVisible.value = true
}

async function handleFilePicked(uploadFile) {
  const rawFile = uploadFile?.raw
  if (!rawFile) return
  saving.value = true
  try {
    const result = await uploadResource(rawFile)
    Object.assign(form, {
      filePath: result.filePath,
      fileSize: result.fileSize || rawFile.size,
      originalFilename: result.originalFilename || rawFile.name,
      contentType: result.contentType || rawFile.type,
    })
    ElMessage.success('文件已上传')
  } finally {
    saving.value = false
  }
}

async function saveResource() {
  await formRef.value?.validate()
  if (!form.url && !form.filePath) {
    ElMessage.warning('请上传文件或登记外部链接')
    return
  }
  const payload = {
    experimentId: Number(form.experimentId),
    title: form.title.trim(),
    resourceType: form.resourceType,
    knowledgePoint: form.knowledgePoint || undefined,
    riskType: form.riskType || undefined,
    tags: form.tags || undefined,
    category: form.required ? 'REQUIRED' : 'EXTENSION',
    description: form.description || undefined,
    url: form.url || undefined,
    filePath: form.filePath || undefined,
    fileSize: Number(form.fileSize || 0),
    originalFilename: form.originalFilename || undefined,
    contentType: form.contentType || undefined,
    requiredFlag: form.required ? 1 : 0,
    status: form.enabled ? 1 : 0,
    completionRule: form.completionRule,
    minProgress: Number(form.minProgress || 0),
    minStudySeconds: Number(form.minStudySeconds || 0),
    openTime: form.openTime || undefined,
    closeTime: form.closeTime || undefined,
    sort: Number(form.sort || 0),
  }
  saving.value = true
  try {
    if (editingResource.value) {
      await updateResource(editingResource.value.id, payload)
      ElMessage.success('资源已更新')
    } else {
      await createResource(payload)
      ElMessage.success('资源已创建')
    }
    formVisible.value = false
    await loadResources()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  await updateResourceStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('状态已更新')
  await loadResources()
}

async function toggleInvalid(row) {
  await ElMessageBox.confirm(`确认将资源「${row.title}」标记为${row.invalidFlag ? '正常' : '失效'}吗？`, '资源状态', { type: 'warning' })
  await markResourceInvalid(row.id, row.invalidFlag ? 0 : 1)
  ElMessage.success('链接状态已更新')
  await loadResources()
}

async function removeResource(row) {
  await ElMessageBox.confirm(`确认删除资源「${row.title}」吗？`, '删除资源', { type: 'warning' })
  await deleteResource(row.id)
  ElMessage.success('资源已删除')
  await loadResources()
}

async function preview(row) {
  const target = row.url || row.filePath
  if (!target) {
    ElMessage.warning('资源未配置预览地址')
    return
  }
  window.open(target, '_blank', 'noopener,noreferrer')
  await loadResources()
}

async function openStats(row) {
  stats.value = await getResourceStats(row.id)
  statsVisible.value = true
}

function splitTags(tags) {
  return String(tags || '').split(/[,，]/).map((item) => item.trim()).filter(Boolean)
}

function typeLabel(type) {
  return resourceTypes.find((item) => item.value === type)?.label || type || '-'
}
</script>

<style scoped>
.business-page { max-width: 1240px; margin: 0 auto; }
.page-head, .pagination-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.page-head { align-items: flex-end; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 14px; }
.toolbar .el-input, .toolbar .el-select { width: 200px; }
.title-cell strong { display: block; color: #13233a; line-height: 1.4; }
.title-cell span, .muted { color: #7b8794; font-size: 12px; }
.metric { display: inline-block; color: #667085; font-size: 12px; margin-right: 8px; }
.pagination-row { color: #667085; padding-top: 14px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; column-gap: 12px; }
.upload-row { display: flex; align-items: center; gap: 12px; color: #667085; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.stats-grid div { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 14px; text-align: center; }
.stats-grid strong { display: block; color: #13233a; font-size: 22px; margin-bottom: 4px; }
.stats-grid span { color: #667085; font-size: 12px; }
@media (max-width: 760px) {
  .page-head, .pagination-row { align-items: stretch; flex-direction: column; }
  .toolbar .el-input, .toolbar .el-select, .form-grid { width: 100%; grid-template-columns: 1fr; }
}
</style>
