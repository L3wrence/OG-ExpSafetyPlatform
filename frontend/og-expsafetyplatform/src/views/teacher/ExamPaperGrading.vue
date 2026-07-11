<template>
  <div class="grading-page">
    <header class="page-head">
      <div>
        <h1>{{ paper?.title || '试卷批改' }}</h1>
        <p>查看学生提交的完整答卷，并批改待评分主观题。</p>
      </div>
      <div class="head-actions">
        <el-button size="small" :icon="Back" @click="router.push(`/teacher/courses/${courseId}/edit?step=exam`)">返回试卷列表</el-button>
        <el-button size="small" :icon="Refresh" @click="loadSubmissions">刷新</el-button>
      </div>
    </header>

    <section class="list-panel">
      <div class="summary-row">
        <div><span>提交人数</span><strong>{{ total }}</strong></div>
        <div><span>待批改</span><strong>{{ pendingTotal }}</strong></div>
        <div><span>及格分</span><strong>{{ paper?.passScore ?? '-' }}</strong></div>
      </div>
      <el-table v-loading="loading" :data="submissions" stripe empty-text="暂无学生提交记录" @row-click="openRecord">
        <el-table-column prop="studentUsername" label="学号" width="140" />
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column prop="objectiveScore" label="客观题" width="90" />
        <el-table-column label="主观题" width="90"><template #default="{ row }">{{ isPending(row) ? '待批改' : (row.subjectiveScore ?? 0) }}</template></el-table-column>
        <el-table-column label="总分" width="90"><template #default="{ row }">{{ isPending(row) ? '待定' : (row.totalScore ?? 0) }}</template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column label="结果" width="100"><template #default="{ row }"><el-tag :type="resultMeta(row).type">{{ resultMeta(row).label }}</el-tag></template></el-table-column>
        <el-table-column prop="submitTime" label="交卷时间" min-width="170" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }"><el-button text type="primary" @click.stop="openRecord(row)">{{ isPending(row) ? '批改' : '查看' }}</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > pageSize" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadSubmissions" />
    </section>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="72%" destroy-on-close>
      <div v-loading="detailLoading" class="paper-review">
        <section v-if="recordDetail" class="record-summary">
          <span>客观题得分<strong>{{ recordDetail.record?.objectiveScore ?? 0 }}</strong></span>
          <span>主观题得分<strong>{{ recordDetail.record?.subjectiveScore ?? (recordDetail.pendingCount ? '待批改' : 0) }}</strong></span>
          <span>总分<strong>{{ recordDetail.pendingCount ? '待定' : (recordDetail.record?.totalScore ?? 0) }}</strong></span>
          <span>结果<strong>{{ resultMeta(recordDetail.record || {}).label }}</strong></span>
        </section>

        <article v-for="(item, index) in recordDetail?.answers || []" :key="item.questionId" class="answer-card">
          <div class="answer-head">
            <div><span>{{ index + 1 }}</span><strong>{{ typeLabel(item.type) }} · {{ item.maxScore }}分</strong></div>
            <el-tag :type="answerTag(item).type">{{ answerTag(item).label }}</el-tag>
          </div>
          <p class="question-content">{{ item.content }}</p>
          <div class="answer-grid">
            <div><span>学生答案</span><p>{{ formatAnswer(item.studentAnswer) }}</p></div>
            <div><span>参考答案</span><p>{{ formatAnswer(item.referenceAnswer) }}</p></div>
          </div>
          <p v-if="item.analysis" class="analysis"><b>评分参考：</b>{{ item.analysis }}</p>
          <div v-if="item.type === 'SHORT_ANSWER'" class="grade-fields">
            <el-form-item label="得分" required>
              <el-input-number v-model="gradeForms[item.answerId].score" :min="0" :max="item.maxScore" :disabled="item.isCorrect !== null" />
              <span class="max-score">/ {{ item.maxScore }} 分</span>
            </el-form-item>
            <el-form-item label="批注">
              <el-input v-model="gradeForms[item.answerId].comment" type="textarea" :rows="2" :disabled="item.isCorrect !== null" placeholder="填写给学生的批改意见" />
            </el-form-item>
          </div>
        </article>
      </div>
      <template #footer>
        <el-button @click="drawerVisible = false">关闭</el-button>
        <el-button v-if="recordDetail?.pendingCount" type="primary" :loading="grading" @click="submitGrades">提交批改</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Refresh } from '@element-plus/icons-vue'
import { getExamPaperDetail } from '@/api/examPaper'
import { getGradingRecordDetail, getPaperSubmissionRecords, gradeShortAnswers } from '@/api/exam'

const route = useRoute(), router = useRouter()
const courseId = computed(() => Number(route.params.courseId)), paperId = computed(() => Number(route.params.paperId))
const paper = ref(null), submissions = ref([]), total = ref(0), pageNum = ref(1), pageSize = 20
const loading = ref(false), detailLoading = ref(false), drawerVisible = ref(false), grading = ref(false)
const recordDetail = ref(null), gradeForms = reactive({})
const pendingTotal = computed(() => submissions.value.filter(isPending).length)
const drawerTitle = computed(() => recordDetail.value ? `${recordDetail.value.studentName || recordDetail.value.studentUsername || '学生'}的答卷` : '学生答卷')

onMounted(async () => {
  const detail = await getExamPaperDetail(paperId.value)
  if (!detail?.paper || Number(detail.paper.courseId) !== courseId.value) {
    ElMessage.error('试卷不存在或不属于当前课堂')
    return router.push(`/teacher/courses/${courseId.value}/edit?step=exam`)
  }
  paper.value = detail.paper
  await loadSubmissions()
})

async function loadSubmissions() {
  loading.value = true
  try {
    const result = await getPaperSubmissionRecords({ paperId: paperId.value, pageNum: pageNum.value, pageSize })
    submissions.value = result?.records || []
    total.value = Number(result?.total || 0)
  } finally { loading.value = false }
}

async function openRecord(row) {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    const detail = await getGradingRecordDetail(row.recordId)
    recordDetail.value = detail
    Object.keys(gradeForms).forEach((key) => delete gradeForms[key])
    ;(detail?.answers || []).filter((item) => item.type === 'SHORT_ANSWER' && item.answerId).forEach((item) => {
      gradeForms[item.answerId] = { score: Number(item.score || 0), comment: item.gradingComment || '' }
    })
  } finally { detailLoading.value = false }
}

async function submitGrades() {
  const pendingAnswers = (recordDetail.value?.answers || []).filter((item) => item.type === 'SHORT_ANSWER' && item.isCorrect === null)
  const grades = pendingAnswers.map((item) => ({ answerId: item.answerId, score: Number(gradeForms[item.answerId]?.score || 0), comment: gradeForms[item.answerId]?.comment || '' }))
  if (!grades.length) return
  await ElMessageBox.confirm('提交后将计算该生最终总分和是否通过，确认提交批改吗？', '确认批改', { type: 'warning' })
  grading.value = true
  try {
    const result = await gradeShortAnswers(recordDetail.value.record.id, grades)
    ElMessage.success(`批改完成，总分 ${result?.totalScore ?? 0}，${result?.passed ? '已通过' : '未通过'}`)
    await loadSubmissions()
    await openRecord({ recordId: recordDetail.value.record.id })
  } finally { grading.value = false }
}

function isPending(row) { return row?.status === 'PENDING_REVIEW' || Number(row?.pendingCount || 0) > 0 }
function statusMeta(status) { return { PENDING_REVIEW: { label: '待批改', type: 'warning' }, GRADED: { label: '已批改', type: 'success' }, EXPIRED: { label: '已超时', type: 'info' }, SUBMITTED: { label: '已提交', type: 'primary' }, REVIEWED: { label: '已批改', type: 'success' } }[status] || { label: status || '-', type: 'info' } }
function resultMeta(row) { if (isPending(row) || row?.passed === null || row?.passed === undefined) return { label: '待批改', type: 'warning' }; return row.passed ? { label: '通过', type: 'success' } : { label: '未通过', type: 'danger' } }
function answerTag(item) { if (item.type === 'SHORT_ANSWER' && item.isCorrect === null) return { label: '待批改', type: 'warning' }; if (item.type === 'SHORT_ANSWER') return { label: `${item.score ?? 0}/${item.maxScore}分`, type: 'primary' }; return item.isCorrect ? { label: '正确', type: 'success' } : { label: '错误', type: 'danger' } }
function typeLabel(type) { return { SINGLE: '单选题', MULTIPLE: '多选题', JUDGE: '判断题', SHORT_ANSWER: '主观题' }[type] || type || '-' }
function formatAnswer(value) { return value === null || value === undefined || value === '' ? '未作答' : String(value) }
</script>

<style scoped>
.grading-page { display: grid; grid-template-rows: auto minmax(0, 1fr); align-content: start; gap: 12px; min-height: calc(100vh - 104px); }.page-head,.list-panel { padding: 20px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; }.page-head { position: sticky; top: 0; z-index: 10; min-height: 0; padding: 10px 16px; }.page-head,.head-actions,.summary-row,.record-summary,.answer-head,.answer-head>div,.answer-grid { display: flex; align-items: center; gap: 12px; }.page-head,.answer-head { justify-content: space-between; }.page-head h1 { margin: 0; font-size: 18px; line-height: 1.35; color: #102a43; letter-spacing: 0; }.page-head p { margin: 3px 0 0; color: #64748b; font-size: 13px; line-height: 1.4; }.summary-row { margin-bottom: 18px; gap: 36px; }.summary-row div,.record-summary span { color: #64748b; }.summary-row strong,.record-summary strong { display: block; margin-top: 3px; font-size: 20px; color: #102a43; }.el-pagination { justify-content: flex-end; margin-top: 18px; }.paper-review { padding: 0 8px 20px; }.record-summary { padding: 14px 16px; margin-bottom: 14px; background: #f7f8fa; border: 1px solid #e5e7eb; border-radius: 6px; gap: 42px; }.answer-card { padding: 18px; margin-bottom: 12px; border: 1px solid #e5e7eb; border-radius: 6px; }.answer-head>div>span { display: grid; place-items: center; width: 28px; height: 28px; color: #fff; background: #8a2f3c; border-radius: 4px; }.question-content { margin: 16px 0; color: #1f2937; line-height: 1.7; }.answer-grid { align-items: stretch; }.answer-grid>div { flex: 1; padding: 12px; background: #f7f8fa; }.answer-grid span { color: #64748b; font-size: 13px; }.answer-grid p { margin: 6px 0 0; line-height: 1.6; }.analysis { padding: 10px 12px; color: #475569; background: #fff8e8; }.grade-fields { padding-top: 14px; border-top: 1px solid #e5e7eb; }.max-score { margin-left: 8px; color: #64748b; }
@media (max-width: 760px) { .page-head,.head-actions,.record-summary,.answer-grid { align-items: stretch; flex-direction: column; }.summary-row { flex-wrap: wrap; } }
</style>
