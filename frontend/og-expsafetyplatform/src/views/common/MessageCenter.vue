<template>
  <div class="message-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Messages</p>
        <h1>消息与日程</h1>
        <p class="page-desc">查看课程通知、站内消息、考试和预约日程。</p>
      </div>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </section>

    <section class="message-layout">
      <div class="panel">
        <div class="section-title">
          <h2>站内消息</h2>
          <el-tag type="warning">{{ unreadCount }} 条未读</el-tag>
        </div>
        <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />
        <div v-else v-loading="loading" class="item-list">
          <article v-for="item in messages" :key="item.id" class="message-item" :class="{ unread: item.value === 0 }">
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <span>{{ item.time || '-' }}</span>
            </div>
            <div class="item-actions">
              <el-button v-if="item.path" text type="primary" @click="go(item)">前往</el-button>
              <el-button v-if="item.value === 0" text type="success" @click="markRead(item)">已读</el-button>
            </div>
          </article>
        </div>
      </div>

      <div class="side-stack">
        <div class="panel">
          <div class="section-title"><h2>公告</h2></div>
          <el-empty v-if="!loading && notices.length === 0" description="暂无公告" :image-size="80" />
          <article v-for="item in notices" :key="item.id" class="notice-item">
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
            <span>{{ item.time || '-' }}</span>
          </article>
        </div>

        <div class="panel">
          <div class="section-title"><h2>日程</h2></div>
          <el-empty v-if="!loading && calendar.length === 0" description="暂无日程" :image-size="80" />
          <article v-for="item in calendar" :key="`${item.type}-${item.id}`" class="notice-item">
            <h3>{{ item.title }}</h3>
            <p>{{ item.startTime }}<template v-if="item.endTime"> 至 {{ item.endTime }}</template></p>
            <el-button v-if="item.path" text type="primary" @click="router.push(item.path)">查看</el-button>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { getPortalCalendar, getPortalMessages, getPortalNotices, getUnreadMessageCount, markMessageRead } from '@/api/portal'

const router = useRouter()
const loading = ref(false)
const messages = ref([])
const notices = ref([])
const calendar = ref([])
const unreadCount = ref(0)

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [messageResult, noticeResult, calendarResult, unreadResult] = await Promise.all([
      getPortalMessages({ limit: 50 }),
      getPortalNotices({ limit: 20 }),
      getPortalCalendar({ limit: 20 }),
      getUnreadMessageCount(),
    ])
    messages.value = messageResult || []
    notices.value = noticeResult || []
    calendar.value = calendarResult || []
    unreadCount.value = unreadResult || 0
  } finally {
    loading.value = false
  }
}

async function markRead(item) {
  await markMessageRead(item.id)
  await loadAll()
}

async function go(item) {
  if (item.value === 0) await markMessageRead(item.id)
  router.push(item.path)
}
</script>

<style scoped>
.message-page { max-width: 1220px; margin: 0 auto; }
.page-head, .section-title { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.page-head { align-items: flex-end; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.message-layout { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr); gap: 16px; align-items: start; }
.side-stack { display: grid; gap: 16px; }
.panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.section-title { margin-bottom: 12px; }
.section-title h2 { color: #13233a; font-size: 18px; }
.item-list { display: grid; gap: 10px; min-height: 220px; }
.message-item, .notice-item { border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; background: #fff; }
.message-item { display: flex; justify-content: space-between; gap: 12px; }
.message-item.unread { border-color: #f5c56b; background: #fffaf0; }
.message-item h3, .notice-item h3 { color: #13233a; font-size: 15px; margin-bottom: 6px; }
.message-item p, .notice-item p { color: #667085; line-height: 1.6; margin: 0 0 6px; }
.message-item span, .notice-item span { color: #98a2b3; font-size: 12px; }
.item-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
@media (max-width: 900px) { .message-layout { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .page-head, .message-item { align-items: stretch; flex-direction: column; } }
</style>
