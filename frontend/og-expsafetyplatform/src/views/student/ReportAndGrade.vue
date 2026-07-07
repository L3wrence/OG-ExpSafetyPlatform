<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Reports & Scores</p>
        <h1>成绩反馈</h1>
        <p class="page-desc">集中查看考试成绩、实验报告状态，并提交或更新实验报告。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建报告</el-button>
    </section>

    <section class="grid">
      <div class="panel">
        <div class="section-title"><h2>考试成绩</h2><el-button :icon="Refresh" @click="loadExamRecords">刷新</el-button></div>
        <el-table v-loading="examLoading" :data="examRecords" stripe>
          <el-table-column prop="paperId" label="试卷ID" width="90" />
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column prop="objectiveScore" label="客观题" width="90" />
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.passed ? 'success' : 'danger'">{{ row.passed ? '通过' : '未通过' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" label="提交时间" min-width="160" />
        </el-table>
      </div>

      <div class="panel">
        <div class="section-title">
          <h2>我的报告</h2>
          <el-select v-model="reportStatus" clearable placeholder="状态" @change="loadReports">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待批改" value="SUBMITTED" />
            <el-option label="已批改" value="GRADED" />
            <el-option label="退回修改" value="RETURNED" />
          </el-select>
        </div>
        <el-table v-loading="reportLoading" :data="reports" stripe>
          <el-table-column prop="title" label="报告标题" min-width="160" />
          <el-table-column prop="experimentId" label="实验ID" width="90" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="reportStatusMeta(row.status).type">{{ reportStatusMeta(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="latestScore" label="成绩" width="80" />
          <el-table-column prop="latestComment" label="评语" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openDetail(row)">反馈</el-button>
              <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="['DRAFT', 'RETURNED'].includes(row.status)" text type="success" @click="handleSubmit(row)">提交</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-row">
          <span>共 {{ reportTotal }} 条记录</span>
          <el-pagination v-model:current-page="reportPage" layout="prev, pager, next" :page-size="10" :total="reportTotal" @current-change="loadReports" />
        </div>
      </div>
    </section>

    <el-dialog v-model="reportVisible" :title="editingReport ? '编辑报告' : '新建报告'" width="680px">
      <el-form :model="reportForm" label-width="92px">
        <el-form-item label="实验ID" required>
          <el-input v-model="reportForm.experimentId" placeholder="请输入实验项目ID" />
        </el-form-item>
        <el-form-item label="报告标题" required>
          <el-input v-model="reportForm.title" placeholder="请输入报告标题" />
        </el-form-item>
        <el-form-item label="文件地址">
          <el-input v-model="reportForm.fileUrl" placeholder="可选，填写附件或报告文件URL" />
        </el-form-item>
        <el-form-item label="报告内容" required>
          <el-input v-model="reportForm.content" type="textarea" :rows="10" placeholder="填写实验目的、步骤、数据记录、分析结论和安全反思" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveReport">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="报告反馈" width="760px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <h2>{{ detailReport.title }}</h2>
          <p class="detail-meta">状态：{{ reportStatusMeta(detailReport.status).label }}　实验ID：{{ detailReport.experimentId }}</p>
          <div v-if="detail.template" class="feedback-box">
            <b>{{ detail.template.title }}</b>
            <p>{{ detail.template.schemaJson }}</p>
          </div>
          <div v-if="detail.rubric?.length" class="feedback-box">
            <b>评分量规</b>
            <el-table :data="detail.rubric" size="small">
              <el-table-column prop="itemName" label="评分项" />
              <el-table-column prop="maxScore" label="满分" width="90" />
              <el-table-column prop="description" label="说明" />
            </el-table>
          </div>
          <div v-if="detail.scoreItems?.length" class="feedback-box">
            <b>逐项得分</b>
            <el-table :data="detail.scoreItems" size="small">
              <el-table-column prop="rubricItemId" label="评分项ID" width="100" />
              <el-table-column prop="score" label="得分" width="90" />
              <el-table-column prop="comment" label="教师反馈" />
            </el-table>
          </div>
          <div v-if="detail.latestScore" class="feedback-box">
            <b>总评</b>
            <p>{{ detail.latestScore.score }} 分 · {{ detail.latestScore.comment || '暂无评语' }}</p>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getExamRecords } from '@/api/exam'
import { createReport, getMyReports, getReportDetail, submitReport, updateReport } from '@/api/report'

const examRecords = ref([])
const reports = ref([])
const examLoading = ref(false)
const reportLoading = ref(false)
const saving = ref(false)
const reportVisible = ref(false)
const detailVisible = ref(false)
const editingReport = ref(null)
const detail = ref(null)
const detailLoading = ref(false)
const reportStatus = ref('')
const reportPage = ref(1)
const reportTotal = ref(0)
const reportForm = reactive({ experimentId: '', title: '', content: '', fileUrl: '' })
const detailReport = computed(() => detail.value?.report || {})

onMounted(() => Promise.allSettled([loadExamRecords(), loadReports()]))

async function loadExamRecords() {
  examLoading.value = true
  try {
    const result = await getExamRecords({ pageNum: 1, pageSize: 8 })
    examRecords.value = result?.records || []
  } finally {
    examLoading.value = false
  }
}

async function loadReports() {
  reportLoading.value = true
  try {
    const result = await getMyReports({
      pageNum: reportPage.value,
      pageSize: 10,
      status: reportStatus.value || undefined,
    })
    reports.value = result?.records || []
    reportTotal.value = result?.total || 0
  } finally {
    reportLoading.value = false
  }
}

function openCreate() {
  editingReport.value = null
  Object.assign(reportForm, { experimentId: '', title: '', content: '', fileUrl: '' })
  reportVisible.value = true
}

function openEdit(row) {
  editingReport.value = row
  Object.assign(reportForm, {
    experimentId: row.experimentId || '',
    title: row.title || '',
    content: row.content || '',
    fileUrl: row.fileUrl || '',
  })
  reportVisible.value = true
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

async function saveReport() {
  if (!reportForm.experimentId || !reportForm.title.trim() || !reportForm.content.trim()) {
    ElMessage.warning('请完整填写实验ID、标题和内容')
    return
  }
  const payload = {
    experimentId: Number(reportForm.experimentId),
    title: reportForm.title.trim(),
    content: reportForm.content.trim(),
    fileUrl: reportForm.fileUrl || undefined,
  }
  saving.value = true
  try {
    if (editingReport.value) {
      await updateReport(editingReport.value.id, payload)
      ElMessage.success('报告已更新')
    } else {
      await createReport(payload)
      ElMessage.success('报告已创建')
    }
    reportVisible.value = false
    await loadReports()
  } finally {
    saving.value = false
  }
}

async function handleSubmit(row) {
  await submitReport(row.id)
  ElMessage.success('报告已提交')
  await loadReports()
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
.page-head, .section-title, .pagination-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.page-head { align-items: flex-end; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.grid { display: grid; grid-template-columns: minmax(360px, 0.8fr) minmax(0, 1.2fr); gap: 16px; align-items: start; }
.panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.section-title { margin-bottom: 12px; }
.section-title h2 { color: #13233a; font-size: 18px; }
.pagination-row { color: #667085; padding-top: 14px; }
.detail-body { min-height: 220px; }
.detail-body h2 { color: #13233a; font-size: 20px; margin-bottom: 8px; }
.detail-meta { color: #667085; margin-bottom: 12px; }
.feedback-box { border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; margin-bottom: 12px; background: #f8fafc; }
.feedback-box b { display: block; color: #13233a; margin-bottom: 8px; }
.feedback-box p { color: #344054; line-height: 1.7; white-space: pre-wrap; margin: 0; }
@media (max-width: 1040px) { .grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .page-head, .section-title, .pagination-row { align-items: stretch; flex-direction: column; } }
</style>
