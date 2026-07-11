<template>
  <div class="chapter-study-page">
    <header class="study-topbar">
      <el-button text :icon="Back" @click="router.push(`/classrooms/${courseId}/learn?module=chapters`)">返回课程</el-button>
      <strong>章节详情</strong>
      <span>{{ courseTitle }}</span>
    </header>

    <main class="study-shell" v-loading="loading">
      <section ref="contentPane" class="content-pane" @scroll="handleContentScroll">
        <section class="steps-section">
          <el-empty v-if="steps.length === 0" description="当前实验暂无步骤内容" />
          <article
            v-else-if="selectedStep"
            :id="stepTarget(selectedStep)"
            :key="selectedStep.id || selectedStep.stepNo"
            class="step-block"
          >
            <div class="step-title">
              <span>{{ selectedStep.stepNo || '-' }}</span>
              <div>
                <p>{{ experiment?.expName || '实验' }}</p>
                <h1>{{ selectedStep.title || '实验步骤' }}</h1>
              </div>
            </div>

            <div class="step-body">
              <p>{{ selectedStep.content || '教师暂未填写本步骤的文字说明。' }}</p>
              <div v-if="selectedStep.mediaUrl" class="step-media-preview">
                <video
                  v-if="isVideoStep(selectedStep)"
                  controls
                  preload="metadata"
                  :src="selectedStep.mediaUrl"
                  @ended="completeSelectedStep"
                />
                <iframe
                  v-else-if="isEmbeddableMedia(selectedStep)"
                  :src="selectedStep.mediaUrl"
                  title="步骤视频或资料"
                />
                <a v-else :href="selectedStep.mediaUrl" target="_blank" rel="noreferrer" @click="completeSelectedStep">打开步骤视频或资料链接</a>
              </div>
              <aside>
                <b>风险提示</b>
                <span>{{ selectedStep.safetyTip || '本步骤暂无额外风险提示。' }}</span>
              </aside>
            </div>
          </article>
        </section>
      </section>

      <aside class="study-sidebar">
        <div class="catalog-head">
          <h2>课程目录</h2>
          <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="搜索实验步骤" />
        </div>
        <nav class="course-catalog">
          <section v-for="group in filteredCourseCatalog" :key="group.experimentId" class="catalog-group">
            <div class="catalog-experiment" :class="{ active: group.experimentId === experimentId }">
              <span>{{ group.index }}</span>
              <strong>{{ group.title }}</strong>
            </div>
            <button
              v-for="node in group.steps"
              :key="node.key"
              type="button"
              class="catalog-step"
              :class="{ active: node.experimentId === experimentId && node.target === activeTarget, done: isStepDone(node.stepId) }"
              @click="openStep(node)"
            >
              <span><el-icon><Check /></el-icon></span>
              <strong>{{ node.no }} {{ node.title }}</strong>
            </button>
          </section>
        </nav>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, Check, Search } from '@element-plus/icons-vue'
import { getCourseDetail } from '@/api/course'
import { getExperimentDetail } from '@/api/experiment'
import { completeStepLearning, getMyStepLearningRecords } from '@/api/learningRecord'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const course = ref(null)
const detail = ref(null)
const keyword = ref('')
const activeTarget = ref('')
const contentPane = ref(null)
const completedStepIds = ref(new Set())
const completingStepIds = new Set()
const experimentDetails = reactive({})

const courseId = computed(() => Number(route.params.courseId))
const experimentId = computed(() => Number(route.params.experimentId))
const experiment = computed(() => detail.value?.experiment || null)
const steps = computed(() => sortedSteps(detail.value?.steps || []))
const selectedStep = computed(() => steps.value.find((step) => stepTarget(step) === activeTarget.value) || steps.value[0] || null)
const courseTitle = computed(() => course.value?.course?.courseName || detail.value?.courseName || '课堂学习')
const courseExperiments = computed(() => course.value?.experiments || [])
const courseCatalog = computed(() => courseExperiments.value.map((experimentItem, index) => {
  const experimentDetail = experimentDetails[experimentItem.id]
  return {
    experimentId: Number(experimentItem.id),
    index: index + 1,
    title: experimentItem.expName || `实验 ${index + 1}`,
    steps: sortedSteps(experimentDetail?.steps || []).map((step, stepIndex) => ({
      key: `${experimentItem.id}-${step.id || step.stepNo || stepIndex}`,
      experimentId: Number(experimentItem.id),
      no: `${index + 1}.${step.stepNo || stepIndex + 1}`,
      title: step.title || `实验步骤 ${stepIndex + 1}`,
      target: stepTarget(step),
      stepId: Number(step.id),
    })),
  }
}))
const filteredCourseCatalog = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return courseCatalog.value
  return courseCatalog.value
    .map((group) => ({
      ...group,
      steps: group.steps.filter((step) => `${group.title} ${step.title}`.toLowerCase().includes(kw)),
    }))
    .filter((group) => group.steps.length > 0 || group.title.toLowerCase().includes(kw))
})

onMounted(loadPage)
watch(() => route.params.experimentId, loadCurrentExperiment)
watch(selectedStep, scheduleReadCompletion)
watch(() => route.query.step, () => {
  nextTick(() => scrollTo(String(route.query.step || '')))
})

async function loadPage() {
  loading.value = true
  try {
    course.value = await getCourseDetail(courseId.value)
    await Promise.all([loadAllExperimentDetails(), loadStepRecords()])
    await loadCurrentExperiment()
  } finally {
    loading.value = false
  }
}

async function loadAllExperimentDetails() {
  await Promise.all(courseExperiments.value.map(async (item) => {
    if (!experimentDetails[item.id]) {
      experimentDetails[item.id] = await getExperimentDetail(item.id).catch(() => ({ steps: [] }))
    }
  }))
}

async function loadCurrentExperiment() {
  if (!experimentDetails[experimentId.value]) {
    experimentDetails[experimentId.value] = await getExperimentDetail(experimentId.value)
  }
  detail.value = experimentDetails[experimentId.value]
  const target = String(route.query.step || firstStepTarget() || '')
  await nextTick()
  scrollTo(target)
}

function sortedSteps(list) {
  return [...list].sort((a, b) => Number(a.stepNo || 0) - Number(b.stepNo || 0))
}

function stepTarget(step) {
  return `step-${step.stepNo || step.id}`
}

function firstStepTarget() {
  const first = steps.value[0]
  return first ? stepTarget(first) : ''
}

async function openStep(node) {
  if (node.experimentId !== experimentId.value) {
    await router.push({
      path: `/classrooms/${courseId.value}/chapters/${node.experimentId}`,
      query: { step: node.target },
    })
    return
  }
  activeTarget.value = node.target
  await router.replace({
    path: route.path,
    query: { ...route.query, step: node.target },
  })
}

function scrollTo(id) {
  if (!id) return
  activeTarget.value = id
}

async function loadStepRecords() {
  const result = await getMyStepLearningRecords({ silent: true }).catch(() => [])
  completedStepIds.value = new Set((result || []).map((record) => Number(record.stepId)))
}

function isStepDone(stepId) {
  return completedStepIds.value.has(Number(stepId))
}

function scheduleReadCompletion() {
  nextTick(() => {
    if (contentPane.value) contentPane.value.scrollTop = 0
    window.setTimeout(checkReadCompletion, 800)
  })
}

function handleContentScroll() {
  checkReadCompletion()
}

function checkReadCompletion() {
  const pane = contentPane.value
  if (!pane || !selectedStep.value || isVideoStep(selectedStep.value)) return
  if (pane.scrollTop + pane.clientHeight >= pane.scrollHeight - 24) completeSelectedStep()
}

async function completeSelectedStep() {
  const stepId = Number(selectedStep.value?.id)
  if (!stepId || isStepDone(stepId) || completingStepIds.has(stepId)) return
  completingStepIds.add(stepId)
  try {
    await completeStepLearning(stepId)
    completedStepIds.value = new Set([...completedStepIds.value, stepId])
  } finally {
    completingStepIds.delete(stepId)
  }
}

function isVideoStep(step) {
  const url = String(step?.mediaUrl || '').toLowerCase()
  return String(step?.mediaType || '').toUpperCase() === 'VIDEO' || /\.(mp4|webm|ogg)(\?|#|$)/.test(url)
}

function isEmbeddableMedia(step) {
  return /^https?:\/\//.test(String(step?.mediaUrl || ''))
}
</script>

<style scoped>
.chapter-study-page { height: calc(100vh - 82px); background: #f5f7fb; overflow: hidden; display: grid; grid-template-rows: 62px minmax(0, 1fr); }
.study-topbar { position: sticky; top: 0; z-index: 10; height: 62px; display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 12px; background: #344b68; color: #d7dee9; padding: 0 18px; }
.study-topbar strong { justify-self: center; font-size: 20px; }
.study-topbar :deep(.el-button.is-text) { color: #d7dee9; }
.study-shell { display: grid; grid-template-columns: minmax(0, 1fr) 430px; min-height: 0; overflow: hidden; }
.content-pane { min-width: 0; height: 100%; background: #fff; overflow-y: auto; }
.steps-section { display: grid; gap: 24px; padding: 44px min(8vw, 110px); }
.step-block { scroll-margin-top: 24px; border: 1px solid #e6ebf2; border-radius: 8px; background: #fff; padding: 22px; }
.step-title { display: grid; grid-template-columns: 44px minmax(0, 1fr); gap: 14px; align-items: start; padding-bottom: 16px; border-bottom: 1px solid #edf1f5; }
.step-title > span { width: 36px; height: 36px; border-radius: 50%; background: #eef6ff; color: #1f6feb; display: inline-flex; align-items: center; justify-content: center; font-weight: 800; }
.step-title p { color: #667085; font-size: 13px; margin-bottom: 6px; }
.step-title h1 { color: #13233a; font-size: 28px; line-height: 1.25; }
.step-body { display: grid; gap: 16px; padding-top: 18px; }
.step-body p, .step-body aside span { color: #344054; line-height: 1.9; white-space: pre-wrap; }
.step-media-preview { border: 1px solid #e7ebf0; border-radius: 8px; overflow: hidden; background: #f7f9fc; }
.step-media-preview video,
.step-media-preview iframe { display: block; width: 100%; aspect-ratio: 16 / 9; border: 0; background: #dfe7ef; }
.step-media-preview a { display: block; color: #1f6feb; padding: 14px; }
.step-body aside { background: #fff7e6; border: 1px solid #ffe3a3; border-radius: 8px; padding: 12px; }
.step-body aside b { display: block; color: #7a4b00; margin-bottom: 6px; }
.study-sidebar { height: 100%; min-width: 0; overflow-y: auto; background: #f8fafc; border-left: 8px solid #d4dbe3; padding: 22px 18px; }
.catalog-head { display: grid; gap: 12px; margin-bottom: 14px; }
.catalog-head h2 { color: #13233a; font-size: 22px; }
.course-catalog { display: grid; gap: 12px; }
.catalog-group { display: grid; gap: 4px; }
.catalog-experiment { display: grid; grid-template-columns: 34px minmax(0, 1fr); align-items: center; gap: 10px; border-radius: 6px; background: #eef2f7; padding: 12px; }
.catalog-experiment.active { background: #eaf2ff; color: #1f6feb; }
.catalog-experiment span { width: 24px; height: 24px; border-radius: 50%; background: #cfd8e3; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-weight: 800; }
.catalog-experiment strong, .catalog-step strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.catalog-step { display: grid; grid-template-columns: 28px minmax(0, 1fr); align-items: center; gap: 10px; border: 0; border-radius: 6px; background: transparent; color: #13233a; padding: 11px 12px 11px 32px; text-align: left; cursor: pointer; }
.catalog-step:hover, .catalog-step.active { background: #eaf2ff; color: #1f6feb; }
.catalog-step span { width: 22px; height: 22px; border-radius: 50%; background: #cfd8e3; color: #fff; display: inline-flex; align-items: center; justify-content: center; }
.catalog-step.done span { background: #36b867; }
.catalog-step.done strong { color: #23864a; }
@media (max-width: 1180px) {
  .chapter-study-page { height: auto; overflow: visible; }
  .study-shell { grid-template-columns: 1fr; overflow: visible; }
  .content-pane, .study-sidebar { height: auto; overflow: visible; }
  .study-sidebar { border-left: 0; border-top: 8px solid #d4dbe3; }
}
@media (max-width: 760px) {
  .study-topbar, .step-title { grid-template-columns: 1fr; }
  .steps-section { padding: 24px 16px; }
}
</style>
