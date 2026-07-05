 import { createRouter, createWebHistory } from 'vue-router'
 import { useAuthStore } from '@/stores/authStore'
 import { getRoleHomePath } from '@/utils/role'
 
 const HomeView = () => import('@/views/common/Home.vue')
 const PlaceholderView = () => import('@/views/common/PlaceholderPage.vue')
 
 const routes = [
   {
     path: '/login',
     component: () => import('@/layouts/CommonLayout.vue'),
     children: [
       {
         path: '',
         name: 'Login',
         component: () => import('@/views/common/Login.vue'),
       },
     ],
   },
   {
     path: '/',
     component: () => import('@/layouts/MainLayout.vue'),
     redirect: '/home',
     children: [
      {
        path: 'home',
        name: 'Home',
        redirect: () => getRoleHomePath(localStorage.getItem('role')),
      },
      {
        path: 'student/home',
        name: 'StudentHome',
        component: HomeView,
        meta: { title: '学生首页', role: 'student' },
      },
      {
        path: 'teacher/home',
        name: 'TeacherHome',
        component: HomeView,
        meta: { title: '教师首页', role: 'teacher' },
      },
      {
        path: 'admin/home',
        name: 'AdminHome',
        component: HomeView,
        meta: { title: '管理员首页', role: 'admin' },
      },
      {
        path: 'student/courses',
        name: 'StudentCourseList',
        component: () => import('@/views/student/CourseList.vue'),
        meta: { title: '实验课程列表', role: 'student' },
      },
      {
        path: 'student/knowledge',
        name: 'StudentSafetyKnowledge',
        component: () => import('@/views/student/SafetyKnowledge.vue'),
        meta: { title: '安全知识学习', role: 'student' },
      },
      {
        path: 'student/exams',
        name: 'StudentExamCenter',
        component: () => import('@/views/student/ExamCenter.vue'),
        meta: { title: '安全考试', role: 'student' },
      },
      {
        path: 'student/reserve',
        name: 'StudentReservationCenter',
        component: () => import('@/views/student/ReservationCenter.vue'),
        meta: { title: '实验预约', role: 'student' },
      },
      {
        path: 'student/grades',
        name: 'StudentReportAndGrade',
        component: () => import('@/views/student/ReportAndGrade.vue'),
        meta: { title: '成绩反馈', role: 'student' },
      },
      {
        path: 'student/ai-assistant',
        name: 'StudentAiAssistant',
        component: () => import('@/views/student/AiAssistant.vue'),
        meta: { title: 'AI实验助手', role: 'student' },
      },
      {
        path: 'student/:module(profile)',
        name: 'StudentPlaceholder',
        component: PlaceholderView,
        meta: { title: '学生功能', role: 'student' },
      },
      {
        path: 'teacher/dashboard',
        name: 'TeacherDashboard',
        component: HomeView,
        meta: { title: '教师工作台', role: 'teacher' },
      },
      {
        path: 'teacher/courses',
        name: 'TeacherCourseManagement',
        component: () => import('@/views/teacher/CourseManagement.vue'),
        meta: { title: '课程管理', role: 'teacher' },
      },
      {
        path: 'teacher/resources',
        name: 'TeacherResourceManagement',
        component: () => import('@/views/teacher/ResourceManagement.vue'),
        meta: { title: '资源管理', role: 'teacher' },
      },
      {
        path: 'teacher/knowledge',
        name: 'TeacherSafetyKnowledgeManagement',
        component: () => import('@/views/teacher/SafetyKnowledgeManagement.vue'),
        meta: { title: '安全知识管理', role: 'teacher' },
      },
      {
        path: 'teacher/exam-papers',
        name: 'TeacherExamPaperManagement',
        component: () => import('@/views/teacher/ExamPaperManagement.vue'),
        meta: { title: '试卷管理', role: 'teacher' },
      },
      {
        path: 'teacher/reservations',
        name: 'TeacherReservationReview',
        component: () => import('@/views/teacher/ReservationReview.vue'),
        meta: { title: '预约审核', role: 'teacher' },
      },
      {
        path: 'teacher/reports',
        name: 'TeacherReportReview',
        component: () => import('@/views/teacher/ReportReview.vue'),
        meta: { title: '报告批改', role: 'teacher' },
      },
      {
        path: 'admin/users',
        name: 'AdminUserManagement',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { title: '用户管理', role: 'admin' },
      },
      {
        path: 'admin/roles',
        name: 'AdminRoleManagement',
        component: () => import('@/views/admin/RoleManagement.vue'),
        meta: { title: '角色管理', role: 'admin' },
      },
      {
        path: 'admin/permissions',
        name: 'AdminPermissionManagement',
        component: () => import('@/views/admin/PermissionManagement.vue'),
        meta: { title: '权限管理', role: 'admin' },
      },
      {
        path: 'admin/:module(notices|logs)',
        name: 'AdminPlaceholder',
        component: PlaceholderView,
        meta: { title: '管理员功能', role: 'admin' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home',
  },
 ]
 
 const router = createRouter({
   history: createWebHistory(),
   routes,
 })
 
 router.beforeEach((to, _from, next) => {
   const authStore = useAuthStore()
   const roleHomePath = getRoleHomePath(authStore.role)
 
   if (to.path !== '/login' && !authStore.token) {
     next({ path: '/login', query: { redirect: to.fullPath } })
     return
   }
 
   if (authStore.token && roleHomePath === '/login') {
     authStore.logout()
     next('/login')
     return
   }
 
   if (to.path === '/login' && authStore.token) {
     next(roleHomePath)
     return
   }
 
   if ((to.path === '/' || to.path === '/home') && authStore.token) {
     next(roleHomePath)
     return
   }
 
   if (to.meta.role && to.meta.role !== authStore.role) {
     next(roleHomePath)
     return
   }
 
   next()
 })
 
 export default router
