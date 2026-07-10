<template>
  <div class="exam-taking-page">
    <header class="exam-header">
      <div>
        <el-button text :icon="Back" @click="returnToExamList">返回考试列表</el-button>
        <h1>{{ session.paperTitle || '安全知识考核' }}</h1>
        <p>请在规定时间内完成作答。提交后系统自动评分，并记录成绩和错题。</p>
      </div>
      <div class="timer-box" :class="{ urgent: remainingSeconds <= 300 }">
        <span>剩余时间</span>
        <strong>{{ countdownText }}</strong>
      </div>
    </header>

    <section v-loading="loading" class="exam-layout">
      <main class="question-pane">
        <el-empty v-if="!loading && session.questions.length === 0" description="试卷暂无题目" />
        <article
          v-for="(question, index) in session.questions"
          :id="`question-${question.id}`"
          :key="question.id"
          class="question-card"
        >
          <div class="question-title">
            <strong>{{ index + 1 }}. {{ question.content }}</strong>
            <el-tag>{{ questionTypeLabel(question.type) }} / {{ question.score || 0 }} 分</el-tag>
          </div>
          <el-radio-group v-if="['SINGLE', 'JUDGE'].includes(question.type)" v-model="answers[question.id]">
            <el-radio v-for="option in normalizedOptions(question)" :key="option.value" :label="option.value">{{ option.label }}</el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="question.type === 'MULTIPLE'" v-model="answers[question.id]">
            <el-checkbox v-for="option in normalizedOptions(question)" :key="option.value" :label="option.value">{{ option.label }}</el-checkbox>
          </el-checkbox-group>
          <el-input v-else v-model="answers[question.id]" type="textarea" :rows="4" placeholder="请输入答案" />
        </article>
      </main>

      <aside class="answer-card">
        <h2>答题卡</h2>
        <div class="answer-grid">
          <button
            v-for="(question, index) in session.questions"
            :key="question.id"
            :class="{ answered: isAnswered(question) }"
            type="button"
            @click="scrollToQuestion(question.id)"
          >
            {{ index + 1 }}
          </button>
        </div>
        <div class="answer-summary">
          <span>已答 {{ answeredCount }}/{{ session.questions.length }}</span>
          <span v-if="lastSaveTime">最近保存 {{ lastSaveTime }}</span>
        </div>
        <div class="answer-actions">
          <el-button :loading="saving" @click="saveCurrentAnswers">保存</el-button>
          <el-button type="primary" :loading="submitting" @click="submitCurrentExam(false)">提交试卷</el-button>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import { saveExamAnswers, startExam, submitExam } from '@/api/exam'

const route = useRoute()
const router = useRouter()
const courseId = computed(() => Number(route.params.courseId))
const paperId = computed(() => Number(route.params.paperId))

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const submitted = ref(false)
const remainingSeconds = ref(0)
const lastSaveTime = ref('')
const session = reactive({ recordId: null, paperId: null, paperTitle: '', questions: [], endTime: null })
const answers = reactive({})
let timer = null

const countdownText = computed(() => {
  const seconds = Math.max(0, remainingSeconds.value)
  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0')
  const sec = String(seconds % 60).padStart(2, '0')
  return `${minutes}:${sec}`
})

const answeredCount = computed(() => session.questions.filter((question) => isAnswered(question)).length)

onMounted(loadExam)
onUnmounted(stopTimer)

async function loadExam() {
  loading.value = true
  try {
    const result = await startExam(paperId.value)
    if (!result) {
      ElMessage.warning('考试暂时无法进入，请返回考试列表后重试')
      return
    }
    Object.assign(session, {
      recordId: result.recordId,
      paperId: result.paperId || paperId.value,
      paperTitle: result.paperTitle || '安全知识考核',
      questions: result.questions || [],
      endTime: result.endTime,
    })
    Object.keys(answers).forEach((key) => delete answers[key])
    session.questions.forEach((question) => {
      const saved = result?.answers?.[question.id] ?? result?.answers?.[String(question.id)]
      answers[question.id] = question.type === 'MULTIPLE' ? String(saved || '').split(',').filter(Boolean) : (saved || '')
    })
    startTimer(result.endTime)
  } finally {
    loading.value = false
  }
}

async function saveCurrentAnswers() {
  if (!session.recordId || submitted.value) return
  saving.value = true
  try {
    await saveExamAnswers(session.recordId, { answers: answerPayload() })
    lastSaveTime.value = new Date().toLocaleTimeString()
    ElMessage.success('答案已保存')
  } finally {
    saving.value = false
  }
}

async function submitCurrentExam(autoSubmit) {
  if (!session.recordId || submitted.value) return
  if (!autoSubmit) {
    await ElMessageBox.confirm('提交后将自动评分，确认提交吗？', '提交试卷', { type: 'warning' })
  }
  submitted.value = true
  submitting.value = true
  try {
    const result = await submitExam(session.recordId, { answers: answerPayload(), autoSubmit })
    stopTimer()
    if (!autoSubmit) {
      await ElMessageBox.alert(`得分：${result?.totalScore || 0}，结果：${result?.passed ? '通过' : '未通过'}`, '交卷完成', {
        type: result?.passed ? 'success' : 'warning',
      })
    } else {
      ElMessage.warning('考试时间已到，系统已自动交卷')
    }
    router.push(`/classrooms/${courseId.value}/learn?module=exam&examTab=records`)
  } finally {
    submitting.value = false
  }
}

async function returnToExamList() {
  await ElMessageBox.confirm('返回前会自动保存当前答案，确认返回考试列表吗？', '返回考试列表', { type: 'warning' })
  await saveCurrentAnswers()
  router.push(`/classrooms/${courseId.value}/learn?module=exam`)
}

function answerPayload() {
  return session.questions.map((question) => ({
    questionId: question.id,
    answer: Array.isArray(answers[question.id]) ? answers[question.id].join(',') : answers[question.id],
  }))
}

function startTimer(endTime) {
  stopTimer()
  const end = new Date(endTime).getTime()
  const tick = () => {
    remainingSeconds.value = Math.max(0, Math.floor((end - Date.now()) / 1000))
    if (remainingSeconds.value <= 0) {
      stopTimer()
      submitCurrentExam(true)
    }
  }
  tick()
  timer = window.setInterval(tick, 1000)
}

function stopTimer() {
  if (timer) window.clearInterval(timer)
  timer = null
}

function scrollToQuestion(id) {
  document.getElementById(`question-${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function isAnswered(question) {
  const answer = answers[question.id]
  return Array.isArray(answer) ? answer.length > 0 : !!answer
}

function normalizedOptions(question) {
  if (question.type === 'JUDGE') return [{ value: 'TRUE', label: '正确' }, { value: 'FALSE', label: '错误' }]
  try {
    const parsed = typeof question.options === 'string' ? JSON.parse(question.options || '[]') : question.options
    if (Array.isArray(parsed)) return parsed.map((item, index) => optionFromValue(item, index))
    return Object.entries(parsed || {}).map(([value, label]) => ({ value, label: `${value}. ${label}` }))
  } catch {
    return String(question.options || '').split(/\n|;/).filter(Boolean).map((item, index) => optionFromValue(item, index))
  }
}

function optionFromValue(item, index) {
  if (typeof item === 'object') return { value: item.value || item.key || String.fromCharCode(65 + index), label: item.label || item.text || item.content }
  const value = String(item).match(/^([A-Z])[\.\、:：]/)?.[1] || String.fromCharCode(65 + index)
  return { value, label: String(item) }
}

function questionTypeLabel(type) {
  return { SINGLE: '单选题', MULTIPLE: '多选题', JUDGE: '判断题', SHORT_ANSWER: '简答题' }[type] || type || '题目'
}
</script>

<style scoped>
.exam-taking-page {
  display: grid;
  gap: 16px;
  max-width: 1480px;
  height: calc(100vh - 96px);
  min-height: 680px;
  margin: 0 auto;
  overflow: hidden;
}

.exam-header,
.question-pane,
.answer-card {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 16px 18px;
}

.exam-header h1 {
  margin: 4px 0;
  color: #13233a;
  font-size: 24px;
}

.exam-header p,
.answer-summary {
  color: #667085;
}

.timer-box {
  min-width: 148px;
  padding: 12px 16px;
  border-radius: 8px;
  background: #eef6ff;
  color: #1f6feb;
  text-align: center;
}

.timer-box.urgent {
  background: #fff1f0;
  color: #cf1322;
}

.timer-box span {
  display: block;
  font-size: 12px;
}

.timer-box strong {
  display: block;
  margin-top: 4px;
  font-size: 28px;
  letter-spacing: 0;
}

.exam-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 16px;
  min-height: 0;
}

.question-pane {
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
}

.question-card {
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
}

.question-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #13233a;
}

.question-title strong {
  line-height: 1.7;
}

.answer-card {
  align-self: start;
  position: sticky;
  top: 0;
  display: grid;
  gap: 14px;
  max-height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.answer-card h2 {
  margin: 0;
  color: #13233a;
  font-size: 18px;
}

.answer-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.answer-grid button {
  height: 38px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  color: #344054;
  cursor: pointer;
}

.answer-grid button.answered {
  border-color: #1f8f99;
  background: #e7f7f8;
  color: #087982;
  font-weight: 700;
}

.answer-summary,
.answer-actions {
  display: grid;
  gap: 8px;
}

@media (max-width: 960px) {
  .exam-taking-page {
    height: auto;
    overflow: visible;
  }

  .exam-header,
  .exam-layout {
    grid-template-columns: 1fr;
  }

  .exam-header {
    flex-direction: column;
    align-items: stretch;
  }

  .answer-card {
    position: static;
  }
}
</style>
