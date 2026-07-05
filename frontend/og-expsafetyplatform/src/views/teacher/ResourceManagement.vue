<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Teaching Resources</p>
        <h1>资源管理</h1>
        <p class="page-desc">维护实验教学资源、资料链接和开放状态。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增资源</el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="资源标题或类型" />
        <el-input v-model="filters.experimentId" clearable placeholder="实验ID" />
        <el-select v-model="filters.resourceType" clearable placeholder="资源类型">
          <el-option label="视频" value="VIDEO" />
          <el-option label="文档" value="DOCUMENT" />
          <el-option label="图片" value="IMAGE" />
          <el-option label="链接" value="LINK" />
          <el-option label="文件" value="FILE" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadResources">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="resources" stripe>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="experimentId" label="实验ID" width="90" />
        <el-table-column prop="resourceType" label="类型" width="100" />
        <el-table-column label="必学" width="90">
          <template #default="{ row }"><el-tag :type="row.requiredFlag ? 'warning' : 'info'">{{ row.requiredFlag ? '必学' : '选学' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '开放' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button text :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '开放' }}</el-button>
            <el-button text type="danger" :icon="Delete" @click="removeResource(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="pageNum" layout="prev, pager, next" :page-size="10" :total="total" @current-change="loadResources" />
      </div>
    </section>

    <el-dialog v-model="formVisible" :title="editingResource ? '编辑资源' : '新增资源'" width="640px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="实验ID" required><el-input v-model="form.experimentId" /></el-form-item>
        <el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.resourceType">
            <el-option label="视频" value="VIDEO" />
            <el-option label="文档" value="DOCUMENT" />
            <el-option label="图片" value="IMAGE" />
            <el-option label="链接" value="LINK" />
            <el-option label="文件" value="FILE" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源URL"><el-input v-model="form.url" /></el-form-item>
        <el-form-item label="文件路径"><el-input v-model="form.filePath" /></el-form-item>
        <el-form-item label="文件大小"><el-input-number v-model="form.fileSize" :min="0" /></el-form-item>
        <el-form-item label="必学"><el-switch v-model="form.required" /></el-form-item>
        <el-form-item label="开放"><el-switch v-model="form.enabled" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveResource">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { createResource, deleteResource, getResources, updateResource, updateResourceStatus } from '@/api/resource'

const filters = reactive({ keyword: '', experimentId: '', resourceType: '' })
const form = reactive({ experimentId: '', title: '', resourceType: 'DOCUMENT', url: '', filePath: '', fileSize: 0, required: false, enabled: true, sort: 0 })
const resources = ref([])
const editingResource = ref(null)
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)

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
    })
    resources.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingResource.value = null
  Object.assign(form, { experimentId: '', title: '', resourceType: 'DOCUMENT', url: '', filePath: '', fileSize: 0, required: false, enabled: true, sort: 0 })
  formVisible.value = true
}

function openEdit(row) {
  editingResource.value = row
  Object.assign(form, {
    experimentId: row.experimentId || '',
    title: row.title || '',
    resourceType: row.resourceType || 'DOCUMENT',
    url: row.url || '',
    filePath: row.filePath || '',
    fileSize: row.fileSize || 0,
    required: row.requiredFlag === 1,
    enabled: row.status === 1,
    sort: row.sort || 0,
  })
  formVisible.value = true
}

async function saveResource() {
  if (!form.experimentId || !form.title.trim()) {
    ElMessage.warning('请填写实验ID和资源标题')
    return
  }
  const payload = {
    experimentId: Number(form.experimentId),
    title: form.title.trim(),
    resourceType: form.resourceType,
    url: form.url || undefined,
    filePath: form.filePath || undefined,
    fileSize: Number(form.fileSize || 0),
    requiredFlag: form.required ? 1 : 0,
    status: form.enabled ? 1 : 0,
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

async function removeResource(row) {
  await ElMessageBox.confirm(`确认删除资源「${row.title}」吗？`, '删除资源', { type: 'warning' })
  await deleteResource(row.id)
  ElMessage.success('资源已删除')
  await loadResources()
}
</script>

<style scoped>
.business-page { max-width: 1240px; margin: 0 auto; }
.page-head, .pagination-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.page-head { align-items: flex-end; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.toolbar .el-input, .toolbar .el-select { max-width: 210px; }
.pagination-row { color: #667085; padding-top: 14px; }
@media (max-width: 760px) { .page-head, .toolbar, .pagination-row { align-items: stretch; flex-direction: column; } .toolbar .el-input, .toolbar .el-select { max-width: none; } }
</style>
