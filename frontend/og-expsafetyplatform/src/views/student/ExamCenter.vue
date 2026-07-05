<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Safety Exams</p>
        <h1>安全考试</h1>
        <p class="page-desc">完成实验安全考核，查看历史成绩和错题解析。</p>
      </div>
      <el-button :icon="Refresh" @click="reloadActiveTab">刷新</el-button>
    </section>

    <el-tabs v-model="activeTab" class="tabs" @tab-change="reloadActiveTab">
      <el-tab-pane label="可参加考试" name="available">
        <section class="panel">
          <div class="toolbar">
            <el-input v-model="courseId" clearable placeholder="按课程ID筛选" />
            <el-button type="primary" :icon="Search" @click="loadAvailable">查询</el-button>
          </div>
          <el-table v-loading="availableLoading" :data="availablePapers" stripe>
            <el-table-column prop="title" label="试卷名称" min-width="190" />
            <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column prop="totalScore" label="总分" width="80" />
            <el-table-column prop="passScore" label="及格分" width="90" />
            <el-table-column prop="duration" label="时长(分钟)" width="110" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" :icon="VideoPlay" @click="handleStart(row)">开始考试</el-button>
              </template>
            </el-table-column>
          </el-table>
          <PaginationBar :total="availableTotal" v-model:page="availablePage" @change="loadAvailable" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="考试记录" name="records">
        <section class="panel">
          <div class="toolbar">
            <el-select v-model="recordStatus" clearable placeholder="考试状态">
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已提交" value="SUBMITTED" />
              <el-option label="已复核" value="REVIEWED" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="loadRecords">查询</el-button>
          </div>
          <el-table v-loading="recordsLoading" :data="records" stripe>
            <el-table-column prop="paperId" label="试卷ID" width="90" />
            <el-table-column prop="totalScore" label="总分" width="90" />
            <el-table-column prop="objectiveScore" label="客观题" width="90" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="examStatusMeta(row.status).type">{{ examStatusMeta(row.status).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="是否通过" width="100">
              <template #default="{ row }">
                <el-tag :type="row.passed ? 'success' : 'danger'">{{ row.passed ? '通过' : '未通过' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" min-width="160" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" :icon="View" @click="openRecord(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <PaginationBar :total="recordsTotal" v-model:page="recordsPage" @change="loadRecords" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="错题解析" name="wrong">
        <section class="panel">
          <div class="toolbar">
            <el-select v-model="wrongType" clearable placeholder="题型">
              <el-option label="单选题" value="SINGLE" />
              <el-option label="多选题" value="MULTIPLE" />
              <el-option label="判断题" value="JUDGE" />
              <el-option label="简答题" value="SHORT_ANSWER" />
            </el-select>
            <el-input v-model="wrongCourseId" clearable placeholder="课程ID" />
            <el-button type="primary" :icon="Search" @click="loadWrongQuestions">查询</el-button>
          </div>
          <el-table v-loading="wrongLoading" :data="wrongQuestions" stripe>
            <el-table-column label="题目" min-width="240">
              <template #default="{ row }">{{ row.question?.content || '-' }}</template>
            </el-table-column>
            <el-table-column prop="wrongAnswer" label="我的答案" min-width="120" />
            <el-table-column prop="correctAnswer" label="正确答案" min-width="120" />
            <el-table-column label="解析" min-width="220">
              <template #default="{ row }">{{ row.analysis || row.question?.analysis || '暂无解析' }}</template>
            </el-table-column>
          </el-table>
          <PaginationBar :total="wrongTotal" v-model:page="wrongPage" @change="loadWrongQuestions" />
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="examVisible" :title="currentPaper?.title || '考试答题'" width="860px" top="4vh">
      <div class="exam-meta">
        <el-tag type="primary">记录ID {{ examSession.recordId }}</el-tag>
        <el-tag type="warning">时长 {{ examSession.duration || 0 }} 分钟</el-tag>
        <span>结束时间：{{ examSession.endTime || '-' }}</span>
      </div>
      <div class="question-list">
        <div v-for="(question, index) in examSession.questions" :key="question.id" class="question-box">
          <div class="question-title">
            <strong>{{ index + 1 }}. {{ question.content }}</strong>
            <el-tag size="small">{{ questionTypeLabel(question.type) }} / {{ question.score || 0 }} 分</el-tag>
          </div>
          <el-radio-group v-if="['SINGLE', 'JUDGE'].includes(question.type)" v-model="answers[question.id]">
            <el-radio v-for="option in normalizedOptions(question)" :key="option.value" :label="option.value">
              {{ option.label }}
            </el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="question.type === 'MULTIPLE'" v-model="answers[question.id]">
            <el-checkbox v-for="option in normalizedOptions(question)" :key="option.value" :label="option.value">
              {{ option.label }}
            </el-checkbox>
          </el-checkbox-group>
          <el-input
            v-else
            v-model="answers[question.id]"
            type="textarea"
            :rows="3"
            placeholder="请输入答案"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="examVisible = false">稍后提交</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交试卷</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordVisible" title="考试详情" width="820px">
      <div v-loading="recordDetailLoading" class="detail-body">
        <template v-if="recordDetail">
          <div class="summary-grid">
            <div><strong>{{ recordDetail.record?.totalScore || 0 }}</strong><span>总分</span></div>
            <div><strong>{{ recordDetail.record?.objectiveScore || 0 }}</strong><span>客观题</span></div>
            <div><strong>{{ recordDetail.record?.passed ? '通过' : '未通过' }}</strong><span>结果</span></div>
          </div>
          <el-table :data="recordDetail.answers || []" stripe>
            <el-table-column label="题目" min-width="220">
              <template #default="{ row }">{{ row.question?.content || '-' }}</template>
            </el-table-column>
            <el-table-column prop="studentAnswer" label="我的答案" min-width="120" />
            <el-table-column prop="correctAnswer" label="正确答案" min-width="120" />
            <el-table-column label="得分" width="90">
              <template #default="{ row }">{{ row.score || 0 }}</template>
            </el-table-column>
            <el-table-column label="解析" min-width="200">
              <template #default="{ row }">{{ row.analysis || '暂无解析' }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { h, onMounted, reactive, ref, resolveComponent } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, VideoPlay, View } from '@element-plus/icons-vue'
import {
  getAvailableExams,
  getExamRecordDetail,
  getExamRecords,
  getWrongQuestions,
  startExam,
  submitExam,
} from '@/api/exam'

const PaginationBar = {
  props: { total: Number, page: Number },
  emits: ['update:page', 'change'],
  setup(props, { emit }) {
    return () => h('div', { class: 'pagination-row' }, [
      h('span', `共 ${props.total || 0} 条记录`),
      h(resolveComponent('ElPagination'), {
        currentPage: props.page,
        'onUpdate:currentPage': (value) => emit('update:page', value),
        pageSize: 10,
        total: props.total || 0,
        layout: 'prev, pager, next',
        onCurrentChange: () => emit('change'),
      }),
    ])
  },
}

const activeTab = ref('available')
const courseId = ref('')
const recordStatus = ref('')
const wrongType = ref('')
const wrongCourseId = ref('')

const availableLoading = ref(false)
const recordsLoading = ref(false)
const wrongLoading = ref(false)
const recordDetailLoading = ref(false)
const submitting = ref(false)

const availablePapers = ref([])
const records = ref([])
const wrongQuestions = ref([])
const recordDetail = ref(null)
const currentPaper = ref(null)
const availablePage = ref(1)
const recordsPage = ref(1)
const wrongPage = ref(1)
const availableTotal = ref(0)
const recordsTotal = ref(0)
const wrongTotal = ref(0)

const examVisible = ref(false)
const recordVisible = ref(false)
const examSession = reactive({ recordId: null, questions: [], duration: 0, endTime: '' })
const answers = reactive({})

onMounted(loadAvailable)

async function reloadActiveTab() {
  if (activeTab.value === 'available') await loadAvailable()
  if (activeTab.value === 'records') await loadRecords()
  if (activeTab.value === 'wrong') await loadWrongQuestions()
}

async function loadAvailable() {
  availableLoading.value = true
  try {
    const result = await getAvailableExams({
      pageNum: availablePage.value,
      pageSize: 10,
      courseId: courseId.value || undefined,
    })
    availablePapers.value = result?.records || []
    availableTotal.value = result?.total || 0
  } finally {
    availableLoading.value = false
  }
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    const result = await getExamRecords({
      pageNum: recordsPage.value,
      pageSize: 10,
      status: recordStatus.value || undefined,
    })
    records.value = result?.records || []
    recordsTotal.value = result?.total || 0
  } finally {
    recordsLoading.value = false
  }
}

async function loadWrongQuestions() {
  wrongLoading.value = true
  try {
    const result = await getWrongQuestions({
      pageNum: wrongPage.value,
      pageSize: 10,
      type: wrongType.value || undefined,
      courseId: wrongCourseId.value || undefined,
    })
    wrongQuestions.value = result?.records || []
    wrongTotal.value = result?.total || 0
  } finally {
    wrongLoading.value = false
  }
}

async function handleStart(paper) {
  currentPaper.value = paper
  const result = await startExam(paper.id)
  Object.assign(examSession, {
    recordId: result?.recordId,
    questions: result?.questions || [],
    duration: result?.duration || paper.duration,
    endTime: result?.endTime,
  })
  Object.keys(answers).forEach((key) => delete answers[key])
  examSession.questions.forEach((question) => {
    answers[question.id] = question.type === 'MULTIPLE' ? [] : ''
  })
  examVisible.value = true
}

async function handleSubmit() {
  const payload = examSession.questions.map((question) => ({
    questionId: question.id,
    answer: Array.isArray(answers[question.id]) ? answers[question.id].join(',') : answers[question.id],
  }))
  submitting.value = true
  try {
    const result = await submitExam(examSession.recordId, { answers: payload })
    examVisible.value = false
    ElMessageBox.alert(
      `总分：${result?.totalScore || 0}，正确题数：${result?.correctCount || 0}/${result?.totalCount || 0}，结果：${result?.passed ? '通过' : '未通过'}`,
      '提交成功',
      { type: result?.passed ? 'success' : 'warning' },
    )
    await Promise.allSettled([loadAvailable(), loadRecords()])
  } finally {
    submitting.value = false
  }
}

async function openRecord(row) {
  recordVisible.value = true
  recordDetailLoading.value = true
  recordDetail.value = null
  try {
    recordDetail.value = await getExamRecordDetail(row.id)
  } finally {
    recordDetailLoading.value = false
  }
}

function normalizedOptions(question) {
  if (question.type === 'JUDGE') {
    return [{ value: 'true', label: '正确' }, { value: 'false', label: '错误' }]
  }
  const raw = question.options
  if (!raw) return []
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (Array.isArray(parsed)) {
      return parsed.map((item, index) => optionFromValue(item, index))
    }
    return Object.entries(parsed).map(([value, label]) => ({ value, label: `${value}. ${label}` }))
  } catch {
    return String(raw).split(/\n|;/).filter(Boolean).map((item, index) => optionFromValue(item, index))
  }
}

function optionFromValue(item, index) {
  if (typeof item === 'object') {
    return { value: item.value || item.key || String.fromCharCode(65 + index), label: item.label || item.text || item.content }
  }
  const value = String(item).match(/^([A-Z])[\.\、:：]/)?.[1] || String.fromCharCode(65 + index)
  return { value, label: String(item) }
}

function questionTypeLabel(type) {
  return { SINGLE: '单选题', MULTIPLE: '多选题', JUDGE: '判断题', SHORT_ANSWER: '简答题' }[type] || type || '题目'
}

function examStatusMeta(status) {
  return {
    IN_PROGRESS: { label: '进行中', type: 'warning' },
    SUBMITTED: { label: '已提交', type: 'primary' },
    REVIEWED: { label: '已复核', type: 'success' },
  }[status] || { label: status || '未知', type: 'info' }
}
</script>

<style scoped>
.business-page { max-width: 1240px; margin: 0 auto; }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.tabs { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.panel { min-height: 320px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.toolbar .el-input, .toolbar .el-select { max-width: 220px; }
.pagination-row { display: flex; justify-content: space-between; align-items: center; color: #667085; padding-top: 14px; }
.exam-meta { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 14px; }
.question-list { max-height: 58vh; overflow: auto; padding-right: 6px; }
.question-box { border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; margin-bottom: 12px; }
.question-title { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 12px; color: #13233a; }
.detail-body { min-height: 180px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 14px; }
.summary-grid div { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.summary-grid strong { display: block; color: #13233a; font-size: 22px; }
.summary-grid span { color: #7b8794; font-size: 12px; }
@media (max-width: 720px) {
  .page-head, .pagination-row { align-items: stretch; flex-direction: column; }
  .toolbar { align-items: stretch; flex-direction: column; }
  .toolbar .el-input, .toolbar .el-select { max-width: none; }
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
