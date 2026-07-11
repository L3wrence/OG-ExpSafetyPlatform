<template>
  <div class="exam-fullscreen" @contextmenu.prevent>
    <header class="exam-topbar">
      <div class="topbar-left">
        <button class="back-top-btn" type="button" @click="handleBack">返回</button>
        <span class="topbar-title">{{ paperTitle }}</span>
      </div>
      <div class="topbar-right">
        <div class="countdown" :class="{ warning: remaining < 300 }">
          <el-icon><Clock /></el-icon>
          <span class="time-text">{{ formatTime(remaining) }}</span>
        </div>
        <button class="submit-top-btn" type="button" @click="handleSubmit">交卷</button>
      </div>
    </header>

    <div class="exam-main">
      <div class="question-panel">
        <div class="q-tags" v-if="currentQuestion">
          <span class="q-type-tag">{{ typeLabel(currentQuestion.type) }}</span>
          <span class="q-score-tag">{{ currentQuestion.score || 0 }}分</span>
          <span class="q-num-tag">第 {{ currentIndex + 1 }}/{{ totalCount }} 题</span>
        </div>
        <div class="q-content" v-if="currentQuestion">
          <p class="q-stem">{{ currentQuestion.content }}</p>
        </div>
        <div class="q-options" v-if="currentQuestion && hasOptions(currentQuestion.type)">
          <div v-for="opt in normalizedOptions(currentQuestion)" :key="opt.value"
            class="opt-item" :class="{ selected: isOptionSelected(currentQuestion, opt.value) }"
            @click="toggleOption(currentQuestion, opt.value)">
            <span class="opt-letter">{{ opt.value }}</span>
            <span class="opt-text">{{ opt.label }}</span>
          </div>
        </div>
        <div class="q-textarea" v-else-if="currentQuestion">
          <el-input
            v-model="answers[currentQuestion.id]"
            type="textarea"
            :rows="6"
            placeholder="请输入答案..."
          />
        </div>
      </div>

      <aside class="answer-sheet">
        <div class="sheet-top"><h3>答题卡</h3><span class="sheet-count">{{ answeredCount }}/{{ totalCount }}</span></div>
        <div class="sheet-grid">
          <button v-for="(q, i) in questions" :key="q.id" class="sheet-btn" type="button"
            :class="{ current: i === currentIndex, done: hasAnswer(q.id) }"
            @click="currentIndex = i">{{ i + 1 }}</button>
        </div>
        <div class="sheet-legend">
          <span><i class="dot current-dot"></i>当前</span>
          <span><i class="dot done-dot"></i>已答</span>
          <span><i class="dot undot"></i>未答</span>
        </div>
      </aside>
    </div>

    <footer class="exam-bottombar">
      <button class="nav-btn" type="button" :disabled="currentIndex === 0" @click="currentIndex--">上一题</button>
      <div class="nav-dots">
        <span v-for="(q, i) in questions" :key="q.id" class="nav-dot"
          :class="{ active: i === currentIndex, done: hasAnswer(q.id) }" @click="currentIndex = i"></span>
      </div>
      <button class="nav-btn" type="button" :disabled="currentIndex >= totalCount - 1" @click="currentIndex++">下一题</button>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'
import { getInProgressExam, saveExamAnswers, startExam, submitExam } from '@/api/exam'

const route = useRoute()
const router = useRouter()
const courseId = computed(() => Number(route.params.courseId || route.query.courseId))
const paperId = computed(() => Number(route.params.paperId || route.query.paperId))
const isContinue = computed(() => route.query.isContinue === '1')

const paperTitle = ref('安全知识考核')
const questions = ref([])
const answers = reactive({})
const currentIndex = ref(0)
const remaining = ref(0)
const recordId = ref(null)
const submitting = ref(false)
let timer = null
let autoSaveTimer = null

const totalCount = computed(() => questions.value.length)
const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const answeredCount = computed(() => questions.value.filter(q => hasAnswer(q.id)).length)

function hasAnswer(qid) { const v = answers[qid]; return v !== undefined && v !== '' && (!Array.isArray(v) || v.length > 0) }
function hasOptions(type) { return ['SINGLE', 'MULTIPLE', 'JUDGE'].includes(type) }
function typeLabel(t) { return { SINGLE: '单选题', MULTIPLE: '多选题', JUDGE: '判断题', SHORT_ANSWER: '简答题' }[t] || t }

function normalizedOptions(q) {
  const opts = q.options
  if (q.type === 'JUDGE' && !opts) return [{ value: 'TRUE', label: '正确' }, { value: 'FALSE', label: '错误' }]
  if (!opts) return []
  if (Array.isArray(opts)) return opts.map((o, i) => optionFromValue(o, i))
  try {
    const parsed = typeof opts === 'string' ? JSON.parse(opts) : opts
    if (Array.isArray(parsed)) return parsed.map((o, i) => optionFromValue(o, i))
    return Object.entries(parsed || {}).map(([k, v]) => ({ value: k, label: v }))
  } catch { return [] }
}

function optionFromValue(item, index) {
  if (typeof item === 'object') {
    return {
      value: item.value || item.key || String.fromCharCode(65 + index),
      label: item.label || item.text || item.content || '',
    }
  }
  const value = String(item).match(/^([A-Z])[\.\、:：]/)?.[1] || String.fromCharCode(65 + index)
  return { value, label: String(item) }
}

function isOptionSelected(q, val) {
  if (['SINGLE', 'JUDGE'].includes(q.type)) return answers[q.id] === val
  return Array.isArray(answers[q.id]) && answers[q.id].includes(val)
}

function toggleOption(q, val) {
  if (['SINGLE', 'JUDGE'].includes(q.type)) {
    answers[q.id] = val
    autoSave()
    return
  }
  if (!Array.isArray(answers[q.id])) answers[q.id] = []
  const idx = answers[q.id].indexOf(val)
  idx >= 0 ? answers[q.id].splice(idx, 1) : answers[q.id].push(val)
  autoSave()
}

function formatTime(sec) {
  const m = String(Math.floor(sec / 60)).padStart(2, '0')
  const s = String(sec % 60).padStart(2, '0')
  return `${m}:${s}`
}

function buildAnswerPayload() {
  return questions.value.map(q => {
    const a = answers[q.id]
    return { questionId: q.id, answer: Array.isArray(a) ? a.join(',') : String(a || '') }
  })
}

async function autoSave() {
  if (!recordId.value) return
  try { await saveExamAnswers(recordId.value, { answers: buildAnswerPayload() }) } catch { /* silent */ }
}

async function loadExam() {
  try {
    let res
    if (isContinue.value) {
      res = await getInProgressExam({ paperId: paperId.value })
    } else {
      res = await startExam(paperId.value)
    }
    if (!res) { ElMessage.error('无法加载试卷'); router.back(); return }
    recordId.value = res.recordId
    paperTitle.value = res.paperTitle || '安全知识考核'
    questions.value = res.questions || []
    questions.value.forEach(q => {
      const saved = res?.answers?.[q.id] ?? res?.answers?.[String(q.id)]
      answers[q.id] = q.type === 'MULTIPLE' ? String(saved || '').split(',').filter(Boolean) : (saved || '')
    })
    const end = new Date(res.endTime).getTime()
    remaining.value = Math.max(0, Math.floor((end - Date.now()) / 1000))
    timer = setInterval(() => {
      remaining.value = Math.max(0, Math.floor((end - Date.now()) / 1000))
      if (remaining.value <= 0) { clearInterval(timer); autoSubmit() }
    }, 1000)
    autoSaveTimer = setInterval(autoSave, 30000)
  } catch { ElMessage.error('加载试卷失败'); router.back() }
}

async function handleSubmit() {
  const undone = totalCount.value - answeredCount.value
  const msg = undone > 0 ? `还有 ${undone} 道题未作答，确定交卷吗？` : '确定提交试卷？'
  try { await ElMessageBox.confirm(msg, '确认交卷', { confirmButtonText: '确定交卷', cancelButtonText: '继续检查', type: 'warning' }); await doSubmit() } catch { }
}
async function handleBack() {
  try {
    await ElMessageBox.confirm('返回后会自动保存答案，但时间不会暂停，确认返回吗？', '返回考试列表', {
      confirmButtonText: '确认返回',
      cancelButtonText: '继续答题',
      type: 'warning',
    })
    await autoSave()
    goBackToExamRecords()
  } catch { }
}
async function autoSubmit() { ElMessage.warning('考试时间已到，自动交卷'); await doSubmit(true) }
async function doSubmit(autoSubmit = false) {
  if (submitting.value) return; submitting.value = true
  try {
    clearInterval(timer); clearInterval(autoSaveTimer)
    const res = await submitExam(recordId.value, { answers: buildAnswerPayload(), autoSubmit })
    const pendingReview = res?.status === 'PENDING_REVIEW' || res?.passed === null || res?.passed === undefined
    const resultText = pendingReview ? '主观题待教师批改，最终总分与是否通过将在批改后更新' : `得分：${res?.totalScore ?? '-'}  ${res?.passed ? '通过' : '未通过'}`
    ElMessageBox.alert(resultText, '交卷成功', {
      type: res?.passed ? 'success' : 'warning',
      callback: goBackToExamRecords,
    })
  } catch { submitting.value = false }
}

function goBackToExamRecords() {
  if (courseId.value) {
    router.push(`/classrooms/${courseId.value}/learn?module=exam&examTab=records`)
  } else {
    router.back()
  }
}

function onKeydown(e) {
  if (e.key === 'ArrowLeft' && currentIndex.value > 0) currentIndex.value--
  if (e.key === 'ArrowRight' && currentIndex.value < totalCount.value - 1) currentIndex.value++
  if (['a','A','b','B','c','C','d','D'].includes(e.key)) { e.preventDefault(); const q = currentQuestion.value; if (q && hasOptions(q.type)) toggleOption(q, e.key.toUpperCase()) }
}

let visibilityWarnings = 0
function onVisibilityChange() {
  if (document.hidden) { visibilityWarnings++; if (visibilityWarnings <= 3) ElMessage.warning(`切屏警告 (${visibilityWarnings}/3)`); else { ElMessage.error('切屏超限，自动交卷'); autoSubmit() } }
}

onMounted(() => {
  loadExam(); document.body.style.overflow = 'hidden'
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('visibilitychange', onVisibilityChange)
})
onBeforeUnmount(() => {
  clearInterval(timer); clearInterval(autoSaveTimer); document.body.style.overflow = ''
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.exam-fullscreen { position: fixed; inset: 0; z-index: 1000; background: #F0F2F5; display: flex; flex-direction: column; font-family: 'PingFang SC','Microsoft YaHei',sans-serif; }
.exam-topbar { height: 50px; flex-shrink: 0; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; background: #7B2D3B; color: #fff; }
.topbar-left { display: flex; align-items: center; gap: 14px; min-width: 0; }
.topbar-title { font-size: 15px; font-weight: 500; }
.topbar-right { display: flex; align-items: center; gap: 20px; }
.countdown { display: flex; align-items: center; gap: 6px; font-size: 15px; }
.countdown.warning .time-text { color: #FF6B6B; animation: blink 1s infinite; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0.4} }
.time-text { font-variant-numeric: tabular-nums; font-weight: 600; min-width: 50px; }
.submit-top-btn { padding: 6px 20px; border: 1px solid rgba(255,255,255,0.3); background: transparent; color: #fff; border-radius: 4px; cursor: pointer; font-size: 13px; }
.back-top-btn { padding: 6px 12px; border: 1px solid rgba(255,255,255,0.3); background: rgba(255,255,255,0.08); color: #fff; border-radius: 4px; cursor: pointer; font-size: 13px; }
.submit-top-btn:hover, .back-top-btn:hover { background: rgba(255,255,255,0.16); }

.exam-main { flex: 1; min-height: 0; display: flex; gap: 0; overflow: hidden; }
.question-panel { flex: 1; min-width: 0; overflow-y: auto; padding: 28px 32px; display: flex; flex-direction: column; gap: 20px; }
.q-tags { display: flex; gap: 10px; }
.q-type-tag { font-size: 12px; background: #7B2D3B; color: #fff; padding: 4px 12px; border-radius: 3px; }
.q-score-tag { font-size: 12px; background: #F3E4E6; color: #7B2D3B; padding: 4px 12px; border-radius: 3px; }
.q-num-tag { font-size: 12px; color: #999; padding: 4px 0; }
.q-stem { font-size: 16px; line-height: 1.8; color: #222; }
.q-options { display: flex; flex-direction: column; gap: 10px; }
.opt-item { display: flex; align-items: center; gap: 14px; padding: 14px 18px; background: #fff; border: 1px solid #E5E5E5; border-radius: 6px; cursor: pointer; transition: all 0.15s; }
.opt-item:hover { border-color: #7B2D3B; background: #FDF8F9; }
.opt-item.selected { border-color: #7B2D3B; background: #F3E4E6; }
.opt-letter { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; border: 1px solid #ccc; border-radius: 50%; font-size: 14px; font-weight: 600; color: #666; flex-shrink: 0; }
.opt-item.selected .opt-letter { border-color: #7B2D3B; background: #7B2D3B; color: #fff; }
.opt-text { font-size: 15px; color: #333; line-height: 1.5; }
.q-textarea { background: #fff; padding: 16px; border-radius: 6px; }

.answer-sheet { width: 240px; flex-shrink: 0; background: #fff; border-left: 1px solid #E5E5E5; display: flex; flex-direction: column; padding: 20px; }
.sheet-top { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 16px; }
.sheet-top h3 { font-size: 15px; color: #222; }
.sheet-count { font-size: 20px; font-weight: 700; color: #7B2D3B; }
.sheet-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.sheet-btn { width: 100%; aspect-ratio: 1; display: flex; align-items: center; justify-content: center; border: 1px solid #ddd; border-radius: 4px; background: #fff; font-size: 13px; font-weight: 600; color: #666; cursor: pointer; }
.sheet-btn:hover { border-color: #7B2D3B; }
.sheet-btn.current { border-color: #7B2D3B; box-shadow: 0 0 0 2px rgba(123,45,59,0.3); }
.sheet-btn.done { background: #7B2D3B; border-color: #7B2D3B; color: #fff; }
.sheet-btn.current.done { background: #8E3E4C; }
.sheet-legend { display: flex; gap: 14px; margin-top: 16px; font-size: 12px; color: #999; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 4px; vertical-align: middle; }
.current-dot { border: 1px solid #7B2D3B; box-shadow: 0 0 0 1px rgba(123,45,59,0.2); }
.done-dot { background: #7B2D3B; }
.undot { border: 1px solid #ddd; }

.exam-bottombar { height: 52px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; gap: 20px; background: #fff; border-top: 1px solid #E5E5E5; padding: 0 24px; }
.nav-btn { padding: 6px 18px; border: 1px solid #ddd; background: #fff; border-radius: 4px; cursor: pointer; font-size: 13px; color: #444; }
.nav-btn:hover:not(:disabled) { border-color: #7B2D3B; color: #7B2D3B; }
.nav-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.nav-dots { display: flex; gap: 6px; overflow-x: auto; max-width: 60vw; padding: 0 10px; }
.nav-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; background: #ddd; cursor: pointer; transition: all 0.15s; }
.nav-dot.active { background: #7B2D3B; transform: scale(1.3); }
.nav-dot.done { background: #B86E78; }

@media (max-width: 768px) { .answer-sheet { display: none; } .question-panel { padding: 20px 16px; } .exam-topbar { padding: 0 12px; } }
</style>
