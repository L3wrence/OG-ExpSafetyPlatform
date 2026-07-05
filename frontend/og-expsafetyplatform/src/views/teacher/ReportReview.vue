<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Report Review</p>
        <h1>报告批改</h1>
        <p class="page-desc">查看学生提交的实验报告，完成评分或退回修改。</p>
      </div>
      <el-button :icon="Refresh" @click="loadReports">刷新</el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="experimentId" clearable placeholder="实验ID" />
        <el-button type="primary" :icon="Search" @click="loadReports">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="reports" stripe>
        <el-table-column prop="studentName" label="学生" min-width="110" />
        <el-table-column prop="title" label="报告标题" min-width="190" />
        <el-table-column prop="experimentId" label="实验ID" width="90" />
        <el-table-column prop="latestSubmitTime" label="提交时间" min-width="160" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="reportStatusMeta(row.status).type">{{ reportStatusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">查看</el-button>
            <el-button text type="success" @click="openGrade(row)">评分</el-button>
            <el-button text type="danger" @click="openReturn(row)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="pageNum" layout="prev, pager, next" :page-size="10" :total="total" @current-change="loadReports" />
      </div>
    </section>

    <el-dialog v-model="detailVisible" title="报告详情" width="760px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-head">
            <h2>{{ detail.title }}</h2>
            <el-tag :type="reportStatusMeta(detail.status).type">{{ reportStatusMeta(detail.status).label }}</el-tag>
          </div>
          <p class="detail-meta">学生：{{ detail.studentName || detail.studentId }}　实验ID：{{ detail.experimentId }}</p>
          <p class="report-content">{{ detail.content }}</p>
          <el-link v-if="detail.fileUrl" :href="detail.fileUrl" target="_blank" type="primary">查看附件</el-link>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="gradeVisible" title="报告评分" width="520px">
      <el-form :model="gradeForm" label-width="80px">
        <el-form-item label="分数"><el-input-number v-model="gradeForm.score" :min="0" :max="100" /></el-form-item>
        <el-form-item label="评语"><el-input v-model="gradeForm.comment" type="textarea" :rows="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitGrade">保存评分</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnVisible" title="退回修改" width="520px">
      <el-input v-model="returnComment" type="textarea" :rows="5" placeholder="填写退回原因和修改建议" />
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="submitReturn">退回报告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getPendingReports, getReportDetail, gradeReport, returnReport } from '@/api/report'

const reports = ref([])
const detail = ref(null)
const currentReport = ref(null)
const experimentId = ref('')
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const detailVisible = ref(false)
const gradeVisible = ref(false)
const returnVisible = ref(false)
const returnComment = ref('')
const gradeForm = reactive({ score: 80, comment: '' })

onMounted(loadReports)

async function loadReports() {
  loading.value = true
  try {
    const result = await getPendingReports({
      pageNum: pageNum.value,
      pageSize: 10,
      experimentId: experimentId.value || undefined,
    })
    reports.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getReportDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

function openGrade(row) {
  currentReport.value = row
  Object.assign(gradeForm, { score: row.latestScore || 80, comment: row.latestComment || '' })
  gradeVisible.value = true
}

function openReturn(row) {
  currentReport.value = row
  returnComment.value = row.latestComment || ''
  returnVisible.value = true
}

async function submitGrade() {
  saving.value = true
  try {
    await gradeReport(currentReport.value.id, gradeForm)
    ElMessage.success('报告已评分')
    gradeVisible.value = false
    await loadReports()
  } finally {
    saving.value = false
  }
}

async function submitReturn() {
  if (!returnComment.value.trim()) {
    ElMessage.warning('请填写退回原因')
    return
  }
  saving.value = true
  try {
    await returnReport(currentReport.value.id, { comment: returnComment.value.trim() })
    ElMessage.success('报告已退回')
    returnVisible.value = false
    await loadReports()
  } finally {
    saving.value = false
  }
}

function reportStatusMeta(status) {
  return {
    DRAFT: { label: '草稿', type: 'info' },
    SUBMITTED: { label: '待批改', type: 'warning' },
    GRADED: { label: '已批改', type: 'success' },
    RETURNED: { label: '退回修改', type: 'danger' },
  }[status] || { label: status || '未知', type: 'info' }
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
.toolbar .el-input { max-width: 180px; }
.pagination-row { color: #667085; padding-top: 14px; }
.detail-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; margin-bottom: 10px; }
.detail-head h2 { color: #13233a; font-size: 20px; }
.detail-meta { color: #667085; margin-bottom: 14px; }
.report-content { white-space: pre-wrap; color: #344054; line-height: 1.8; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 14px; }
.detail-body { min-height: 180px; }
@media (max-width: 720px) {
  .page-head, .toolbar, .pagination-row { align-items: stretch; flex-direction: column; }
  .toolbar .el-input { max-width: none; }
}
</style>
