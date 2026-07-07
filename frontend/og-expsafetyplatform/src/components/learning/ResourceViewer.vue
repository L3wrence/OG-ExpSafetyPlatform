<template>
  <el-dialog v-model="visible" :title="preview?.title || '资源预览'" width="860px" destroy-on-close @closed="cleanup">
    <div v-loading="loading" class="resource-viewer">
      <template v-if="preview">
        <el-alert
          v-if="preview.invalidFlag"
          type="warning"
          :closable="false"
          title="该资源已标记失效，请联系教师更新。"
        />

        <div v-else class="preview-surface">
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
      </template>
      <el-empty v-else-if="!loading" description="资源暂不可预览" />
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button :loading="saving" @click="saveNote">保存笔记</el-button>
      <el-button type="primary" :loading="saving" @click="markComplete">确认完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Link as LinkIcon } from '@element-plus/icons-vue'
import { getResourceFileBlob, getResourcePreview, viewResource } from '@/api/resource'
import { updateLearningProgress } from '@/api/learningRecord'

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
const objectUrl = ref('')
const videoRef = ref(null)
let lastSavedAt = 0

const contentType = computed(() => String(preview.value?.contentType || '').toLowerCase())
const resourceType = computed(() => String(preview.value?.resourceType || '').toUpperCase())
const isExternal = computed(() => /^https?:\/\//.test(preview.value?.previewUrl || ''))
const isVideo = computed(() => contentType.value.startsWith('video/') || resourceType.value.includes('VIDEO'))
const isImage = computed(() => contentType.value.startsWith('image/') || ['IMAGE', 'JPG', 'PNG'].includes(resourceType.value))
const isPdf = computed(() => contentType.value.includes('pdf') || resourceType.value.includes('PDF') || resourceType.value.includes('DOCUMENT'))

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
    if (preview.value?.previewUrl && !isExternal.value) {
      const blob = await getResourceFileBlob(props.resourceId)
      objectUrl.value = URL.createObjectURL(blob)
    }
  } finally {
    loading.value = false
  }
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
  const now = Date.now()
  if (!force && now - lastSavedAt < 5000) return
  lastSavedAt = now
  const duration = videoRef.value.duration || 0
  const current = videoRef.value.currentTime || 0
  const progress = duration > 0 ? Math.min(100, Math.round((current / duration) * 100)) : 0
  await updateLearningProgress({
    resourceId: props.resourceId,
    progress,
    durationSeconds: 5,
    lastPositionSeconds: Math.round(current),
    note: note.value,
    finishFlag: progress >= 80 ? 1 : 0,
  })
  preview.value.progress = progress
  preview.value.lastPositionSeconds = Math.round(current)
  if (progress >= 80) emit('completed')
}

async function saveNote() {
  if (!preview.value) return
  saving.value = true
  try {
    await updateLearningProgress({
      resourceId: props.resourceId,
      progress: Number(preview.value.progress || 0),
      durationSeconds: 0,
      lastPositionSeconds: preview.value.lastPositionSeconds || 0,
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
      lastPositionSeconds: videoRef.value ? Math.round(videoRef.value.currentTime || 0) : Number(preview.value?.lastPositionSeconds || 0),
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

function openExternal() {
  window.open(preview.value?.previewUrl, '_blank', 'noopener,noreferrer')
}

function cleanup() {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
  objectUrl.value = ''
  preview.value = null
  note.value = ''
  lastSavedAt = 0
}
</script>

<style scoped>
.resource-viewer { min-height: 360px; }
.preview-surface { background: #0f172a; border-radius: 8px; overflow: hidden; min-height: 360px; display: flex; align-items: center; justify-content: center; margin-bottom: 12px; }
.media { width: 100%; max-height: 520px; display: block; }
.video { background: #000; }
.image { object-fit: contain; background: #fff; }
.document { height: 520px; border: 0; background: #fff; }
.link-box { color: #344054; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 24px; text-align: center; }
.viewer-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; color: #667085; margin-bottom: 12px; }
</style>
