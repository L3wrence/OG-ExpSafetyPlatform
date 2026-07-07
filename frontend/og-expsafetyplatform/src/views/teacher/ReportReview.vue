<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">AmazingTeaching</p>
        <h1>报告批改</h1>
        <p class="page-desc">完成油气实验报告评分、逐项反馈和成绩闭环，帮助学生复盘实验数据与安全反思。</p>
      </div>
      <el-button :icon="Refresh" @click="loadReports">刷新</el-button>
    </section>

    <section class="panel rubric-panel">
      <div class="section-title">
        <h2>报告模板与评分量规</h2>
        <el-button :icon="Search" @click="loadRubricConfig">加载配置</el-button>
      </div>
      <el-form :model="templateForm" label-width="88px">
        <el-form-item label="实验ID">
          <el-input v-model="configExperimentId" placeholder="输入实验ID后维护模板和量规" />
        </el-form-item>
        <el-form-item label="模板标题">
          <el-input v-model="templateForm.title" placeholder="例如：钻井液性能测试实验报告模板" />
        </el-form-item>
        <el-form-item label="模板结构">
          <el-input v-model="templateForm.schemaJson" type="textarea" :rows="4" placeholder='可填写 JSON 或结构说明，例如 {"sections":["实验目的","数据记录","误差分析"]}' />
        </el-form-item>
      </el-form>
      <div class="rubric-list">
        <div v-for="(item, index) in rubricItems" :key="index" class="rubric-row">
          <el-input v-model="item.itemName" placeholder="评分项" />
          <el-input-number v-model="item.maxScore" :min="1" :max="100" />
          <el-input v-model="item.description" placeholder="评分说明" />
          <el-button text type="danger" @click="rubricItems.splice(index, 1)">移除</el-button>
        </div>
      </div>
      <div class="rubric-actions">
        <el-button @click="addRubricItem">添加评分项</el-button>
        <el-button type="primary" :loading="configSaving" @click="saveRubricConfig">保存模板和量规</el-button>
      </div>
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
        <el-table-column label="成绩" width="90">
          <template #default="{ row }">{{ row.latestScore != null ? `${row.latestScore}分` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="latestComment" label="最近反馈" min-width="170" show-overflow-tooltip />
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
            <h2>{{ detailReport.title }}</h2>
            <el-tag :type="reportStatusMeta(detailReport.status).type">{{ reportStatusMeta(detailReport.status).label }}</el-tag>
          </div>
          <p class="detail-meta">学生：{{ detailReport.studentName || detailReport.studentId }}　实验ID：{{ detailReport.experimentId }}</p>
          <div class="feedback-summary">
            <div><strong>{{ detailReport.latestScore != null ? `${detailReport.latestScore}分` : '待评分' }}</strong><span>当前成绩</span></div>
            <div><strong>{{ detail.scoreItems?.length || 0 }}</strong><span>逐项反馈</span></div>
            <p>{{ detailReport.latestComment || '暂无总评，评分后学生将在实验路径中看到反馈。' }}</p>
          </div>
          <p class="report-content">{{ detailReport.content }}</p>
          <el-link v-if="detailReport.fileUrl" :href="detailReport.fileUrl" target="_blank" type="primary">查看附件</el-link>
          <div v-if="detail.rubric?.length" class="score-detail">
            <h3>评分量规</h3>
            <el-table :data="detail.rubric" size="small">
              <el-table-column prop="itemName" label="评分项" />
              <el-table-column prop="maxScore" label="满分" width="90" />
              <el-table-column prop="description" label="说明" />
            </el-table>
          </div>
          <div v-if="detail.scoreItems?.length" class="score-detail">
            <h3>逐项反馈</h3>
            <el-table :data="detail.scoreItems" size="small">
              <el-table-column prop="rubricItemId" label="评分项ID" width="100" />
              <el-table-column prop="score" label="得分" width="90" />
              <el-table-column prop="comment" label="反馈" />
            </el-table>
          </div>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="gradeVisible" title="报告评分" width="520px">
      <div v-if="currentRubric.length" class="grade-summary">
        <strong>量规总分 {{ rubricTotalScore }} 分</strong>
        <span>逐项评分会汇总为学生的实验成绩反馈。</span>
      </div>
      <el-form :model="gradeForm" label-width="88px">
        <template v-if="currentRubric.length">
          <el-form-item v-for="item in currentRubric" :key="item.id" :label="item.itemName">
            <div class="grade-item">
              <el-input-number v-model="rubricScoreMap[item.id].score" :min="0" :max="item.maxScore" />
              <el-input v-model="rubricScoreMap[item.id].comment" placeholder="单项反馈" />
            </div>
          </el-form-item>
        </template>
        <el-form-item v-else label="分数"><el-input-number v-model="gradeForm.score" :min="0" :max="100" /></el-form-item>
        <el-form-item label="总评"><el-input v-model="gradeForm.comment" type="textarea" :rows="5" /></el-form-item>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getPendingReports, getReportDetail, getReportRubric, getReportTemplate, gradeReport, gradeReportWithRubric, returnReport, saveReportRubric, saveReportTemplate } from '@/api/report'

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
const configExperimentId = ref('')
const configSaving = ref(false)
const templateForm = reactive({ title: '', schemaJson: '', status: 1 })
const rubricItems = ref([])
const currentRubric = ref([])
const rubricScoreMap = reactive({})
const detailReport = computed(() => detail.value?.report || detail.value || {})
const rubricTotalScore = computed(() => currentRubric.value.reduce((sum, item) => sum + Number(item.maxScore || 0), 0))

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

async function openGrade(row) {
  currentReport.value = row
  Object.assign(gradeForm, { score: row.latestScore || 80, comment: row.latestComment || '' })
  currentRubric.value = await getReportRubric(row.experimentId, { silent: true }).catch(() => [])
  Object.keys(rubricScoreMap).forEach((key) => delete rubricScoreMap[key])
  currentRubric.value.forEach((item) => {
    rubricScoreMap[item.id] = { score: 0, comment: '' }
  })
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
    if (currentRubric.value.length) {
      await gradeReportWithRubric(currentReport.value.id, {
        comment: gradeForm.comment,
        items: currentRubric.value.map((item) => ({
          rubricItemId: item.id,
          score: rubricScoreMap[item.id]?.score || 0,
          comment: rubricScoreMap[item.id]?.comment || '',
        })),
      })
    } else {
      await gradeReport(currentReport.value.id, gradeForm)
    }
    ElMessage.success('报告已评分')
    gradeVisible.value = false
    await loadReports()
  } finally {
    saving.value = false
  }
}

async function loadRubricConfig() {
  if (!configExperimentId.value) {
    ElMessage.warning('请先填写实验ID')
    return
  }
  const [template, rubric] = await Promise.all([
    getReportTemplate(configExperimentId.value, { silent: true }).catch(() => null),
    getReportRubric(configExperimentId.value, { silent: true }).catch(() => []),
  ])
  Object.assign(templateForm, {
    title: template?.title || '',
    schemaJson: template?.schemaJson || '',
    status: template?.status ?? 1,
  })
  rubricItems.value = (rubric || []).map((item) => ({ ...item }))
}

function addRubricItem() {
  rubricItems.value.push({
    itemName: '',
    description: '',
    maxScore: 10,
    orderNo: rubricItems.value.length + 1,
  })
}

async function saveRubricConfig() {
  if (!configExperimentId.value || !templateForm.title.trim() || !templateForm.schemaJson.trim()) {
    ElMessage.warning('请填写实验ID、模板标题和模板结构')
    return
  }
  const validItems = rubricItems.value.filter((item) => item.itemName?.trim() && item.maxScore > 0)
  if (validItems.length === 0) {
    ElMessage.warning('至少配置一个有效评分项')
    return
  }
  configSaving.value = true
  try {
    await saveReportTemplate(configExperimentId.value, {
      experimentId: Number(configExperimentId.value),
      title: templateForm.title.trim(),
      schemaJson: templateForm.schemaJson.trim(),
      status: templateForm.status,
    })
    await saveReportRubric(configExperimentId.value, validItems.map((item, index) => ({
      itemName: item.itemName.trim(),
      description: item.description || '',
      maxScore: item.maxScore,
      orderNo: index + 1,
    })))
    ElMessage.success('报告模板和评分量规已保存')
    await loadRubricConfig()
  } finally {
    configSaving.value = false
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
.rubric-panel { margin-bottom: 16px; }
.rubric-list { display: grid; gap: 8px; margin-top: 10px; }
.rubric-row { display: grid; grid-template-columns: minmax(120px, 0.7fr) 120px minmax(180px, 1fr) 64px; gap: 8px; align-items: center; }
.rubric-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px; }
.grade-item { display: grid; grid-template-columns: 120px minmax(0, 1fr); gap: 8px; width: 100%; }
.score-detail { margin-top: 14px; }
.score-detail h3 { color: #13233a; font-size: 16px; margin-bottom: 8px; }
.feedback-summary { display: grid; grid-template-columns: 120px 120px minmax(0, 1fr); gap: 10px; align-items: stretch; margin-bottom: 14px; }
.feedback-summary div, .feedback-summary p, .grade-summary { background: #f0fbf4; border: 1px solid #b7dfc5; border-radius: 8px; padding: 12px; }
.feedback-summary strong { display: block; color: #2d6a4f; font-size: 20px; }
.feedback-summary span, .feedback-summary p, .grade-summary span { color: #536579; line-height: 1.6; margin: 0; }
.grade-summary { display: grid; gap: 4px; margin-bottom: 12px; }
.grade-summary strong { color: #13233a; }
.detail-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; margin-bottom: 10px; }
.detail-head h2 { color: #13233a; font-size: 20px; }
.detail-meta { color: #667085; margin-bottom: 14px; }
.report-content { white-space: pre-wrap; color: #344054; line-height: 1.8; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 14px; }
.detail-body { min-height: 180px; }
@media (max-width: 720px) {
  .page-head, .toolbar, .pagination-row { align-items: stretch; flex-direction: column; }
  .toolbar .el-input { max-width: none; }
  .rubric-row, .grade-item, .feedback-summary { grid-template-columns: 1fr; }
}
</style>
