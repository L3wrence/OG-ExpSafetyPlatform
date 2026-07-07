<template>
  <div class="learning-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">AmazingTeaching</p>
        <h1>{{ courseTitle }}</h1>
        <p class="page-desc">油气工程实验教学与考核路径：预习资源、风险认知、安全准入、实验预约、报告复盘和成绩反馈。</p>
      </div>
      <div class="head-actions">
        <div class="course-progress">
          <strong>{{ courseProgress }}%</strong>
          <span>课程总体进度</span>
        </div>
        <el-button v-if="nextTask" type="primary" :icon="VideoPlay" @click="goContinue">继续学习</el-button>
        <el-button :icon="Back" @click="router.push('/student/courses')">返回课程</el-button>
      </div>
    </section>

    <section class="experiment-hero" :style="{ backgroundImage: `url(${experimentVisual})` }">
      <div>
        <p>{{ detail?.experiment?.scenarioIntro || '从真实油气工程现场进入实验' }}</p>
        <h2>{{ detail?.experiment?.expName || '选择一个实验项目开始学习' }}</h2>
        <span>{{ detail?.experiment?.description || courseDetail?.course?.tagline || '围绕真实油气工程场景完成从预习到考核反馈的实验闭环。' }}</span>
      </div>
    </section>

    <section class="learning-layout">
      <aside class="catalog-panel">
        <div class="panel-title">
          <el-icon><Collection /></el-icon>
          <span>实验目录</span>
        </div>
        <button
          v-for="item in experiments"
          :key="item.id"
          :class="['catalog-item', { active: item.id === activeExperimentId }]"
          type="button"
          @click="selectExperiment(item.id)"
        >
          <strong>{{ item.expName }}</strong>
          <span>{{ item.expCode }} · {{ riskLabel(item.riskLevel) }} · {{ item.durationMinutes || 0 }} 分钟</span>
          <div class="catalog-progress">
            <el-progress :percentage="experimentProgress(item)" :stroke-width="6" />
            <em>{{ catalogState(item) }}</em>
          </div>
        </button>
        <el-empty v-if="!courseLoading && experiments.length === 0" description="暂无开放实验" :image-size="80" />
      </aside>

      <main class="study-panel" v-loading="detailLoading">
        <template v-if="detail">
          <div class="study-head">
            <div>
              <h2>{{ detail.experiment?.expName }}</h2>
              <span>{{ detail.experiment?.description || detail.courseName }}</span>
            </div>
            <el-tag :type="detail.reservationAllowed ? 'success' : 'warning'">
              {{ detail.reservationAllowed ? '可预约实验' : '需完成准入条件' }}
            </el-tag>
          </div>
          <el-alert
            v-if="admissionStatus && !admissionStatus.qualified"
            class="admission-alert"
            type="warning"
            :closable="false"
            :title="admissionStatus.reason || '暂未满足实验准入条件'"
          />

          <div class="visual-path">
            <div v-for="item in visualPathItems" :key="item.label" :class="{ active: item.active, done: item.done }">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
              <small>{{ item.status }}</small>
            </div>
          </div>

          <section class="next-action-band">
            <div>
              <p>当前建议</p>
              <strong>{{ closedLoopNextAction.title }}</strong>
              <span>{{ closedLoopNextAction.desc }}</span>
            </div>
            <el-button type="primary" :icon="closedLoopNextAction.icon" @click="closedLoopNextAction.action">
              {{ closedLoopNextAction.button }}
            </el-button>
          </section>

          <div class="progress-strip">
            <div>
              <strong>{{ Math.round(Number(detail.learningProgress || 0)) }}%</strong>
              <span>必学资源进度</span>
            </div>
            <div>
              <strong>{{ detail.examPassed ? '已通过' : '未通过' }}</strong>
              <span>安全准入考试</span>
            </div>
            <div>
              <strong>{{ reservationMeta.label }}</strong>
              <span>实验预约</span>
            </div>
            <div>
              <strong>{{ reportScoreText }}</strong>
              <span>成绩反馈</span>
            </div>
            <div>
              <strong>{{ reportMeta.label }}</strong>
              <span>实验报告</span>
            </div>
          </div>

          <section class="objective-grid">
            <div>
              <b>学习目标</b>
              <p>{{ detail.experiment?.objective || '按教师要求完成本实验的预习、操作准备和报告反馈。' }}</p>
            </div>
            <div>
              <b>实验原理</b>
              <p>{{ detail.experiment?.principle || '暂无实验原理说明。' }}</p>
            </div>
            <div>
              <b>仪器设备</b>
              <p>{{ detail.experiment?.equipment || '暂无仪器设备说明。' }}</p>
            </div>
            <div>
              <b>实验准备清单</b>
              <p>{{ detail.experiment?.materials || detail.experiment?.safetyRequirement || '请按实验规程完成物料、PPE 和记录表准备。' }}</p>
            </div>
          </section>

          <el-tabs v-model="activeTab">
            <el-tab-pane label="学习资源" name="resources">
              <el-empty v-if="(detail.resources || []).length === 0" description="暂无教学资源" />
              <div v-else class="resource-list">
                <article v-for="resource in detail.resources" :key="resource.id" class="resource-card">
                  <div>
                    <h3>{{ resource.title }}</h3>
                    <p>{{ resource.resourceType || 'RESOURCE' }} · {{ resource.requiredFlag ? '必学' : '选学' }} · {{ resource.completionRule || 'CONFIRM' }}</p>
                  </div>
                  <div class="resource-actions">
                    <el-button :icon="View" @click="previewResource(resource)">预览</el-button>
                    <el-button v-if="resource.url" :icon="LinkIcon" @click="openExternal(resource.url)">外部链接</el-button>
                    <el-button type="primary" :icon="Check" @click="completeResource(resource)">标记完成</el-button>
                  </div>
                </article>
              </div>
            </el-tab-pane>

            <el-tab-pane label="实验规程" name="steps">
              <el-empty v-if="(detail.steps || []).length === 0" description="暂无实验步骤" />
              <div v-else class="step-list">
                <article v-for="step in detail.steps" :key="step.id || step.stepNo" class="step-card">
                  <div class="step-no">{{ step.stepNo }}</div>
                  <div class="step-column">
                    <h3>{{ step.title }}</h3>
                    <p>{{ step.content }}</p>
                    <a v-if="step.mediaUrl" :href="step.mediaUrl" target="_blank" rel="noreferrer">查看图文/视频资料</a>
                  </div>
                  <div class="safety-column">
                    <h4>风险提示</h4>
                    <p>{{ step.safetyTip || '本步骤暂无额外风险提示，请遵守实验室通用安全规范。' }}</p>
                  </div>
                </article>
              </div>
            </el-tab-pane>

            <el-tab-pane label="安全与准入" name="safety">
              <div class="info-grid">
                <div><b>危险源</b><span>{{ detail.experiment?.hazardSources || '-' }}</span></div>
                <div><b>风险类型</b><span>{{ detail.experiment?.riskTypes || '-' }}</span></div>
                <div><b>PPE要求</b><span>{{ detail.experiment?.ppeRequirements || '-' }}</span></div>
                <div><b>前置知识</b><span>{{ detail.experiment?.prerequisiteKnowledge || '-' }}</span></div>
                <div><b>异常处理</b><span>{{ detail.experiment?.abnormalHandling || '-' }}</span></div>
                <div><b>应急处置</b><span>{{ detail.experiment?.emergencyProcedure || '-' }}</span></div>
              </div>
              <div v-if="admissionStatus?.missingTasks?.length" class="missing-box">
                <b>缺失条件</b>
                <span v-for="task in admissionStatus.missingTasks" :key="task.taskId">{{ task.taskName }}</span>
              </div>
              <div class="task-actions">
                <el-button type="primary" :icon="EditPen" @click="router.push('/student/exams')">参加安全考试</el-button>
                <el-button :disabled="!detail.reservationAllowed" :icon="Calendar" @click="router.push('/student/reserve')">预约实验</el-button>
              </div>
              <div class="ai-helper">
                <div>
                  <h3>AI 辅助解释</h3>
                  <p>围绕当前实验的风险、设备和安全规程提问，回答只作学习参考。</p>
                </div>
                <el-input v-model="aiQuestion" type="textarea" :rows="3" placeholder="例如：这个实验为什么要先检查压力表？" />
                <div class="task-actions">
                  <el-button type="primary" :icon="MagicStick" :loading="askingAi" @click="askExperimentAi">提问</el-button>
                </div>
                <p v-if="aiAnswer" class="ai-answer">{{ aiAnswer }}</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="报告提交" name="report">
              <div class="report-guide">
                <p><b>数据记录要求：</b>{{ detail.experiment?.dataRecordRequirement || '按教师要求记录实验数据。' }}</p>
                <p><b>评分标准：</b>{{ detail.experiment?.gradingCriteria || '暂无评分标准。' }}</p>
                <p><b>当前反馈：</b>{{ reportFeedbackText }}</p>
                <p v-if="detail.experiment?.reportTemplateUrl">
                  <b>报告模板：</b><a :href="detail.experiment.reportTemplateUrl" target="_blank" rel="noreferrer">下载/查看模板</a>
                </p>
              </div>
              <el-form :model="reportForm" label-width="88px">
                <el-form-item label="报告标题">
                  <el-input v-model="reportForm.title" placeholder="请输入报告标题" />
                </el-form-item>
                <el-form-item label="附件地址">
                  <el-input v-model="reportForm.fileUrl" placeholder="可选，填写报告文件URL" />
                </el-form-item>
                <el-form-item label="报告内容">
                  <el-input v-model="reportForm.content" type="textarea" :rows="10" placeholder="填写实验目的、步骤记录、数据、分析结论和安全反思" />
                </el-form-item>
              </el-form>
              <div class="task-actions">
                <el-button :loading="reportSaving" @click="saveReportDraft">保存草稿</el-button>
                <el-button type="primary" :loading="reportSubmitting" :disabled="!canSubmitReport" @click="submitCurrentReport">提交报告</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="讨论答疑" name="discussion">
              <div class="discussion-entry">
                <div>
                  <h3>实验问题答疑</h3>
                  <p>围绕当前油气实验的设备、风险、数据和报告发布问题，沉淀可复用的工程经验。</p>
                </div>
                <el-button type="primary" :icon="ChatLineRound" @click="openDiscussion">进入讨论</el-button>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
        <el-empty v-else-if="!detailLoading" description="请选择一个实验项目开始学习" />
      </main>

      <aside class="task-panel">
        <div class="panel-title">
          <el-icon><Tickets /></el-icon>
          <span>学习任务</span>
        </div>
        <div class="path-summary">
          <strong>{{ Math.round(Number(taskPath?.progress || 0)) }}%</strong>
          <span>路径完成度</span>
        </div>
        <el-empty v-if="!taskPath?.tasks?.length" description="暂无学习任务" :image-size="70" />
        <div v-for="item in taskPath?.tasks || []" :key="item.task.id" class="task-row">
          <div>
            <span>{{ item.task.taskName }}</span>
            <small>{{ taskTypeLabel(item.task.taskType) }} · {{ item.task.requiredFlag ? '必做' : '选学' }}</small>
            <small v-if="item.lockedReason">{{ item.lockedReason }}</small>
            <small v-else-if="item.task.deadline">{{ taskDeadlineText(item.task.deadline) }}</small>
          </div>
          <el-tag :type="stateMeta(item.state).type">{{ stateMeta(item.state).label }}</el-tag>
          <el-button
            v-if="canOperateTask(item) && !item.completed"
            text
            type="primary"
            @click="handleTaskAction(item)"
          >
            {{ item.task.taskType === 'CHECKLIST' ? '确认' : '进入' }}
          </el-button>
        </div>
        <div v-if="taskPath?.nextTask" class="notice-box">
          下一步：{{ taskPath.nextTask.task.taskName }}
        </div>
        <div class="task-nav">
          <el-button :disabled="!previousTask" @click="handleTaskAction(previousTask)">上一个任务</el-button>
          <el-button type="primary" :disabled="!nextTask" @click="handleTaskAction(nextTask)">下一个任务</el-button>
        </div>
        <div v-if="taskPath?.reminders?.length" class="notice-box warning">
          {{ taskPath.reminders.length }} 个任务临近截止或已逾期。
        </div>
      </aside>
    </section>
    <ResourceViewer v-model="viewerVisible" :resource-id="activeResourceId" @completed="refreshCurrentExperiment" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Back,
  Calendar,
  ChatLineRound,
  Check,
  Collection,
  EditPen,
  Link as LinkIcon,
  MagicStick,
  Operation,
  Tickets,
  View,
  VideoPlay,
} from '@element-plus/icons-vue'
import ResourceViewer from '@/components/learning/ResourceViewer.vue'
import { getCourseDetail } from '@/api/course'
import { getExperimentDetail } from '@/api/experiment'
import { updateLearningProgress } from '@/api/learningRecord'
import { confirmChecklistTask, getLearningPath } from '@/api/learningTask'
import { createReport, getMyReports, submitReport, updateReport } from '@/api/report'
import { getMyReservations } from '@/api/reservation'
import { askAi } from '@/api/ai'
import procedureSafety from '@/assets/amazing/procedure-safety.png'

const route = useRoute()
const router = useRouter()
const courseLoading = ref(false)
const detailLoading = ref(false)
const reportSaving = ref(false)
const reportSubmitting = ref(false)
const courseDetail = ref(null)
const detail = ref(null)
const taskPath = ref(null)
const reports = ref([])
const reservations = ref([])
const activeExperimentId = ref(null)
const activeTab = ref('resources')
const reportForm = reactive({ id: null, title: '', content: '', fileUrl: '', status: '' })
const viewerVisible = ref(false)
const activeResourceId = ref(null)
const askingAi = ref(false)
const aiQuestion = ref('')
const aiAnswer = ref('')

const experiments = computed(() => courseDetail.value?.experiments || [])
const courseTitle = computed(() => courseDetail.value?.course?.courseName || '课程学习')
const courseProgress = computed(() => Math.round(Number(courseDetail.value?.averageProgress || 0)))
const currentReport = computed(() => reports.value.find((item) => Number(item.experimentId) === Number(activeExperimentId.value)))
const currentReservation = computed(() => reservations.value.find((item) => Number(item.experimentId) === Number(activeExperimentId.value) && !['CANCELLED', 'REJECTED'].includes(item.status)))
const reportMeta = computed(() => reportStatusMeta(reportForm.status || currentReport.value?.status))
const reservationMeta = computed(() => reservationStatusMeta(currentReservation.value?.status))
const canSubmitReport = computed(() => ['DRAFT', 'RETURNED', ''].includes(reportForm.status || ''))
const nextTask = computed(() => taskPath.value?.nextTask || null)
const previousTask = computed(() => {
  const tasks = taskPath.value?.tasks || []
  const index = tasks.findIndex((item) => item.task?.id === nextTask.value?.task?.id)
  if (index <= 0) return null
  return tasks[index - 1]
})
const admissionStatus = computed(() => detail.value?.admissionStatus || null)
const experimentVisual = computed(() => detail.value?.experiment?.coverUrl || procedureSafety)
const visualPathItems = computed(() => [
  { label: '预习资源', status: `${Math.round(Number(detail.value?.learningProgress || 0))}%`, icon: Collection, active: activeTab.value === 'resources', done: Number(detail.value?.learningProgress || 0) >= 80 },
  { label: '风险认知', status: riskLabel(detail.value?.experiment?.riskLevel), icon: Operation, active: activeTab.value === 'safety', done: Boolean(detail.value?.experiment?.hazardSources || detail.value?.experiment?.riskTypes) },
  { label: '安全准入', status: detail.value?.examPassed ? '已通过' : '待通过', icon: EditPen, active: activeTab.value === 'safety', done: detail.value?.examPassed || detail.value?.reservationAllowed },
  { label: '实验预约', status: reservationMeta.value.label, icon: Calendar, active: false, done: ['待审核', '已通过'].includes(reservationMeta.value.label) || detail.value?.reservationAllowed },
  { label: '实验报告', status: reportMeta.value.label, icon: Tickets, active: activeTab.value === 'report', done: ['SUBMITTED', 'GRADED'].includes(reportForm.status || currentReport.value?.status) },
  { label: '成绩反馈', status: reportScoreText.value, icon: Check, active: activeTab.value === 'report', done: (reportForm.status || currentReport.value?.status) === 'GRADED' },
])
const reportScoreText = computed(() => currentReport.value?.latestScore != null ? `${currentReport.value.latestScore}分` : '待反馈')
const reportFeedbackText = computed(() => currentReport.value?.latestComment || (currentReport.value?.status === 'GRADED' ? '教师已完成批改，请查看成绩。' : '暂无教师反馈，提交报告后将在此显示。'))
const closedLoopNextAction = computed(() => {
  if (Number(detail.value?.learningProgress || 0) < 80) {
    return { title: '先完成预习资源', desc: '观看视频、阅读图文或确认必学资料，让工程场景先变清楚。', button: '去预习', icon: VideoPlay, action: () => { activeTab.value = 'resources' } }
  }
  if (!detail.value?.examPassed && detail.value?.experiment?.examRequired !== 0) {
    return { title: '通过安全准入考试', desc: '完成风险识别和准入考核后，才能进入预约与线下实验。', button: '去考试', icon: EditPen, action: () => router.push('/student/exams') }
  }
  if (!currentReservation.value && detail.value?.reservationAllowed) {
    return { title: '预约实验时段', desc: '选择合适的实验室和时段，提交本次油气实验预约。', button: '去预约', icon: Calendar, action: () => router.push('/student/reserve') }
  }
  if (!['SUBMITTED', 'GRADED'].includes(reportForm.status || currentReport.value?.status)) {
    return { title: '完成实验报告', desc: '记录数据、分析误差和安全反思，形成可评价的实验成果。', button: '写报告', icon: Tickets, action: () => { activeTab.value = 'report' } }
  }
  return { title: '查看成绩反馈', desc: '对照教师评语和评分标准复盘实验操作与数据分析。', button: '看反馈', icon: Check, action: () => { activeTab.value = 'report' } }
})

watch(() => route.query.experimentId, (value) => {
  if (value) selectExperiment(Number(value))
})

onMounted(async () => {
  await loadCourse()
  const firstId = Number(route.query.experimentId || experiments.value[0]?.id || 0)
  if (firstId) await selectExperiment(firstId)
})

async function loadCourse() {
  courseLoading.value = true
  try {
    courseDetail.value = await getCourseDetail(route.params.courseId)
  } finally {
    courseLoading.value = false
  }
}

async function selectExperiment(id) {
  activeExperimentId.value = id
  detailLoading.value = true
  aiAnswer.value = ''
  try {
    const [experimentDetail, reportResult, reservationResult] = await Promise.all([
      getExperimentDetail(id),
      getMyReports({ pageNum: 1, pageSize: 100 }),
      getMyReservations({ pageNum: 1, pageSize: 100 }, { silent: true }).catch(() => ({ records: [] })),
    ])
    detail.value = experimentDetail
    reports.value = reportResult?.records || []
    reservations.value = reservationResult?.records || []
    taskPath.value = await getLearningPath(id)
    loadReportForm()
  } finally {
    detailLoading.value = false
  }
}

async function askExperimentAi() {
  if (!aiQuestion.value.trim()) {
    ElMessage.warning('请输入要解释的问题')
    return
  }
  askingAi.value = true
  try {
    const result = await askAi({
      scene: 'SAFETY_QA',
      experimentId: activeExperimentId.value,
      question: aiQuestion.value.trim(),
    })
    aiAnswer.value = result?.answer || result?.content || String(result || '')
  } finally {
    askingAi.value = false
  }
}

async function completeResource(resource) {
  await updateLearningProgress({
    resourceId: resource.id,
    progress: 100,
    durationSeconds: 60,
    finishFlag: 1,
  })
  ElMessage.success('学习进度已更新')
  await selectExperiment(activeExperimentId.value)
}

function previewResource(resource) {
  activeResourceId.value = resource.id
  viewerVisible.value = true
}

async function refreshCurrentExperiment() {
  if (activeExperimentId.value) {
    await selectExperiment(activeExperimentId.value)
  }
}

function canOperateTask(item) {
  return item.opened && !item.locked && ['READ_RESOURCE', 'WATCH_VIDEO', 'SAFETY_KNOWLEDGE', 'PRACTICE', 'EXAM', 'CHECKLIST'].includes(item.task.taskType)
}

async function handleTaskAction(item) {
  if (!item || !canOperateTask(item)) return
  if (item.task.taskType === 'CHECKLIST') {
    await confirmChecklistTask(item.task.id)
    ElMessage.success('准备清单已确认')
    taskPath.value = await getLearningPath(activeExperimentId.value)
    return
  }
  if (['PRACTICE', 'EXAM'].includes(item.task.taskType)) {
    router.push('/student/exams')
    return
  }
  activeTab.value = item.task.taskType === 'SAFETY_KNOWLEDGE' ? 'safety' : 'resources'
}

function loadReportForm() {
  const report = currentReport.value
  Object.assign(reportForm, {
    id: report?.id || null,
    title: report?.title || `${detail.value?.experiment?.expName || '实验'}报告`,
    content: report?.content || '',
    fileUrl: report?.fileUrl || '',
    status: report?.status || '',
  })
}

async function saveReportDraft() {
  if (!validateReport()) return null
  reportSaving.value = true
  try {
    const payload = reportPayload()
    let id = reportForm.id
    if (id) {
      await updateReport(id, payload)
    } else {
      const result = await createReport(payload)
      id = result?.id
      reportForm.id = id
    }
    ElMessage.success('报告草稿已保存')
    await refreshReports()
    return id
  } finally {
    reportSaving.value = false
  }
}

async function submitCurrentReport() {
  if (!validateReportSubmit()) return
  reportSubmitting.value = true
  try {
    const id = await saveReportDraft()
    if (!id) return
    await submitReport(id)
    ElMessage.success('报告已提交')
    await refreshReports()
  } finally {
    reportSubmitting.value = false
  }
}

async function refreshReports() {
  const result = await getMyReports({ pageNum: 1, pageSize: 100 })
  reports.value = result?.records || []
  loadReportForm()
}

function validateReport() {
  if (!reportForm.title.trim()) {
    ElMessage.warning('请填写报告标题')
    return false
  }
  if (!['', 'DRAFT', 'RETURNED'].includes(reportForm.status)) {
    ElMessage.warning('当前报告状态不允许修改或重复提交')
    return false
  }
  return true
}

function validateReportSubmit() {
  if (!validateReport()) return false
  if (!reportForm.content.trim()) {
    ElMessage.warning('请填写报告内容后再提交')
    return false
  }
  return true
}

function reportPayload() {
  return {
    experimentId: activeExperimentId.value,
    title: reportForm.title.trim(),
    content: reportForm.content.trim(),
    fileUrl: reportForm.fileUrl || undefined,
  }
}

function goContinue() {
  handleTaskAction(nextTask.value)
}

function experimentProgress(item) {
  return Math.round(Number(item.learningProgress || 0))
}

function catalogState(item) {
  const progress = experimentProgress(item)
  if (progress >= 100) return '预习完成'
  if (progress > 0) return '学习中'
  return item.status === 1 ? '待开始' : '未开放'
}

function taskDeadlineText(deadline) {
  const date = new Date(deadline)
  if (Number.isNaN(date.getTime())) return '截止时间待确认'
  const diff = date.getTime() - Date.now()
  if (diff < 0) return `已逾期 ${timeLeft(Math.abs(diff))}`
  return `截止 ${formatDate(date)}，剩余 ${timeLeft(diff)}`
}

function formatDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function timeLeft(ms) {
  const hours = Math.floor(ms / 3_600_000)
  if (hours >= 24) return `${Math.floor(hours / 24)} 天`
  if (hours > 0) return `${hours} 小时`
  return `${Math.max(1, Math.floor(ms / 60_000))} 分钟`
}

function openExternal(url) {
  window.open(url, '_blank', 'noopener,noreferrer')
}

function openDiscussion() {
  router.push({
    path: '/discussions',
    query: {
      courseId: route.params.courseId,
      experimentId: activeExperimentId.value,
    },
  })
}

function riskLabel(risk) {
  return { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }[risk] || risk || '未分级'
}

function taskTypeLabel(type) {
  return {
    READ_RESOURCE: '阅读资源',
    WATCH_VIDEO: '观看视频',
    SAFETY_KNOWLEDGE: '安全知识',
    PRACTICE: '完成练习',
    EXAM: '参加考试',
    CHECKLIST: '准备清单',
  }[type] || type || '任务'
}

function stateMeta(state) {
  return {
    COMPLETED: { label: '完成', type: 'success' },
    NOT_OPEN: { label: '未开放', type: 'info' },
    LOCKED: { label: '锁定', type: 'info' },
    DUE_SOON: { label: '临期', type: 'warning' },
    OVERDUE: { label: '逾期', type: 'danger' },
    IN_PROGRESS: { label: '进行中', type: 'primary' },
    NOT_STARTED: { label: '未开始', type: 'info' },
  }[state] || { label: '待完成', type: 'info' }
}

function reportStatusMeta(status) {
  return {
    DRAFT: { label: '草稿', type: 'info' },
    RETURNED: { label: '退回修改', type: 'danger' },
    SUBMITTED: { label: '已提交', type: 'warning' },
    GRADED: { label: '已批改', type: 'success' },
  }[status] || { label: '未创建', type: 'info' }
}

function reservationStatusMeta(status) {
  return {
    PENDING: { label: '待审核', type: 'warning' },
    APPROVED: { label: '已通过', type: 'success' },
    REJECTED: { label: '已拒绝', type: 'danger' },
    CANCELLED: { label: '已取消', type: 'info' },
  }[status] || { label: '未预约', type: 'info' }
}
</script>

<style scoped>
.learning-page { max-width: 1280px; margin: 0 auto; }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 18px; }
.experiment-hero { min-height: 210px; border-radius: 8px; overflow: hidden; background-size: cover; background-position: center; display: flex; align-items: center; margin-bottom: 16px; }
.experiment-hero > div { max-width: 660px; padding: 26px; background: linear-gradient(90deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.76), rgba(255, 255, 255, 0)); }
.experiment-hero p { color: #177e89; font-weight: 800; margin-bottom: 8px; }
.experiment-hero h2 { color: #13233a; font-size: 28px; line-height: 1.2; margin-bottom: 8px; }
.experiment-hero span { color: #344054; line-height: 1.7; }
.head-actions { display: flex; align-items: center; gap: 10px; }
.course-progress { min-width: 116px; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 10px 12px; }
.course-progress strong { display: block; color: #13233a; font-size: 20px; line-height: 1; }
.course-progress span { color: #667085; font-size: 12px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.learning-layout { display: grid; grid-template-columns: 260px minmax(0, 1fr) 260px; gap: 16px; align-items: start; }
.catalog-panel, .study-panel, .task-panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.panel-title { display: flex; align-items: center; gap: 8px; color: #13233a; font-weight: 700; margin-bottom: 12px; }
.catalog-item { width: 100%; text-align: left; border: 1px solid #edf1f5; background: #f8fafc; border-radius: 8px; padding: 12px; margin-bottom: 10px; cursor: pointer; }
.catalog-item.active { border-color: #409eff; background: #eef6ff; }
.catalog-item strong { display: block; color: #13233a; line-height: 1.4; margin-bottom: 6px; }
.catalog-item span { color: #667085; font-size: 12px; }
.catalog-progress { display: grid; gap: 4px; margin-top: 8px; }
.catalog-progress em { color: #667085; font-size: 12px; font-style: normal; }
.study-head { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.admission-alert { margin-bottom: 12px; }
.visual-path { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 8px; margin-bottom: 14px; }
.visual-path div { min-height: 72px; display: grid; align-content: center; justify-items: center; gap: 6px; color: #536579; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; }
.visual-path div.active { color: #177e89; border-color: #99d2d7; background: #eefafa; }
.visual-path div.done { color: #2d6a4f; border-color: #b7dfc5; background: #f0fbf4; }
.visual-path span { font-size: 12px; }
.visual-path small { color: inherit; font-size: 11px; opacity: 0.78; }
.next-action-band { display: flex; align-items: center; justify-content: space-between; gap: 16px; background: #f0fbf4; border: 1px solid #b7dfc5; border-radius: 8px; padding: 14px; margin-bottom: 14px; }
.next-action-band p { color: #2d6a4f; font-size: 12px; font-weight: 800; margin-bottom: 4px; }
.next-action-band strong { display: block; color: #13233a; font-size: 17px; margin-bottom: 4px; }
.next-action-band span { color: #536579; line-height: 1.6; }
.study-head h2 { color: #13233a; font-size: 22px; margin-bottom: 6px; }
.study-head span { color: #667085; line-height: 1.5; }
.progress-strip { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 14px; }
.progress-strip div { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.progress-strip strong { display: block; color: #13233a; font-size: 20px; }
.progress-strip span { color: #7b8794; font-size: 12px; }
.objective-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.objective-grid div { background: #fff; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.objective-grid b { display: block; color: #13233a; margin-bottom: 6px; }
.objective-grid p { color: #667085; line-height: 1.6; margin: 0; white-space: pre-wrap; }
.resource-list, .step-list { display: grid; gap: 12px; }
.resource-card { display: flex; justify-content: space-between; gap: 12px; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.resource-card h3, .step-card h3 { color: #13233a; font-size: 16px; margin-bottom: 6px; }
.resource-card p, .step-card p, .report-guide p { color: #667085; line-height: 1.65; margin: 0; }
.resource-actions, .task-actions { display: flex; gap: 8px; align-items: center; justify-content: flex-end; margin-top: 12px; }
.step-card { display: grid; grid-template-columns: 44px minmax(0, 1fr) minmax(220px, 0.8fr); gap: 12px; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.step-no { width: 34px; height: 34px; border-radius: 50%; background: #eef6ff; color: #1f6feb; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.safety-column { background: #fff7e6; border: 1px solid #ffe3a3; border-radius: 8px; padding: 10px; }
.safety-column h4 { color: #7a4b00; margin-bottom: 6px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.info-grid div { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.info-grid b { display: block; color: #13233a; margin-bottom: 6px; }
.info-grid span { color: #667085; line-height: 1.6; white-space: pre-wrap; }
.missing-box { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; background: #fff7e6; border: 1px solid #ffe3a3; border-radius: 8px; padding: 12px; margin-top: 12px; }
.missing-box b { color: #7a4b00; margin-right: 4px; }
.missing-box span { color: #7a4b00; background: #fff; border: 1px solid #ffe3a3; border-radius: 999px; padding: 4px 9px; font-size: 12px; }
.ai-helper { display: grid; gap: 10px; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 14px; margin-top: 14px; }
.ai-helper h3 { color: #13233a; font-size: 16px; margin-bottom: 4px; }
.ai-helper p { color: #667085; line-height: 1.6; margin: 0; }
.ai-answer { white-space: pre-wrap; background: #fff; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.report-guide { display: grid; gap: 8px; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; margin-bottom: 14px; }
.discussion-entry { display: flex; justify-content: space-between; align-items: center; gap: 16px; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 16px; }
.discussion-entry h3 { color: #13233a; font-size: 16px; margin-bottom: 6px; }
.discussion-entry p { color: #667085; line-height: 1.6; margin: 0; }
.task-row { display: flex; justify-content: space-between; align-items: center; gap: 8px; padding: 10px 0; border-bottom: 1px solid #f0f2f5; color: #344054; }
.task-row div { display: grid; gap: 2px; min-width: 0; }
.task-row small { color: #7b8794; font-size: 12px; }
.task-nav { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 12px; }
.path-summary { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.path-summary strong { display: block; color: #13233a; font-size: 22px; }
.path-summary span { color: #7b8794; font-size: 12px; }
.notice-box { color: #7a4b00; background: #fff7e6; border: 1px solid #ffe3a3; border-radius: 8px; padding: 12px; line-height: 1.6; margin-top: 12px; }
.notice-box.warning { color: #8a2f0f; background: #fff1f0; border-color: #ffd6d1; }
@media (max-width: 1100px) { .learning-layout { grid-template-columns: 1fr; } }
@media (max-width: 760px) {
  .page-head, .head-actions, .resource-card, .study-head { align-items: stretch; flex-direction: column; }
  .next-action-band { align-items: stretch; flex-direction: column; }
  .progress-strip, .info-grid, .objective-grid, .step-card, .visual-path { grid-template-columns: 1fr; }
  .experiment-hero > div { padding: 22px; background: rgba(255, 255, 255, 0.88); }
}
</style>
