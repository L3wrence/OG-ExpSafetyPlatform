<template>
  <div class="main-layout">
    <header class="app-header">
      <div class="brand-area" @click="router.push(homePath)">
        <div class="brand-mark">
          <el-icon :size="24"><Platform /></el-icon>
        </div>
        <div class="brand-copy">
          <strong>油气工程实验教学与考核平台</strong>
          <span>{{ roleLabel }}</span>
        </div>
      </div>

      <el-menu
        :default-active="currentRoute"
        :router="true"
        mode="horizontal"
        class="top-nav"
        :ellipsis="false"
      >
        <el-menu-item :index="homePath">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <template v-if="isUnifiedLearner">
          <el-menu-item v-if="can('resource:view')" index="/resources">
            <el-icon><Folder /></el-icon>
            <span>资源学习</span>
          </el-menu-item>
          <el-menu-item v-if="can('course:view')" index="/classrooms">
            <el-icon><Reading /></el-icon>
            <span>我的课堂</span>
          </el-menu-item>
          <el-menu-item v-if="can('course:view')" index="/discussions">
            <el-icon><ChatLineRound /></el-icon>
            <span>学习交流</span>
          </el-menu-item>
          <el-menu-item v-if="can('portal:message')" index="/messages">
            <el-icon><Bell /></el-icon>
            <span>消息</span>
          </el-menu-item>
        </template>

        <template v-if="authStore.role === 'admin'">
          <el-menu-item v-if="can('user:view')" index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item v-if="can('role:view')" index="/admin/roles">
            <el-icon><Avatar /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item v-if="can('permission:view')" index="/admin/permissions">
            <el-icon><Lock /></el-icon>
            <span>权限管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/notices">
            <el-icon><Bell /></el-icon>
            <span>公告管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/logs">
            <el-icon><List /></el-icon>
            <span>操作日志</span>
          </el-menu-item>
          <el-menu-item v-if="can('teacher-certification:review')" index="/admin/teacher-certifications">
            <el-icon><Document /></el-icon>
            <span>教师认证</span>
          </el-menu-item>
          <el-menu-item v-if="can('resource-submission:review')" index="/admin/resource-submissions">
            <el-icon><Folder /></el-icon>
            <span>资源投稿</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="header-actions">
        <el-tooltip content="通知" placement="bottom">
          <el-badge :value="unreadMessages" :hidden="unreadMessages === 0" :max="99">
            <el-button text class="top-icon-btn" aria-label="通知" title="通知" @click="router.push('/messages')">
              <el-icon :size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>
        </el-tooltip>
        <el-dropdown trigger="click">
          <span class="user-info-dropdown">
            <el-avatar :size="32" :src="authStore.userInfo?.avatarUrl" :icon="UserFilled" />
            <span class="username">{{ displayName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/profile')">
                <el-icon><UserFilled /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="content-area">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowDown,
  Avatar,
  Bell,
  ChatLineRound,
  Document,
  Folder,
  HomeFilled,
  List,
  Lock,
  Platform,
  Reading,
  SwitchButton,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/authStore'
import { ROLE_LABELS } from '@/utils/constant'
import { getRoleHomePath } from '@/utils/role'
import router from '@/router'
import { getUnreadMessageCount, recordRecentVisit } from '@/api/portal'

const route = useRoute()
const authStore = useAuthStore()

const currentRoute = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '')
const homePath = computed(() => getRoleHomePath(authStore.role))
const displayName = computed(() => authStore.userInfo?.realName || authStore.userInfo?.name || authStore.userInfo?.username || '用户')
const roleLabel = computed(() => ROLE_LABELS[authStore.role] || '普通用户')
const isUnifiedLearner = computed(() => authStore.role === 'user')
const unreadMessages = ref(0)

function can(permission) {
  return authStore.hasPermission(permission)
}

async function handleLogout() {
  await authStore.logoutRemote()
  router.push('/login')
}

async function loadUnreadMessages() {
  if (!authStore.token || !can('portal:message')) return
  unreadMessages.value = await getUnreadMessageCount({ silent: true }).catch(() => 0)
}

onMounted(loadUnreadMessages)

watch(
  () => route.fullPath,
  (path) => {
    if (!authStore.token || path === '/login') return
    recordRecentVisit({
      title: pageTitle.value || '功能页面',
      path,
      module: String(route.name || ''),
    }).catch(() => {})
    loadUnreadMessages()
  },
  { immediate: true },
)
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: #f4f7f8;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  min-height: 64px;
  display: grid;
  grid-template-columns: minmax(260px, 340px) minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 0 22px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e5ecef;
  box-shadow: 0 4px 18px rgba(15, 35, 45, 0.06);
  backdrop-filter: blur(10px);
}

.brand-area {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  cursor: pointer;
}

.brand-mark {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #fff;
  background: #177e89;
  border-radius: 8px;
}

.brand-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.brand-copy strong {
  color: #13233a;
  font-size: 16px;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-copy span {
  color: #667085;
  font-size: 12px;
}

.top-nav {
  min-width: 0;
  height: 64px;
  border-bottom: none;
  overflow-x: auto;
  overflow-y: hidden;
  background: transparent;
}

.top-nav::-webkit-scrollbar {
  height: 0;
}

.top-nav :deep(.el-menu-item) {
  height: 64px !important;
  line-height: 64px !important;
  margin: 0 2px !important;
  padding: 0 12px !important;
  border-radius: 0 !important;
  color: #344054;
}

.top-nav :deep(.el-menu-item:hover) {
  color: #177e89;
  background: #eefafa !important;
}

.top-nav :deep(.el-menu-item.is-active) {
  color: #177e89;
  background: transparent !important;
  border-bottom: 3px solid #177e89;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.top-icon-btn {
  padding: 6px;
  border-radius: 8px;
}

.top-icon-btn:hover,
.user-info-dropdown:hover {
  background: #f0f5f6;
}

.user-info-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.username {
  max-width: 92px;
  color: #333;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.content-area {
  min-height: calc(100vh - 64px);
  padding: 20px;
}

@media (max-width: 1180px) {
  .app-header {
    grid-template-columns: minmax(220px, 300px) minmax(0, 1fr) auto;
    padding: 0 14px;
  }

  .top-nav :deep(.el-menu-item) {
    padding: 0 10px !important;
  }
}

@media (max-width: 860px) {
  .app-header {
    grid-template-columns: 1fr auto;
    grid-template-rows: 58px 50px;
    gap: 0 10px;
    min-height: 108px;
  }

  .top-nav {
    grid-column: 1 / -1;
    grid-row: 2;
    height: 50px;
  }

  .top-nav :deep(.el-menu-item) {
    height: 50px !important;
    line-height: 50px !important;
  }

  .brand-copy strong {
    font-size: 15px;
  }

  .content-area {
    min-height: calc(100vh - 108px);
    padding: 14px;
  }
}

@media (max-width: 560px) {
  .brand-copy strong {
    white-space: normal;
  }

  .brand-copy span,
  .username {
    display: none;
  }
}
</style>
