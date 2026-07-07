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
        component: HomeView,
        meta: { title: '实验教学与考核驾驶舱', role: 'student', permission: 'portal:view' },
      },
      {
        path: 'teacher/home',
        name: 'TeacherHome',
        component: HomeView,
        meta: { title: '教师课程建设台', role: 'teacher', permission: 'portal:view' },
      },
      {
        path: 'admin/home',
        name: 'AdminHome',
        component: HomeView,
        meta: { title: '管理员首页', role: 'admin', permission: 'portal:view' },
      },
      {
        path: 'lab/home',
        name: 'LabAdminHome',
        component: HomeView,
        meta: { title: '实验室首页', role: 'lab_admin', permission: 'portal:view' },
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
        meta: { title: '课程讨论', permission: 'course:view' },
      },
      {
        path: 'student/courses',
        name: 'StudentCourseList',
        component: () => import('@/views/student/CourseList.vue'),
        meta: { title: '实验课程列表', role: 'student', permission: 'course:view' },
      },
      {
        path: 'student/learning/:courseId',
        name: 'StudentLearningCenter',
        component: () => import('@/views/student/LearningCenter.vue'),
        meta: { title: '实验教学与考核路径', role: 'student', permission: 'course:view' },
      },
      {
        path: 'student/resources',
        name: 'StudentResourceCenter',
        component: () => import('@/views/student/ResourceCenter.vue'),
        meta: { title: '油气资源库', role: 'student', permission: 'resource:view' },
      },
      {
        path: 'student/exams',
        name: 'StudentExamCenter',
        component: () => import('@/views/student/ExamCenter.vue'),
        meta: { title: '安全考试', role: 'student', permission: 'exam:take' },
      },
      {
        path: 'student/reserve',
        name: 'StudentReservationCenter',
        component: () => import('@/views/student/ReservationCenter.vue'),
        meta: { title: '实验预约', role: 'student', permission: 'reservation:view' },
      },
      {
        path: 'student/grades',
        name: 'StudentReportAndGrade',
        component: () => import('@/views/student/ReportAndGrade.vue'),
        meta: { title: '成绩反馈', role: 'student', permission: 'report:view' },
      },
      {
        path: 'teacher/dashboard',
        name: 'TeacherDashboard',
        component: () => import('@/views/teacher/TeacherDashboard.vue'),
        meta: { title: '教师工作台', role: 'teacher', permission: 'dashboard:view' },
      },
      {
        path: 'teacher/courses',
        name: 'TeacherCourseManagement',
        component: () => import('@/views/teacher/CourseManagement.vue'),
        meta: { title: '课程管理', role: 'teacher', permission: 'course:view' },
      },
      {
        path: 'teacher/courses/:courseId/edit',
        name: 'TeacherCourseEditor',
        component: () => import('@/views/teacher/CourseEditor.vue'),
        meta: { title: '课程建设', role: 'teacher', permission: 'course:update' },
      },
      {
        path: 'teacher/resources',
        name: 'TeacherResourceManagement',
        component: () => import('@/views/teacher/ResourceManagement.vue'),
        meta: { title: '资源管理', role: 'teacher', permission: 'resource:view' },
      },
      {
        path: 'teacher/experiments',
        name: 'TeacherExperimentManagement',
        component: () => import('@/views/teacher/ExperimentManagement.vue'),
        meta: { title: '实验路径', role: 'teacher', permission: 'experiment:view' },
      },
      {
        path: 'teacher/exam-papers',
        name: 'TeacherExamPaperManagement',
        component: () => import('@/views/teacher/ExamPaperManagement.vue'),
        meta: { title: '试卷管理', role: 'teacher', permission: 'exam-paper:view' },
      },
      {
        path: 'teacher/reservations',
        name: 'TeacherReservationReview',
        component: () => import('@/views/teacher/ReservationReview.vue'),
        meta: { title: '预约审核', roles: ['teacher', 'lab_admin'], permission: 'reservation:review' },
      },
      {
        path: 'teacher/reports',
        name: 'TeacherReportReview',
        component: () => import('@/views/teacher/ReportReview.vue'),
        meta: { title: '报告批改', role: 'teacher', permission: 'report:review' },
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
