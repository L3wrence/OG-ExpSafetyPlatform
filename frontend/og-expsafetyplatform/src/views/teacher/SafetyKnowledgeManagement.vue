<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Safety Knowledge</p>
        <h1>安全知识管理</h1>
        <p class="page-desc">维护实验安全知识点、风险类型和开放状态。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增知识点</el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="知识点或内容" />
        <el-input v-model="filters.experimentId" clearable placeholder="实验ID" />
        <el-input v-model="filters.riskType" clearable placeholder="风险类型" />
        <el-button type="primary" :icon="Search" @click="loadKnowledge">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="knowledgeList" stripe>
        <el-table-column prop="knowledgePoint" label="知识点" min-width="160" />
        <el-table-column prop="riskType" label="风险类型" width="120" />
        <el-table-column prop="experimentId" label="实验ID" width="90" />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '开放' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" :icon="Delete" @click="removeKnowledge(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="pageNum" layout="prev, pager, next" :page-size="10" :total="total" @current-change="loadKnowledge" />
      </div>
    </section>

    <el-dialog v-model="formVisible" :title="editingKnowledge ? '编辑知识点' : '新增知识点'" width="660px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="实验ID"><el-input v-model="form.experimentId" /></el-form-item>
        <el-form-item label="知识点" required><el-input v-model="form.knowledgePoint" /></el-form-item>
        <el-form-item label="风险类型"><el-input v-model="form.riskType" /></el-form-item>
        <el-form-item label="步骤ID"><el-input v-model="form.relatedStepId" /></el-form-item>
        <el-form-item label="开放"><el-switch v-model="form.enabled" /></el-form-item>
        <el-form-item label="内容" required><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveKnowledge">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { createSafetyKnowledge, deleteSafetyKnowledge, getSafetyKnowledge, updateSafetyKnowledge } from '@/api/safetyKnowledge'

const filters = reactive({ keyword: '', experimentId: '', riskType: '' })
const form = reactive({ experimentId: '', knowledgePoint: '', riskType: '', relatedStepId: '', content: '', enabled: true })
const knowledgeList = ref([])
const editingKnowledge = ref(null)
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)

onMounted(loadKnowledge)

async function loadKnowledge() {
  loading.value = true
  try {
    const result = await getSafetyKnowledge({
      pageNum: pageNum.value,
      pageSize: 10,
      keyword: filters.keyword || undefined,
      experimentId: filters.experimentId || undefined,
      riskType: filters.riskType || undefined,
    })
    knowledgeList.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingKnowledge.value = null
  Object.assign(form, { experimentId: '', knowledgePoint: '', riskType: '', relatedStepId: '', content: '', enabled: true })
  formVisible.value = true
}

function openEdit(row) {
  editingKnowledge.value = row
  Object.assign(form, {
    experimentId: row.experimentId || '',
    knowledgePoint: row.knowledgePoint || '',
    riskType: row.riskType || '',
    relatedStepId: row.relatedStepId || '',
    content: row.content || '',
    enabled: row.status === 1,
  })
  formVisible.value = true
}

async function saveKnowledge() {
  if (!form.knowledgePoint.trim() || !form.content.trim()) {
    ElMessage.warning('请填写知识点和内容')
    return
  }
  const payload = {
    experimentId: form.experimentId ? Number(form.experimentId) : undefined,
    knowledgePoint: form.knowledgePoint.trim(),
    riskType: form.riskType || undefined,
    relatedStepId: form.relatedStepId ? Number(form.relatedStepId) : undefined,
    content: form.content.trim(),
    status: form.enabled ? 1 : 0,
  }
  saving.value = true
  try {
    if (editingKnowledge.value) {
      await updateSafetyKnowledge(editingKnowledge.value.id, payload)
      ElMessage.success('知识点已更新')
    } else {
      await createSafetyKnowledge(payload)
      ElMessage.success('知识点已创建')
    }
    formVisible.value = false
    await loadKnowledge()
  } finally {
    saving.value = false
  }
}

async function removeKnowledge(row) {
  await ElMessageBox.confirm(`确认删除知识点「${row.knowledgePoint}」吗？`, '删除知识点', { type: 'warning' })
  await deleteSafetyKnowledge(row.id)
  ElMessage.success('知识点已删除')
  await loadKnowledge()
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
.toolbar .el-input { max-width: 210px; }
.pagination-row { color: #667085; padding-top: 14px; }
@media (max-width: 760px) { .page-head, .toolbar, .pagination-row { align-items: stretch; flex-direction: column; } .toolbar .el-input { max-width: none; } }
</style>
