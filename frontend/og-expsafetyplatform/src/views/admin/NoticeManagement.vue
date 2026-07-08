<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Notice Administration</p>
        <h1>公告管理</h1>
        <p class="page-desc">维护面向不同角色的门户公告，控制发布时间、优先级和展示状态。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增公告</el-button>
    </section>

    <section class="toolbar">
      <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索标题或内容" @keyup.enter="loadNotices" />
      <el-select v-model="filters.targetRole" clearable placeholder="目标角色">
        <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态">
        <el-option label="发布中" :value="1" />
        <el-option label="已下线" :value="0" />
      </el-select>
      <el-button :icon="Refresh" @click="loadNotices">刷新</el-button>
    </section>

    <section class="table-card">
      <el-table :data="notices" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="目标角色" width="110">
          <template #default="{ row }">{{ roleLabel(row.targetRole) }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" effect="plain">{{ priorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">{{ Number(row.status) === 1 ? '发布中' : '已下线' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" min-width="170" />
        <el-table-column prop="expireTime" label="过期时间" min-width="170">
          <template #default="{ row }">{{ row.expireTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="Number(row.status) !== 1" text type="success" @click="publishNotice(row)">发布</el-button>
            <el-button v-else text type="warning" @click="offlineNotice(row)">下线</el-button>
            <el-button text type="danger" :icon="Delete" @click="removeNotice(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && notices.length === 0" description="暂无公告" />
      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="pageNum"
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="total"
          @current-change="loadNotices"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingNotice ? '编辑公告' : '新增公告'" width="640px">
      <el-form :model="form" label-width="88px">
        <el-form-item label="标题"><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="目标角色">
          <el-select v-model="form.targetRole">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="发布" inactive-text="下线" />
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker v-model="form.publishTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择发布时间" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker v-model="form.expireTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveNotice">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createAdminNotice,
  deleteAdminNotice,
  getAdminNotices,
  offlineAdminNotice,
  publishAdminNotice,
  updateAdminNotice,
} from '@/api/adminPortal'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingNotice = ref(null)
const notices = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({ keyword: '', targetRole: '', status: '' })
const form = reactive({
  title: '',
  content: '',
  targetRole: 'ALL',
  priority: 'MEDIUM',
  enabled: true,
  publishTime: '',
  expireTime: '',
})

const roleOptions = [
  { label: '全部角色', value: 'ALL' },
  { label: '普通用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' },
]

watch([() => filters.targetRole, () => filters.status], () => {
  pageNum.value = 1
  loadNotices()
})

onMounted(loadNotices)

async function loadNotices() {
  loading.value = true
  try {
    const result = await getAdminNotices({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      targetRole: filters.targetRole || undefined,
      status: filters.status === '' ? undefined : filters.status,
    })
    notices.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingNotice.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingNotice.value = row
  Object.assign(form, {
    title: row.title || '',
    content: row.content || '',
    targetRole: row.targetRole || 'ALL',
    priority: row.priority || 'MEDIUM',
    enabled: Number(row.status) === 1,
    publishTime: toInputTime(row.publishTime),
    expireTime: toInputTime(row.expireTime),
  })
  dialogVisible.value = true
}

async function saveNotice() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      targetRole: form.targetRole,
      priority: form.priority,
      status: form.enabled ? 1 : 0,
      publishTime: form.publishTime || undefined,
      expireTime: form.expireTime || undefined,
    }
    if (editingNotice.value) {
      await updateAdminNotice(editingNotice.value.id, payload)
      ElMessage.success('公告已更新')
    } else {
      await createAdminNotice(payload)
      ElMessage.success('公告已创建')
    }
    dialogVisible.value = false
    await loadNotices()
  } finally {
    saving.value = false
  }
}

async function publishNotice(row) {
  await publishAdminNotice(row.id)
  ElMessage.success('公告已发布')
  await loadNotices()
}

async function offlineNotice(row) {
  await offlineAdminNotice(row.id)
  ElMessage.success('公告已下线')
  await loadNotices()
}

async function removeNotice(row) {
  try {
    await ElMessageBox.confirm(`确认删除公告“${row.title}”吗？`, '删除公告', { type: 'warning' })
    await deleteAdminNotice(row.id)
    ElMessage.success('公告已删除')
    await loadNotices()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  }
}

function resetForm() {
  Object.assign(form, {
    title: '',
    content: '',
    targetRole: 'ALL',
    priority: 'MEDIUM',
    enabled: true,
    publishTime: '',
    expireTime: '',
  })
}

function roleLabel(value) {
  return roleOptions.find((item) => item.value === value)?.label || value || '全部角色'
}

function priorityLabel(value) {
  return { HIGH: '高', MEDIUM: '中', LOW: '低' }[value] || '中'
}

function priorityType(value) {
  return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }[value] || 'warning'
}

function toInputTime(value) {
  return value ? String(value).replace(' ', 'T').slice(0, 19) : ''
}
</script>

<style scoped>
.admin-page { max-width: 1240px; margin: 0 auto; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.toolbar, .table-card { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: minmax(260px, 1fr) 150px 130px auto; gap: 12px; padding: 14px; margin-bottom: 16px; }
.table-card { padding: 14px; }
.pagination-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #667085; padding-top: 14px; }
@media (max-width: 900px) {
  .page-head { align-items: stretch; flex-direction: column; }
  .toolbar { grid-template-columns: 1fr; }
}
</style>
