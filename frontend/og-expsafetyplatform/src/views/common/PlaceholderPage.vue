<template>
  <section class="placeholder-page">
    <el-empty :description="`${pageTitle}正在完善`">
      <el-button type="primary" @click="router.push(homePath)">返回首页</el-button>
    </el-empty>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { getRoleHomePath } from '@/utils/role'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const moduleLabels = {
  reserve: '实验预约',
  grades: '成绩反馈',
  'ai-assistant': 'AI实验助手',
  profile: '个人中心',
  dashboard: '工作台',
  courses: '课程管理',
  resources: '资源管理',
  knowledge: '安全知识管理',
  'exam-papers': '试卷管理',
  reservations: '预约审核',
  reports: '报告批改',
  users: '用户管理',
  roles: '角色管理',
  permissions: '权限管理',
  notices: '公告管理',
  logs: '操作日志',
}

const pageTitle = computed(() => moduleLabels[route.params.module] || route.meta.title || '页面')
const homePath = computed(() => getRoleHomePath(authStore.role))
</script>

<style scoped>
.placeholder-page {
  min-height: calc(100vh - 96px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8px;
}
</style>
