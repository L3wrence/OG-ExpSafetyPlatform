 <template>
   <div class="main-layout" :class="{ collapsed: appStore.sidebarCollapsed }">
     <!-- Sidebar -->
     <aside class="sidebar">
       <div class="sidebar-header">
         <div class="logo-wrapper">
           <el-icon :size="28" color="#409eff"><Platform /></el-icon>
           <span class="logo-text" v-show="!appStore.sidebarCollapsed">AmazingTeaching</span>
         </div>
       </div>
       <el-menu
         :default-active="currentRoute"
         :collapse="appStore.sidebarCollapsed"
         :router="true"
         class="sidebar-menu"
         background-color="transparent"
         text-color="rgba(255,255,255,0.65)"
         active-text-color="#fff"
       >
         <el-menu-item :index="homePath">
           <el-icon><HomeFilled /></el-icon>
           <template #title>首页</template>
         </el-menu-item>
 
         <!-- Role-specific menu items -->
         <template v-if="authStore.role === 'student'">
           <el-menu-item v-if="can('course:view')" index="/student/courses">
             <el-icon><Reading /></el-icon>
             <template #title>实验课程</template>
           </el-menu-item>
           <el-menu-item v-if="can('resource:view')" index="/student/resources">
             <el-icon><Folder /></el-icon>
             <template #title>油气资源库</template>
           </el-menu-item>
           <el-menu-item v-if="can('course:view')" index="/discussions">
             <el-icon><ChatLineRound /></el-icon>
             <template #title>学习交流</template>
           </el-menu-item>
          <el-menu-item v-if="can('exam:take')" index="/student/exams">
            <el-icon><EditPen /></el-icon>
            <template #title>安全考试</template>
          </el-menu-item>
          <el-menu-item v-if="can('reservation:view')" index="/student/reserve">
            <el-icon><Calendar /></el-icon>
            <template #title>实验预约</template>
          </el-menu-item>
           <el-menu-item v-if="can('report:view')" index="/student/grades">
             <el-icon><Trophy /></el-icon>
             <template #title>报告与成绩</template>
           </el-menu-item>
           <el-menu-item v-if="can('portal:message')" index="/messages">
             <el-icon><Bell /></el-icon>
             <template #title>消息与日程</template>
           </el-menu-item>
           <el-menu-item index="/profile">
             <el-icon><UserFilled /></el-icon>
             <template #title>个人中心</template>
           </el-menu-item>
         </template>
 
         <template v-if="authStore.role === 'teacher'">
           <el-menu-item index="/teacher/dashboard">
             <el-icon><DataBoard /></el-icon>
             <template #title>教师工作台</template>
           </el-menu-item>
          <el-menu-item v-if="can('course:view')" index="/teacher/courses">
            <el-icon><Reading /></el-icon>
            <template #title>课程建设</template>
           </el-menu-item>
           <el-menu-item v-if="can('experiment:view')" index="/teacher/experiments">
             <el-icon><Operation /></el-icon>
             <template #title>实验路径</template>
           </el-menu-item>
           <el-menu-item v-if="can('resource:view')" index="/teacher/resources">
             <el-icon><Folder /></el-icon>
             <template #title>资源管理</template>
           </el-menu-item>
           <el-menu-item v-if="can('exam-paper:view')" index="/teacher/exam-papers">
             <el-icon><EditPen /></el-icon>
             <template #title>题库与考试</template>
           </el-menu-item>
           <el-menu-item v-if="can('reservation:review')" index="/teacher/reservations">
             <el-icon><Calendar /></el-icon>
             <template #title>预约管理</template>
           </el-menu-item>
           <el-menu-item v-if="can('report:review')" index="/teacher/reports">
             <el-icon><Document /></el-icon>
             <template #title>报告批改</template>
           </el-menu-item>
           <el-menu-item index="/profile">
             <el-icon><UserFilled /></el-icon>
             <template #title>个人中心</template>
           </el-menu-item>
         </template>

         <template v-if="authStore.role === 'lab_admin'">
           <el-menu-item index="/lab/home">
             <el-icon><Monitor /></el-icon>
             <template #title>实验室运行</template>
           </el-menu-item>
           <el-menu-item v-if="can('reservation:review')" index="/teacher/reservations">
             <el-icon><Calendar /></el-icon>
             <template #title>预约审核</template>
           </el-menu-item>
           <el-menu-item index="/profile">
             <el-icon><UserFilled /></el-icon>
             <template #title>个人中心</template>
           </el-menu-item>
         </template>
 
         <template v-if="authStore.role === 'admin'">
           <el-menu-item v-if="can('user:view')" index="/admin/users">
             <el-icon><User /></el-icon>
             <template #title>用户管理</template>
           </el-menu-item>
           <el-menu-item v-if="can('role:view')" index="/admin/roles">
             <el-icon><Avatar /></el-icon>
             <template #title>角色管理</template>
           </el-menu-item>
           <el-menu-item v-if="can('permission:view')" index="/admin/permissions">
             <el-icon><Lock /></el-icon>
             <template #title>权限管理</template>
           </el-menu-item>
           <el-menu-item index="/admin/notices">
             <el-icon><Bell /></el-icon>
             <template #title>公告管理</template>
           </el-menu-item>
           <el-menu-item index="/admin/logs">
             <el-icon><List /></el-icon>
             <template #title>操作日志</template>
           </el-menu-item>
           <el-menu-item index="/profile">
             <el-icon><UserFilled /></el-icon>
             <template #title>个人中心</template>
           </el-menu-item>
         </template>
       </el-menu>
     </aside>
 
     <!-- Main Content Area -->
     <div class="main-area">
       <!-- Top Bar -->
       <header class="top-bar">
         <div class="top-bar-left">
           <el-button
             text
             :aria-label="appStore.sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
             :title="appStore.sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
             @click="appStore.toggleSidebar"
             class="collapse-btn"
           >
             <el-icon :size="20">
               <Fold v-if="!appStore.sidebarCollapsed" />
               <Expand v-else />
             </el-icon>
           </el-button>
           <el-breadcrumb separator="/">
             <el-breadcrumb-item :to="{ path: homePath }">首页</el-breadcrumb-item>
             <el-breadcrumb-item v-if="pageTitle">{{ pageTitle }}</el-breadcrumb-item>
           </el-breadcrumb>
         </div>
         <div class="top-bar-right">
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
               <span class="role-name">{{ roleLabel }}</span>
               <el-icon><ArrowDown /></el-icon>
             </span>
             <template #dropdown>
               <el-dropdown-menu>
                 <el-dropdown-item @click="handleLogout">
                 <el-icon><SwitchButton /></el-icon>退出登录
                 </el-dropdown-item>
               </el-dropdown-menu>
             </template>
           </el-dropdown>
         </div>
       </header>
 
       <!-- Page Content -->
       <main class="content-area">
         <router-view />
       </main>
     </div>
   </div>
 </template>
 
 <script setup>
 import { computed, onMounted, ref, watch } from 'vue'
 import { useRoute } from 'vue-router'
 import {
   ArrowDown,
   Avatar,
   Bell,
   Calendar,
   ChatLineRound,
   DataBoard,
   Document,
   EditPen,
   Expand,
   Fold,
   Folder,
   HomeFilled,
   List,
   Lock,
   Monitor,
   Operation,
   Platform,
   Reading,
   SwitchButton,
   Trophy,
   User,
   UserFilled,
 } from '@element-plus/icons-vue'
 import { useAppStore } from '@/stores/appStore'
 import { useAuthStore } from '@/stores/authStore'
 import { ROLE_LABELS } from '@/utils/constant'
 import { getRoleHomePath } from '@/utils/role'
 import router from '@/router'
 import { getUnreadMessageCount, recordRecentVisit } from '@/api/portal'
 
 const route = useRoute()
 const appStore = useAppStore()
 const authStore = useAuthStore()
 
 const currentRoute = computed(() => route.path)
 const pageTitle = computed(() => route.meta?.title || '')
 const homePath = computed(() => getRoleHomePath(authStore.role))
const displayName = computed(() => authStore.userInfo?.realName || authStore.userInfo?.name || authStore.userInfo?.username || '用户')
const roleLabel = computed(() => ROLE_LABELS[authStore.role] || '未绑定角色')
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
       title: route.meta?.title || '功能页面',
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
   display: flex;
   height: 100vh;
   background: #f0f2f5;
 }
 
 /* Sidebar */
 .sidebar {
   width: 220px;
   background: linear-gradient(180deg, #12312f 0%, #24483e 52%, #5b4b35 100%);
   display: flex;
   flex-direction: column;
   transition: width 0.3s;
   overflow: hidden;
   flex-shrink: 0;
   z-index: 100;
 }
 .collapsed .sidebar {
   width: 64px;
 }
 .sidebar-header {
   height: 60px;
   display: flex;
   align-items: center;
   justify-content: center;
   border-bottom: 1px solid rgba(255,255,255,0.06);
   padding: 0 12px;
 }
 .logo-wrapper {
   display: flex;
   align-items: center;
   gap: 10px;
   overflow: hidden;
 }
 .logo-text {
   font-size: 17px;
   font-weight: 600;
   color: #fff;
   white-space: nowrap;
 }
 .sidebar-menu {
   flex: 1;
   border-right: none;
   padding: 8px 0;
 }
 .sidebar-menu:not(.el-menu--collapse) {
   width: 220px;
 }
 
 /* Main Area */
 .main-area {
   flex: 1;
   display: flex;
   flex-direction: column;
   overflow: hidden;
   min-width: 0;
 }
 
 /* Top Bar */
 .top-bar {
   height: 56px;
   background: #fff;
   display: flex;
   align-items: center;
   justify-content: space-between;
   padding: 0 20px;
   box-shadow: 0 1px 4px rgba(0,0,0,0.06);
   flex-shrink: 0;
   z-index: 10;
 }
 .top-bar-left {
   display: flex;
   align-items: center;
   gap: 12px;
 }
 .collapse-btn {
   padding: 4px;
 }
 .top-bar-right {
   display: flex;
   align-items: center;
   gap: 8px;
 }
 .top-icon-btn {
   padding: 6px;
   border-radius: 8px;
 }
 .top-icon-btn:hover {
   background: #f0f2f5;
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
 .user-info-dropdown:hover {
   background: #f0f2f5;
 }
 .username {
   font-size: 14px;
   color: #333;
 }
 .role-name {
   font-size: 12px;
   color: #909399;
 }
 
 /* Content */
 .content-area {
   flex: 1;
   overflow-y: auto;
   padding: 20px;
   background: #f0f2f5;
 }
 
 /* Responsive */
 @media (max-width: 768px) {
   .sidebar {
     position: fixed;
     left: 0;
     top: 0;
     height: 100%;
     z-index: 1000;
     transform: translateX(-100%);
   }
   .collapsed .sidebar {
     transform: translateX(0);
     width: 220px;
   }
 }
 </style>
