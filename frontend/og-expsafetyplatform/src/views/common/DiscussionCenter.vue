<template>
  <div class="discussion-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Discussion</p>
        <h1>课程讨论与答疑</h1>
        <p class="page-desc">围绕课程和实验发布问题，查看教师回复与已解决问题。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">提问</el-button>
    </section>

    <section class="discussion-layout">
      <div class="panel">
        <div class="toolbar">
          <el-input v-model="filters.courseId" clearable placeholder="课程ID" />
          <el-input v-model="filters.experimentId" clearable placeholder="实验ID" />
          <el-select v-model="filters.status" clearable placeholder="状态">
            <el-option label="开放" value="OPEN" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
          <el-button :icon="Search" @click="loadTopics">查询</el-button>
        </div>

        <el-empty v-if="!loading && topics.length === 0" description="暂无讨论" />
        <div v-else v-loading="loading" class="topic-list">
          <article
            v-for="topic in topics"
            :key="topic.id"
            class="topic-card"
            :class="{ active: topic.id === current?.id }"
            @click="selectTopic(topic)"
          >
            <div class="topic-title">
              <h3>{{ topic.title }}</h3>
              <el-tag v-if="topic.isFeatured" type="warning">精华</el-tag>
            </div>
            <p>{{ topic.content }}</p>
            <div class="topic-meta">
              <span>{{ topic.userName }}</span>
              <span>{{ statusLabel(topic.status) }}</span>
              <span>{{ topic.replyCount || 0 }} 回复</span>
            </div>
          </article>
        </div>
      </div>

      <div class="panel detail-panel">
        <template v-if="current">
          <div class="detail-head">
            <div>
              <h2>{{ current.title }}</h2>
              <span>{{ current.userName }} · {{ current.createTime || '-' }}</span>
            </div>
            <div class="detail-actions" v-if="canManage">
              <el-button text type="warning" @click="toggleFeatured">{{ current.isFeatured ? '取消精华' : '设为精华' }}</el-button>
              <el-button text type="success" @click="setStatus('RESOLVED')">已解决</el-button>
              <el-button text type="info" @click="setStatus('CLOSED')">关闭</el-button>
            </div>
          </div>
          <p class="topic-content">{{ current.content }}</p>

          <div class="reply-list">
            <article v-for="reply in current.replies || []" :key="reply.id" class="reply-card" :class="{ teacher: reply.isTeacherReply }">
              <div>
                <strong>{{ reply.userName }}</strong>
                <el-tag v-if="reply.isTeacherReply" size="small" type="success">教师回复</el-tag>
                <span>{{ reply.createTime || '-' }}</span>
              </div>
              <p>{{ reply.content }}</p>
            </article>
            <el-empty v-if="(current.replies || []).length === 0" description="暂无回复" :image-size="80" />
          </div>

          <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="输入回复内容" />
          <div class="reply-actions">
            <el-button type="primary" :loading="saving" @click="submitReply">回复</el-button>
          </div>
        </template>
        <el-empty v-else description="请选择一个讨论主题" />
      </div>
    </section>

    <el-dialog v-model="createVisible" title="发布问题" width="620px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="课程ID" required><el-input v-model="createForm.courseId" /></el-form-item>
        <el-form-item label="实验ID"><el-input v-model="createForm.experimentId" /></el-form-item>
        <el-form-item label="标题" required><el-input v-model="createForm.title" /></el-form-item>
        <el-form-item label="内容" required><el-input v-model="createForm.content" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="匿名展示"><el-switch v-model="createForm.isAnonymous" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTopic">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/authStore'
import { createDiscussion, getDiscussionDetail, getDiscussions, replyDiscussion, updateDiscussionFeatured, updateDiscussionStatus } from '@/api/discussion'

const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const createVisible = ref(false)
const topics = ref([])
const current = ref(null)
const replyContent = ref('')
const filters = reactive({
  courseId: route.query.courseId || '',
  experimentId: route.query.experimentId || '',
  status: '',
})
const createForm = reactive({
  courseId: route.query.courseId || '',
  experimentId: route.query.experimentId || '',
  title: '',
  content: '',
  isAnonymous: false,
})

const canManage = computed(() => ['teacher', 'admin'].includes(authStore.role))

onMounted(async () => {
  await loadTopics()
  if (route.query.topicId) {
    await selectTopic({ id: Number(route.query.topicId) })
  }
})

async function loadTopics() {
  loading.value = true
  try {
    const result = await getDiscussions({
      pageNum: 1,
      pageSize: 30,
      courseId: filters.courseId || undefined,
      experimentId: filters.experimentId || undefined,
      status: filters.status || undefined,
    })
    topics.value = result?.records || []
    if (!current.value && topics.value.length) await selectTopic(topics.value[0])
  } finally {
    loading.value = false
  }
}

async function selectTopic(topic) {
  current.value = await getDiscussionDetail(topic.id)
  replyContent.value = ''
}

function openCreate() {
  Object.assign(createForm, {
    courseId: filters.courseId || route.query.courseId || '',
    experimentId: filters.experimentId || route.query.experimentId || '',
    title: '',
    content: '',
    isAnonymous: false,
  })
  createVisible.value = true
}

async function submitTopic() {
  if (!createForm.courseId || !createForm.title.trim() || !createForm.content.trim()) {
    ElMessage.warning('请填写课程ID、标题和内容')
    return
  }
  saving.value = true
  try {
    const result = await createDiscussion({
      courseId: Number(createForm.courseId),
      experimentId: createForm.experimentId ? Number(createForm.experimentId) : undefined,
      title: createForm.title.trim(),
      content: createForm.content.trim(),
      isAnonymous: createForm.isAnonymous ? 1 : 0,
    })
    createVisible.value = false
    await loadTopics()
    await selectTopic({ id: result.id })
  } finally {
    saving.value = false
  }
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  saving.value = true
  try {
    await replyDiscussion(current.value.id, { content: replyContent.value.trim() })
    await selectTopic(current.value)
  } finally {
    saving.value = false
  }
}

async function setStatus(status) {
  await updateDiscussionStatus(current.value.id, status)
  await selectTopic(current.value)
  await loadTopics()
}

async function toggleFeatured() {
  await updateDiscussionFeatured(current.value.id, current.value.isFeatured ? 0 : 1)
  await selectTopic(current.value)
  await loadTopics()
}

function statusLabel(status) {
  return { OPEN: '开放', RESOLVED: '已解决', CLOSED: '关闭' }[status] || status || '未知'
}
</script>

<style scoped>
.discussion-page { max-width: 1280px; margin: 0 auto; }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.discussion-layout { display: grid; grid-template-columns: 420px minmax(0, 1fr); gap: 16px; align-items: start; }
.panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.toolbar { display: grid; grid-template-columns: 1fr 1fr 1fr auto; gap: 8px; margin-bottom: 12px; }
.topic-list { display: grid; gap: 10px; min-height: 280px; }
.topic-card { border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; cursor: pointer; background: #fff; }
.topic-card.active { border-color: #409eff; background: #eef6ff; }
.topic-title, .detail-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.topic-card h3, .detail-head h2 { color: #13233a; font-size: 16px; margin-bottom: 6px; }
.topic-card p, .topic-content, .reply-card p { color: #344054; line-height: 1.7; white-space: pre-wrap; }
.topic-meta { display: flex; flex-wrap: wrap; gap: 10px; color: #98a2b3; font-size: 12px; margin-top: 8px; }
.detail-panel { min-height: 520px; }
.detail-head span { color: #98a2b3; font-size: 12px; }
.detail-actions { display: flex; flex-wrap: wrap; gap: 4px; }
.topic-content { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; margin: 12px 0; }
.reply-list { display: grid; gap: 10px; margin-bottom: 12px; }
.reply-card { border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.reply-card.teacher { background: #f0f9f2; border-color: #bfe8c7; }
.reply-card div { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 6px; }
.reply-card span { color: #98a2b3; font-size: 12px; }
.reply-actions { display: flex; justify-content: flex-end; margin-top: 10px; }
@media (max-width: 980px) { .discussion-layout { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .page-head, .topic-title, .detail-head { align-items: stretch; flex-direction: column; } .toolbar { grid-template-columns: 1fr; } }
</style>
