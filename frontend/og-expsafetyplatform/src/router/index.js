 import { createRouter, createWebHistory } from 'vue-router'
 import { useAuthStore } from '@/stores/authStore'
 import { getRoleHomePath } from '@/utils/role'
 
 const HomeView = () => import('@/views/common/Home.vue')
 
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
        redirect: '/user/home',
      },
      {
        path: 'user/home',
        name: 'UserHome',
        component: HomeView,
        meta: { title: '公共资源学习首页', role: 'user', permission: 'portal:view' },
      },
      {
        path: 'teacher/home',
        name: 'TeacherHome',
        redirect: '/user/home',
      },
      {
        path: 'admin/home',
        name: 'AdminHome',
        component: HomeView,
        meta: { title: '管理员首页', role: 'admin', permission: 'portal:view' },
      },
      {
        path: 'profile',
        name: 'ProfileCenter',
        component: () => import('@/views/common/ProfileCenter.vue'),
        meta: { title: '个人中心', permission: 'profile:update' },
      },
      {
        path: 'messages',
        name: 'MessageCenter',
        component: () => import('@/views/common/MessageCenter.vue'),
        meta: { title: '消息与日程', permission: 'portal:message' },
      },
      {
        path: 'discussions',
        name: 'DiscussionCenter',
        component: () => import('@/views/common/DiscussionCenter.vue'),
        meta: { title: '学习交流', permission: 'course:view' },
      },
      {
        path: 'resources',
        name: 'UnifiedResourceCenter',
        component: () => import('@/views/student/ResourceCenter.vue'),
        meta: { title: '资源学习', permission: 'resource:view' },
      },
      {
        path: 'classrooms',
        name: 'UnifiedClassroomList',
        component: () => import('@/views/student/CourseList.vue'),
        meta: { title: '我的课堂', permission: 'course:view' },
      },
      {
        path: 'classrooms/:courseId/learn',
        name: 'UnifiedClassroomLearning',
        component: () => import('@/views/student/LearningCenter.vue'),
        meta: { title: '课堂学习路径', permission: 'course:view' },
      },
      {
        path: 'classrooms/:courseId/chapters/:experimentId',
        name: 'UnifiedClassroomChapterLearning',
        component: () => import('@/views/student/ChapterLearning.vue'),
        meta: { title: '章节详情', permission: 'course:view' },
      },
      {
        path: 'student/courses',
        name: 'StudentCourseList',
        redirect: '/classrooms',
      },
      {
        path: 'student/learning/:courseId',
        name: 'StudentLearningCenter',
        redirect: (to) => `/classrooms/${to.params.courseId}/learn`,
      },
      {
        path: 'student/resources',
        name: 'StudentResourceCenter',
        redirect: '/resources',
      },
      {
        path: 'student/exams',
        name: 'StudentExamCenter',
        redirect: '/classrooms?module=exam',
      },
      {
        path: 'student/reserve',
        name: 'StudentReservationCenter',
        redirect: '/classrooms?module=reservation',
      },
      {
        path: 'student/grades',
        name: 'StudentReportAndGrade',
        redirect: '/classrooms',
      },
      {
        path: 'teacher/dashboard',
        name: 'TeacherDashboard',
        redirect: '/classrooms',
      },
      {
        path: 'teacher/courses',
        name: 'TeacherCourseManagement',
        component: () => import('@/views/teacher/CourseManagement.vue'),
        meta: { title: '课堂管理', role: 'user', permission: 'course:create' },
      },
      {
        path: 'teacher/courses/:courseId/edit',
        name: 'TeacherCourseEditor',
        component: () => import('@/views/teacher/CourseEditor.vue'),
        meta: { title: '课堂详细', role: 'user', permission: 'course:update' },
      },
      {
        path: 'teacher/resources',
        name: 'TeacherResourceManagement',
        redirect: '/classrooms',
      },
      {
        path: 'teacher/experiments',
        name: 'TeacherExperimentManagement',
        redirect: '/classrooms',
      },
      {
        path: 'teacher/exam-papers',
        name: 'TeacherExamPaperManagement',
        redirect: '/classrooms',
      },
      {
        path: 'teacher/reservations',
        name: 'TeacherReservationReview',
        redirect: '/classrooms',
      },
      {
        path: 'teacher/reports',
        name: 'TeacherReportReview',
        redirect: '/classrooms',
      },
      {
        path: 'admin/users',
        name: 'AdminUserManagement',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { title: '用户管理', role: 'admin', permission: 'user:view' },
      },
      {
        path: 'admin/roles',
        name: 'AdminRoleManagement',
        component: () => import('@/views/admin/RoleManagement.vue'),
        meta: { title: '角色管理', role: 'admin', permission: 'role:view' },
      },
      {
        path: 'admin/permissions',
        name: 'AdminPermissionManagement',
        component: () => import('@/views/admin/PermissionManagement.vue'),
        meta: { title: '权限管理', role: 'admin', permission: 'permission:view' },
      },
      {
        path: 'admin/notices',
        name: 'AdminNoticeManagement',
        component: () => import('@/views/admin/NoticeManagement.vue'),
        meta: { title: '公告管理', role: 'admin', permission: 'portal:notice:manage' },
      },
      {
        path: 'admin/teacher-certifications',
        name: 'AdminTeacherCertificationReview',
        component: () => import('@/views/admin/TeacherCertificationReview.vue'),
        meta: { title: '教师认证审核', role: 'admin', permission: 'teacher-certification:review' },
      },
      {
        path: 'admin/resource-submissions',
        name: 'AdminResourceSubmissionReview',
        component: () => import('@/views/admin/ResourceSubmissionReview.vue'),
        meta: { title: '资源投稿审核', role: 'admin', permission: 'resource-submission:review' },
      },
      {
        path: 'admin/logs',
        name: 'AdminOperationLog',
        component: () => import('@/views/admin/OperationLog.vue'),
        meta: { title: '操作日志', role: 'admin', permission: 'operation-log:view' },
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
 
  const allowedRoles = to.meta.roles || (to.meta.role ? [to.meta.role] : null)
  if (allowedRoles && !allowedRoles.includes(authStore.role)) {
    next(roleHomePath)
    return
  }

  const requiredPermission = to.meta.permission
  if (requiredPermission && !authStore.hasPermission(requiredPermission)) {
    if (to.path !== roleHomePath) {
      next(roleHomePath)
    } else {
      next(false)
    }
    return
  }
 
  next()
})
 
 export default router
