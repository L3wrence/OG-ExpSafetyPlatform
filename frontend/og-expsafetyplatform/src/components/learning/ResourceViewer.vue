<template>
  <el-dialog v-model="visible" :title="preview?.title || '资源预览'" width="1080px" destroy-on-close @closed="cleanup">
    <div v-loading="loading" class="resource-viewer">
      <template v-if="preview">
        <el-alert
          v-if="preview.invalidFlag"
          type="warning"
          :closable="false"
          title="该资源已标记失效，请联系教师更新。"
        />

        <div v-else class="viewer-grid">
          <section class="preview-column">
            <div class="preview-surface">
              <video
                v-if="isVideo"
                ref="videoRef"
                class="media video"
                controls
                :src="objectUrl || preview.previewUrl"
                @loadedmetadata="restoreVideoPosition"
                @timeupdate="saveVideoProgress"
                @pause="saveVideoProgress(true)"
              />
              <img v-else-if="isImage" class="media image" :src="objectUrl || preview.previewUrl" :alt="preview.title" />
              <iframe v-else-if="isPdf" class="media document" :src="objectUrl || preview.previewUrl" title="资源文档预览" />
              <div v-else-if="isExternal" class="link-box">
                <el-button type="primary" :icon="LinkIcon" @click="openExternal">打开外部资源</el-button>
              </div>
              <div v-else class="link-box">
                <p>当前文件类型暂不支持内嵌预览，可下载或在新窗口查看。</p>
                <el-button type="primary" :icon="Download" @click="openExternal">查看资源</el-button>
              </div>
            </div>

            <div class="viewer-meta">
              <el-tag :type="preview.progress >= 100 ? 'success' : 'info'">进度 {{ Math.round(Number(preview.progress || 0)) }}%</el-tag>
              <el-tag v-if="preview.knowledgePoint" type="primary">{{ preview.knowledgePoint }}</el-tag>
              <el-tag v-if="preview.riskType" type="warning">{{ preview.riskType }}</el-tag>
              <span>{{ preview.resourceType || 'RESOURCE' }}</span>
              <span v-if="preview.originalFilename">{{ preview.originalFilename }}</span>
            </div>

            <el-input
              v-model="note"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              placeholder="记录本资源的实验要点、风险提示或疑问"
            />
          </section>

          <aside class="insight-panel">
            <section>
              <h3>为什么学</h3>
              <p>{{ preview.aiSummary || `围绕${preview.experimentName || '当前实验'}理解资源中的工程操作、风险和报告依据。` }}</p>
              <div class="completion-box">
                <b>完成条件</b>
                <span>{{ completionText }}</span>
              </div>
            </section>

            <section>
              <div class="section-head">
                <h3>时间点问题</h3>
                <el-tag size="small">{{ formatTime(currentPosition) }}</el-tag>
              </div>
              <el-segmented v-model="timelineForm.noteType" :options="timelineTypeOptions" />
              <el-input
                v-model="timelineForm.content"
                type="textarea"
                :rows="3"
                maxlength="1000"
                show-word-limit
                placeholder="在当前秒点记录看不懂的问题、设备细节或风险提醒"
              />
              <div class="note-actions">
                <el-select v-model="timelineForm.visibility" size="small">
                  <el-option label="仅自己可见" value="PRIVATE" />
                  <el-option label="课程内可见" value="COURSE" />
                </el-select>
                <el-button type="primary" :loading="noteSaving" @click="saveTimelineNote">记录</el-button>
              </div>
              <el-empty v-if="timelineNotes.length === 0" description="暂无时间点记录" :image-size="64" />
              <div v-else class="timeline-notes">
                <button v-for="item in timelineNotes" :key="item.id" type="button" @click="seekTo(item.positionSeconds)">
                  <span>{{ formatTime(item.positionSeconds) }}</span>
                  <b>{{ typeLabel(item.noteType) }}</b>
                  <em>{{ item.content }}</em>
                </button>
              </div>
            </section>

            <section>
              <div class="section-head">
                <h3>AI 一句话解释</h3>
                <el-button text type="primary" :icon="MagicStick" :loading="aiLoading" @click="askResourceAi">解释当前资源</el-button>
              </div>
              <p class="ai-answer">{{ aiAnswer || '可让 AI 用一句话解释当前知识点或风险点，回答仅作学习参考。' }}</p>
            </section>
          </aside>
        </div>
      </template>
      <el-empty v-else-if="!loading" description="资源暂不可预览" />
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button :loading="saving" @click="saveNote">保存总笔记</el-button>
      <el-button type="primary" :loading="saving" @click="markComplete">确认完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Link as LinkIcon, MagicStick } from '@element-plus/icons-vue'
import {
  createResourceTimelineNote,
  getResourceFileBlob,
  getResourcePreview,
  getResourceTimelineNotes,
  viewResource,
} from '@/api/resource'
import { updateLearningProgress } from '@/api/learningRecord'
import { askAi } from '@/api/ai'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  resourceId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue', 'completed'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const preview = ref(null)
const note = ref('')
const loading = ref(false)
const saving = ref(false)
const noteSaving = ref(false)
const aiLoading = ref(false)
const objectUrl = ref('')
const videoRef = ref(null)
const currentPosition = ref(0)
const timelineNotes = ref([])
const aiAnswer = ref('')
const timelineForm = reactive({ noteType: 'NOTE', visibility: 'PRIVATE', content: '' })
let lastSavedAt = 0

const timelineTypeOptions = [
  { label: '笔记', value: 'NOTE' },
  { label: '问题', value: 'QUESTION' },
  { label: '风险', value: 'RISK' },
]
const contentType = computed(() => String(preview.value?.contentType || '').toLowerCase())
const resourceType = computed(() => String(preview.value?.resourceType || '').toUpperCase())
const isExternal = computed(() => /^https?:\/\//.test(preview.value?.previewUrl || ''))
const isVideo = computed(() => contentType.value.startsWith('video/') || resourceType.value.includes('VIDEO'))
const isImage = computed(() => contentType.value.startsWith('image/') || ['IMAGE', 'JPG', 'PNG'].includes(resourceType.value))
const isPdf = computed(() => contentType.value.includes('pdf') || resourceType.value.includes('PDF') || resourceType.value.includes('DOCUMENT'))
const completionText = computed(() => {
  const rule = preview.value?.completionRule || 'CONFIRM'
  if (rule === 'PROGRESS') {
    return `学习进度达到 ${preview.value?.minProgress || 80}% 后自动完成`
  }
  if (preview.value?.minStudySeconds) {
    return `至少学习 ${preview.value.minStudySeconds} 秒后确认完成`
  }
  return '阅读或观看后点击确认完成'
})

watch(() => [visible.value, props.resourceId], async ([open]) => {
  if (open && props.resourceId) await loadPreview()
})

async function loadPreview() {
  loading.value = true
  cleanup()
  try {
    await viewResource(props.resourceId)
    preview.value = await getResourcePreview(props.resourceId)
    note.value = preview.value?.note || ''
    currentPosition.value = Number(preview.value?.lastPositionSeconds || 0)
    await loadTimelineNotes()
    if (preview.value?.previewUrl && !isExternal.value) {
      const blob = await getResourceFileBlob(props.resourceId)
      objectUrl.value = URL.createObjectURL(blob)
    }
  } finally {
    loading.value = false
  }
}

async function loadTimelineNotes() {
  timelineNotes.value = await getResourceTimelineNotes(props.resourceId)
}

function restoreVideoPosition() {
  nextTick(() => {
    if (videoRef.value && preview.value?.lastPositionSeconds) {
      videoRef.value.currentTime = Number(preview.value.lastPositionSeconds || 0)
    }
  })
}

async function saveVideoProgress(force = false) {
  if (!videoRef.value || !preview.value) return
  currentPosition.value = Math.round(videoRef.value.currentTime || 0)
  const now = Date.now()
  if (!force && now - lastSavedAt < 5000) return
  lastSavedAt = now
  const duration = videoRef.value.duration || 0
  const progress = duration > 0 ? Math.min(100, Math.round((currentPosition.value / duration) * 100)) : 0
  await updateLearningProgress({
    resourceId: props.resourceId,
    progress,
    durationSeconds: 5,
    lastPositionSeconds: currentPosition.value,
    note: note.value,
    finishFlag: progress >= Number(preview.value.minProgress || 80) ? 1 : 0,
  })
  preview.value.progress = progress
  preview.value.lastPositionSeconds = currentPosition.value
  if (progress >= Number(preview.value.minProgress || 80)) emit('completed')
}

async function saveTimelineNote() {
  if (!timelineForm.content.trim()) {
    ElMessage.warning('请先填写时间点内容')
    return
  }
  noteSaving.value = true
  try {
    await createResourceTimelineNote(props.resourceId, {
      experimentId: preview.value?.experimentId,
      positionSeconds: currentPosition.value,
      noteType: timelineForm.noteType,
      visibility: timelineForm.visibility,
      content: timelineForm.content.trim(),
    })
    timelineForm.content = ''
    await loadTimelineNotes()
    ElMessage.success('时间点已记录')
  } finally {
    noteSaving.value = false
  }
}

async function askResourceAi() {
  aiLoading.value = true
  try {
    const answer = await askAi({
      scene: 'SAFETY_QA',
      experimentId: preview.value?.experimentId,
      question: `请用一句话解释资源「${preview.value?.title}」中的${preview.value?.knowledgePoint || '关键知识点'}，并说明和${preview.value?.riskType || '实验风险'}的关系。`,
    })
    aiAnswer.value = typeof answer === 'string' ? answer : (answer?.answer || answer?.content || '暂未生成解释')
  } finally {
    aiLoading.value = false
  }
}

async function saveNote() {
  if (!preview.value) return
  saving.value = true
  try {
    await updateLearningProgress({
      resourceId: props.resourceId,
      progress: Number(preview.value.progress || 0),
      durationSeconds: 0,
      lastPositionSeconds: preview.value.lastPositionSeconds || currentPosition.value,
      note: note.value,
    })
    ElMessage.success('笔记已保存')
  } finally {
    saving.value = false
  }
}

async function markComplete() {
  saving.value = true
  try {
    await updateLearningProgress({
      resourceId: props.resourceId,
      progress: 100,
      durationSeconds: isVideo.value ? 0 : 60,
      lastPositionSeconds: videoRef.value ? Math.round(videoRef.value.currentTime || 0) : Number(preview.value?.lastPositionSeconds || currentPosition.value),
      note: note.value,
      finishFlag: 1,
    })
    ElMessage.success('学习进度已更新')
    emit('completed')
    visible.value = false
  } finally {
    saving.value = false
  }
}

function seekTo(seconds) {
  if (videoRef.value) {
    videoRef.value.currentTime = Number(seconds || 0)
    videoRef.value.play?.()
  }
}

function openExternal() {
  window.open(preview.value?.previewUrl, '_blank', 'noopener,noreferrer')
}

function typeLabel(type) {
  return { NOTE: '笔记', QUESTION: '问题', RISK: '风险' }[type] || '记录'
}

function formatTime(seconds) {
  const value = Math.max(0, Number(seconds || 0))
  const minute = Math.floor(value / 60)
  const second = value % 60
  return `${minute}:${String(second).padStart(2, '0')}`
}

function cleanup() {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
  objectUrl.value = ''
  preview.value = null
  note.value = ''
  timelineNotes.value = []
  aiAnswer.value = ''
  currentPosition.value = 0
  timelineForm.noteType = 'NOTE'
  timelineForm.visibility = 'PRIVATE'
  timelineForm.content = ''
  lastSavedAt = 0
}
</script>

<style scoped>
.resource-viewer { min-height: 420px; }
.viewer-grid { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 16px; align-items: start; }
.preview-surface { background: #0f172a; border-radius: 8px; overflow: hidden; min-height: 420px; display: flex; align-items: center; justify-content: center; margin-bottom: 12px; }
.media { width: 100%; max-height: 560px; display: block; }
.video { background: #000; }
.image { object-fit: contain; background: #fff; }
.document { height: 560px; border: 0; background: #fff; }
.link-box { color: #344054; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 24px; text-align: center; }
.viewer-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; color: #667085; margin-bottom: 12px; }
.insight-panel { display: grid; gap: 12px; }
.insight-panel section { border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; background: #fff; }
.insight-panel h3 { color: #13233a; font-size: 15px; margin-bottom: 8px; }
.insight-panel p, .completion-box span, .timeline-notes em { color: #667085; line-height: 1.6; margin: 0; }
.completion-box { background: #f8fafc; border-radius: 8px; padding: 10px; margin-top: 10px; }
.completion-box b { display: block; color: #13233a; margin-bottom: 4px; }
.section-head, .note-actions { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
.note-actions { margin-top: 8px; }
.timeline-notes { display: grid; gap: 8px; margin-top: 10px; max-height: 220px; overflow: auto; }
.timeline-notes button { text-align: left; border: 1px solid #edf1f5; border-radius: 8px; background: #fbfcfe; padding: 8px; cursor: pointer; }
.timeline-notes button:hover { border-color: #9ec5fe; }
.timeline-notes span { color: #1f6feb; font-weight: 700; margin-right: 8px; }
.timeline-notes b { color: #13233a; margin-right: 8px; }
.timeline-notes em { display: block; font-style: normal; margin-top: 4px; }
.ai-answer { background: #f6f9ff; border-radius: 8px; padding: 10px; }
@media (max-width: 900px) {
  .viewer-grid { grid-template-columns: 1fr; }
  .preview-surface { min-height: 300px; }
  .document { height: 420px; }
}
</style>
