<template>
  <div class="portal-page" v-loading="loading">
    <section class="amazing-hero" :style="{ backgroundImage: `url(${labHero})` }">
      <div class="hero-copy">
        <p>油气工程实验教学与考核平台</p>
        <h1>{{ roleTitle }}</h1>
        <span>{{ subtitle }}</span>
        <div class="hero-actions">
          <el-button v-if="isLearner" type="primary" :icon="Reading" @click="go('/classrooms')">进入我的课堂</el-button>
          <el-button v-if="isLearner" :icon="Folder" @click="go('/resources')">浏览资源学习</el-button>
          <el-button v-if="canManageClassroom" type="primary" :icon="Operation" @click="go('/teacher/courses')">管理我的课堂</el-button>
        </div>
      </div>
    </section>

    <section class="portal-toolbar">
      <div>
        <h1>知识检索</h1>
        <p>搜索课程、实验、资源和安全知识，把学习任务直接接到当前路径。</p>
      </div>
      <div class="search-box">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索课程、实验、资源、安全知识"
          :prefix-icon="Search"
          @keyup.enter="doSearch"
        />
        <el-button type="primary" :icon="Search" :loading="searching" @click="doSearch">搜索</el-button>
      </div>
    </section>

    <section v-if="isLearner" class="student-priority-grid">
      <el-card shadow="never" class="panel todo-panel">
        <template #header>
          <div class="panel-header">
            <span>今日待办</span>
            <el-button text :icon="Refresh" @click="loadHome">刷新</el-button>
          </div>
        </template>
        <div v-if="home.todos.length" class="item-list">
          <button v-for="item in home.todos" :key="`${item.type}-${item.id}`" class="list-item" type="button" @click="go(item.path)">
            <el-tag size="small" :type="tagType(item.type)">{{ typeLabel(item.type) }}</el-tag>
            <span>{{ item.title }}</span>
            <time>{{ formatTime(item.time) }}</time>
          </button>
        </div>
        <el-empty v-else description="暂无待办" :image-size="80" />
      </el-card>

      <el-card shadow="never" class="panel continue-panel">
        <template #header><div class="panel-header"><span>继续学习</span></div></template>
        <button v-if="continueItem" class="continue-card" type="button" @click="go(continueItem.path)">
          <strong>{{ continueItem.title }}</strong>
          <span>{{ continueItem.status === 'OVERDUE' ? '任务已逾期' : '定位到最近未完成任务' }}</span>
        </button>
        <el-empty v-else description="暂无未完成学习任务" :image-size="80" />
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header><div class="panel-header"><span>即将截止</span></div></template>
        <div v-if="deadlineItems.length" class="item-list">
          <button v-for="item in deadlineItems" :key="`${item.type}-${item.id}`" class="list-item" type="button" @click="go(item.path)">
            <el-tag size="small" type="warning">{{ typeLabel(item.type) }}</el-tag>
            <span>{{ item.title }}</span>
            <time>{{ formatTime(item.time) }}</time>
          </button>
        </div>
        <el-empty v-else description="暂无临近截止事项" :image-size="80" />
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header><div class="panel-header"><span>准入和预约提醒</span></div></template>
        <div v-if="admissionItems.length" class="item-list">
          <button v-for="item in admissionItems" :key="`${item.type}-${item.id}`" class="list-item" type="button" @click="go(item.path)">
            <el-tag size="small" :type="tagType(item.type)">{{ typeLabel(item.type) }}</el-tag>
            <span>{{ item.title }}</span>
            <time>{{ formatTime(item.time) }}</time>
          </button>
        </div>
        <el-empty v-else description="暂无准入或预约提醒" :image-size="80" />
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header><div class="panel-header"><span>我的课程进度</span></div></template>
        <div class="student-metrics">
          <button v-for="metric in home.metrics" :key="metric.code" type="button" @click="go(metric.path)">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}<small>{{ metric.unit }}</small></strong>
          </button>
        </div>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header><div class="panel-header"><span>个性化学习建议</span></div></template>
        <div class="advice-list">
          <button type="button" @click="go('/classrooms')">从课堂学习页继续完成必做资源和准备清单</button>
          <button type="button" @click="go('/student/exams')">查看正式安全考试状态，未通过实验优先复习错题</button>
          <button type="button" @click="go('/student/grades')">报告被退回时先处理教师反馈再重新提交</button>
        </div>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header><div class="panel-header"><span>消息与日程</span></div></template>
        <div v-if="home.calendarEvents.length" class="timeline compact">
          <button v-for="event in home.calendarEvents" :key="`${event.type}-${event.id}`" type="button" @click="go(event.path)">
            <span class="timeline-dot" :class="event.type"></span>
            <span class="timeline-main">{{ event.title }}</span>
            <time>{{ formatTime(event.startTime) }}</time>
          </button>
        </div>
        <el-empty v-else description="暂无日程" :image-size="80" />
      </el-card>
    </section>

    <section v-if="!isLearner" class="metric-grid">
      <button
        v-for="metric in home.metrics"
        :key="metric.code"
        class="metric-card"
        :class="metric.type"
        type="button"
        @click="go(metric.path)"
      >
        <span class="metric-label">{{ metric.label }}</span>
        <strong>{{ metric.value }}<small>{{ metric.unit }}</small></strong>
      </button>
    </section>

    <section v-if="!isLearner" class="content-grid">
      <el-card shadow="never" class="panel todo-panel">
        <template #header>
          <div class="panel-header">
            <span>待办事项</span>
            <el-button text :icon="Refresh" @click="loadHome">刷新</el-button>
          </div>
        </template>
        <div v-if="home.todos.length" class="item-list">
          <button v-for="item in home.todos" :key="`${item.type}-${item.id}`" class="list-item" type="button" @click="go(item.path)">
            <el-tag size="small" :type="tagType(item.type)">{{ item.type || '待办' }}</el-tag>
            <span>{{ item.title }}</span>
            <time>{{ formatTime(item.time) }}</time>
          </button>
        </div>
        <el-empty v-else description="暂无待办" :image-size="80" />
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <div class="panel-header">
            <span>公告与消息</span>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" />
          </div>
        </template>
        <el-tabs v-model="messageTab">
          <el-tab-pane label="公告" name="notices">
            <div v-if="home.notices.length" class="item-list">
              <div v-for="item in home.notices" :key="item.id" class="list-item static">
                <el-tag size="small" :type="noticeTag(item.status)">{{ noticeLabel(item.status) }}</el-tag>
                <span>{{ item.title }}</span>
                <time>{{ formatTime(item.time) }}</time>
              </div>
            </div>
            <el-empty v-else description="暂无公告" :image-size="80" />
          </el-tab-pane>
          <el-tab-pane label="消息" name="messages">
            <div v-if="home.messages.length" class="item-list">
              <button
                v-for="item in home.messages"
                :key="item.id"
                class="list-item"
                :class="{ unread: Number(item.value) === 0 }"
                type="button"
                @click="readMessage(item)"
              >
                <el-tag size="small" :type="Number(item.value) === 0 ? 'warning' : 'info'">
                  {{ Number(item.value) === 0 ? '未读' : '已读' }}
                </el-tag>
                <span>{{ item.title }}</span>
                <time>{{ formatTime(item.time) }}</time>
              </button>
            </div>
            <el-empty v-else description="暂无消息" :image-size="80" />
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <div class="panel-header">
            <span>学习日历与实验日程</span>
          </div>
        </template>
        <div v-if="home.calendarEvents.length" class="timeline">
          <button v-for="event in home.calendarEvents" :key="`${event.type}-${event.id}`" type="button" @click="go(event.path)">
            <span class="timeline-dot" :class="event.type"></span>
            <span class="timeline-main">{{ event.title }}</span>
            <time>{{ formatTime(event.startTime) }}</time>
          </button>
        </div>
        <el-empty v-else description="暂无日程" :image-size="80" />
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <div class="panel-header">
            <span>快捷入口</span>
          </div>
        </template>
        <div class="shortcut-grid">
          <button v-for="item in home.shortcuts" :key="`${item.title}-${item.path}`" type="button" @click="go(item.path)">
            <el-icon><component :is="shortcutIcon(item.type)" /></el-icon>
            <span>{{ item.title }}</span>
          </button>
        </div>
        <div class="recent-block">
          <h2>最近访问</h2>
          <div v-if="home.recentVisits.length" class="recent-list">
            <button v-for="item in home.recentVisits" :key="item.id" type="button" @click="go(item.path)">
              <span>{{ item.title }}</span>
              <time>{{ formatTime(item.time) }}</time>
            </button>
          </div>
          <el-empty v-else description="暂无最近访问" :image-size="70" />
        </div>
      </el-card>
    </section>

    <el-dialog v-model="searchVisible" title="搜索结果" width="680px">
      <div v-if="searchResults.length" class="search-results">
        <button v-for="item in searchResults" :key="`${item.type}-${item.id}`" type="button" @click="openSearchResult(item)">
          <el-tag size="small" :type="tagType(item.type)">{{ typeLabel(item.type) }}</el-tag>
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.description || '暂无摘要' }}</p>
          </div>
        </button>
      </div>
      <el-empty v-else description="没有找到匹配内容" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  Document,
  EditPen,
  Folder,
  Link,
  Notebook,
  Operation,
  Reading,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/authStore'
import { getPortalHome, markMessageRead, searchPortal } from '@/api/portal'
import labHero from '@/assets/amazing/lab-hero.png'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const searching = ref(false)
const searchVisible = ref(false)
const messageTab = ref('notices')
const keyword = ref('')
const searchResults = ref([])

const home = reactive({
  role: '',
  metrics: [],
  todos: [],
  notices: [],
  messages: [],
  calendarEvents: [],
  recentVisits: [],
  shortcuts: [],
})

const roleTitle = computed(() => ({
  user: '油气工程公共学习空间',
  admin: '平台治理中心',
}[authStore.role] || '统一门户'))

const subtitle = computed(() => ({
  user: '浏览公共资源，加入课堂后完成实验教学与考核闭环。',
  admin: '维护平台用户、权限、公告和运行日志。',
}[authStore.role] || '汇总当前账号可访问的事项。'))

const unreadCount = computed(() => home.messages.filter((item) => Number(item.value) === 0).length)
const isLearner = computed(() => authStore.role === 'user')
const canManageClassroom = computed(() => authStore.hasPermission('course:create'))
const continueItem = computed(() => home.todos.find((item) => item.type === 'learning') || home.todos[0] || null)
const deadlineItems = computed(() => home.todos.filter((item) => ['DUE_SOON', 'OVERDUE'].includes(item.status)).slice(0, 5))
const admissionItems = computed(() => home.todos.filter((item) => ['admission', 'reservation', 'report'].includes(item.type)).slice(0, 6))
const shortcutIconMap = {
  course: Reading,
  experiment: Operation,
  exam: EditPen,
  knowledge: Notebook,
  resource: Folder,
  reservation: Calendar,
  report: Document,
}

onMounted(loadHome)

async function loadHome() {
  loading.value = true
  try {
    const data = await getPortalHome()
    Object.assign(home, {
      role: data?.role || '',
      metrics: data?.metrics || [],
      todos: data?.todos || [],
      notices: data?.notices || [],
      messages: data?.messages || [],
      calendarEvents: data?.calendarEvents || [],
      recentVisits: data?.recentVisits || [],
      shortcuts: data?.shortcuts || [],
    })
  } finally {
    loading.value = false
  }
}

async function doSearch() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  searching.value = true
  try {
    searchResults.value = await searchPortal({ keyword: keyword.value.trim(), limit: 12 })
    searchVisible.value = true
  } finally {
    searching.value = false
  }
}

async function readMessage(item) {
  if (Number(item.value) === 0) {
    await markMessageRead(item.id)
    item.value = 1
  }
  go(item.path)
}

function openSearchResult(item) {
  searchVisible.value = false
  go(item.path)
}

function go(path) {
  if (path) router.push(path)
}

function shortcutIcon(type) {
  return shortcutIconMap[type] || Link
}

function formatTime(value) {
  if (!value) return '未设置'
  return String(value).replace('T', ' ').slice(0, 16)
}

function tagType(type) {
  return {
    exam: 'warning',
    learning: 'primary',
    admission: 'warning',
    reservation: 'success',
    report: 'danger',
    course: 'primary',
    experiment: 'success',
    resource: 'info',
    knowledge: 'warning',
    log: 'info',
  }[type] || 'info'
}

function typeLabel(type) {
  return {
    course: '课程',
    experiment: '实验',
    learning: '学习',
    exam: '考试',
    reservation: '预约',
    report: '报告',
    admission: '准入',
    resource: '资源',
    knowledge: '安全知识',
    notice: '公告',
  }[type] || type || '结果'
}

function noticeTag(priority) {
  return priority === 'HIGH' ? 'danger' : priority === 'MEDIUM' ? 'warning' : 'info'
}

function noticeLabel(priority) {
  return priority === 'HIGH' ? '重要' : priority === 'MEDIUM' ? '普通' : '一般'
}
</script>

<style scoped>
.portal-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.amazing-hero {
  min-height: 260px;
  border-radius: 8px;
  background-size: cover;
  background-position: center;
  overflow: hidden;
  display: flex;
  align-items: stretch;
}
.hero-copy {
  width: min(620px, 100%);
  padding: 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.76), rgba(255, 255, 255, 0));
}
.hero-copy p {
  color: #177e89;
  font-weight: 800;
  margin-bottom: 8px;
}
.hero-copy h1 {
  color: #13233a;
  font-size: 34px;
  line-height: 1.15;
  margin-bottom: 10px;
}
.hero-copy span {
  max-width: 480px;
  color: #344054;
  line-height: 1.7;
}
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}
.portal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
.portal-toolbar h1 {
  font-size: 22px;
  color: #1f2937;
}
.portal-toolbar p {
  margin-top: 6px;
  color: #6b7280;
}
.search-box {
  width: min(520px, 48vw);
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.metric-card {
  min-height: 108px;
  padding: 16px;
  text-align: left;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
}
.metric-card:hover {
  border-color: #409eff;
}
.metric-label {
  display: block;
  color: #6b7280;
  font-size: 14px;
}
.metric-card strong {
  display: block;
  margin-top: 14px;
  font-size: 30px;
  color: #111827;
}
.metric-card small {
  margin-left: 4px;
  font-size: 14px;
  color: #6b7280;
}
.metric-card.warning strong { color: #b45309; }
.metric-card.danger strong { color: #b91c1c; }
.metric-card.success strong { color: #047857; }
.student-priority-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
  gap: 16px;
}
.student-priority-grid .todo-panel {
  grid-row: span 2;
}
.continue-card {
  width: 100%;
  min-height: 118px;
  text-align: left;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
}
.continue-card strong {
  display: block;
  color: #111827;
  font-size: 18px;
  line-height: 1.4;
  margin-bottom: 8px;
}
.continue-card span {
  color: #6b7280;
}
.student-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.student-metrics button {
  min-height: 86px;
  text-align: left;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
}
.student-metrics span {
  display: block;
  color: #6b7280;
  margin-bottom: 10px;
}
.student-metrics strong {
  color: #111827;
  font-size: 24px;
}
.student-metrics small {
  margin-left: 3px;
  color: #6b7280;
  font-size: 12px;
}
.advice-list {
  display: grid;
  gap: 10px;
}
.advice-list button {
  text-align: left;
  color: #344054;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
}
.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 16px;
}
.panel {
  border-radius: 8px;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.item-list {
  display: flex;
  flex-direction: column;
}
.list-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 0;
  background: transparent;
  border: 0;
  border-bottom: 1px solid #f1f5f9;
  text-align: left;
  cursor: pointer;
}
.list-item.static {
  cursor: default;
}
.list-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1f2937;
}
.list-item time,
.timeline time,
.recent-list time {
  color: #9ca3af;
  font-size: 12px;
}
.list-item.unread span {
  font-weight: 600;
}
.timeline,
.recent-list {
  display: flex;
  flex-direction: column;
}
.timeline button,
.recent-list button {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  background: transparent;
  border: 0;
  border-bottom: 1px solid #f1f5f9;
  text-align: left;
  cursor: pointer;
}
.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
}
.timeline-dot.reservation { background: #67c23a; }
.timeline-dot.exam { background: #e6a23c; }
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.shortcut-grid button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
}
.recent-block {
  margin-top: 18px;
}
.recent-block h2 {
  margin-bottom: 8px;
  font-size: 15px;
  color: #1f2937;
}
.search-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.search-results button {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  text-align: left;
  cursor: pointer;
}
.search-results p {
  margin-top: 4px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 1080px) {
  .metric-grid,
  .content-grid,
  .student-priority-grid {
    grid-template-columns: 1fr 1fr;
  }
  .portal-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .search-box {
    width: 100%;
  }
}
@media (max-width: 720px) {
  .amazing-hero {
    min-height: 320px;
    background-position: 62% center;
  }
  .hero-copy {
    padding: 24px;
    background: rgba(255, 255, 255, 0.88);
  }
  .hero-copy h1 {
    font-size: 26px;
  }
  .metric-grid,
  .content-grid,
  .student-priority-grid,
  .student-metrics {
    grid-template-columns: 1fr;
  }
  .search-box {
    grid-template-columns: 1fr;
  }
  .list-item,
  .timeline button,
  .recent-list button {
    grid-template-columns: auto minmax(0, 1fr);
  }
  .list-item time,
  .timeline time,
  .recent-list time {
    grid-column: 2;
  }
}
</style>
