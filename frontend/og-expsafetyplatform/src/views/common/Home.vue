 <template>
   <div class="home-page">
     <!-- Student Dashboard -->
     <template v-if="authStore.role === 'student'">
       <div class="welcome-banner">
         <div class="welcome-content">
           <h2 class="welcome-title">欢迎回来，{{ authStore.userInfo?.name || '同学' }}！</h2>
           <p class="welcome-desc">继续你的油气工程学习之旅，完成实验任务，掌握安全知识。</p>
         </div>
         <div class="welcome-progress">
           <div class="progress-label">
             <span>本学期进度</span>
             <span class="progress-value">{{ studentProgress.percent }}%</span>
           </div>
           <el-progress :percentage="studentProgress.percent" :stroke-width="8" striped striped-flow />
           <div class="progress-detail">
             <span>已修：{{ studentProgress.courseDone }}/{{ studentProgress.courseTotal }} 门课程</span>
             <span>已完成：{{ studentProgress.experimentDone }}/{{ studentProgress.experimentTotal }} 个实验</span>
           </div>
         </div>
       </div>
 
       <!-- Todo Cards -->
       <div class="stat-cards">
         <div class="stat-card todo-exam" @click="router.push('/student/exams')">
           <div class="stat-icon"><el-icon :size="24"><EditPen /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ studentStats.examCount }}</span>
             <span class="stat-label">待参加考试</span>
           </div>
         </div>
         <div class="stat-card todo-reserve" @click="router.push('/student/reserve')">
           <div class="stat-icon"><el-icon :size="24"><Calendar /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ studentStats.reserveCount }}</span>
             <span class="stat-label">待预约实验</span>
           </div>
         </div>
         <div class="stat-card todo-report" @click="router.push('/student/grades')">
           <div class="stat-icon"><el-icon :size="24"><Document /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ studentStats.reportCount }}</span>
             <span class="stat-label">待提交报告</span>
           </div>
         </div>
         <div class="stat-card todo-grade" @click="router.push('/student/grades')">
           <div class="stat-icon"><el-icon :size="24"><Trophy /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ studentStats.avgScore }}%</span>
             <span class="stat-label">考试通过率</span>
           </div>
         </div>
       </div>
 
       <div class="home-grid-2col">
         <!-- Announcements -->
         <div class="home-card announcements-card">
           <div class="card-header">
             <h3><el-icon><Bell /></el-icon> 系统公告</h3>
             <el-button text type="primary" size="small">更多</el-button>
           </div>
           <div class="card-body">
             <div v-for="item in announcements" :key="item.id" class="announcement-item">
               <div class="announcement-tag">
                 <el-tag :type="item.priority === 'high' ? 'danger' : item.priority === 'medium' ? 'warning' : 'info'" size="small">
                   {{ item.priority === 'high' ? '重要' : item.priority === 'medium' ? '普通' : '一般' }}
                 </el-tag>
               </div>
               <div class="announcement-content">
                 <span class="announcement-title">{{ item.title }}</span>
                 <span class="announcement-time">{{ item.time }}</span>
               </div>
             </div>
             <div v-if="announcements.length === 0" class="empty-hint">暂无公告</div>
           </div>
         </div>
 
         <!-- Learning Stats Pie -->
         <div class="home-card chart-card">
           <div class="card-header">
             <h3><el-icon><DataAnalysis /></el-icon> 学习统计</h3>
           </div>
           <div class="card-body chart-body">
             <div ref="studentChartRef" class="chart-container"></div>
           </div>
         </div>
       </div>
 
       <!-- Recommended Resources -->
       <div class="home-card">
         <div class="card-header">
           <h3><el-icon><StarFilled /></el-icon> 推荐学习</h3>
           <el-button text type="primary" size="small">更多推荐</el-button>
         </div>
         <div class="card-body">
           <div v-if="recommendations.length === 0" class="empty-hint">暂无推荐内容</div>
           <div v-else class="recommend-grid">
             <div v-for="item in recommendations" :key="item.id" class="recommend-card" @click="handleRecommendClick(item)">
               <div class="recommend-icon" :class="item.type">
                 <el-icon :size="20">
                   <component :is="item.type === 'resource' ? 'Folder' : item.type === 'experiment' ? 'Monitor' : 'Notebook'" />
                 </el-icon>
               </div>
               <div class="recommend-info">
                 <span class="recommend-title">{{ item.title }}</span>
                 <el-tag size="small" type="warning">热度 {{ item.score }}</el-tag>
               </div>
               <div class="recommend-reason">{{ item.reason }}</div>
             </div>
           </div>
         </div>
       </div>
     </template>
 
     <!-- Teacher Dashboard -->
     <template v-if="authStore.role === 'teacher'">
       <div class="welcome-banner teacher-banner">
         <div class="welcome-content">
           <h2 class="welcome-title">欢迎回来，{{ authStore.userInfo?.name || '老师' }}！</h2>
           <p class="welcome-desc">今日有 <strong>{{ teacherStats.todoTotal }}</strong> 项待办事项等待处理。</p>
         </div>
         <div class="quick-actions">
           <el-button type="primary" :icon="Plus" @click="router.push('/teacher/courses')">创建课程</el-button>
           <el-button type="success" :icon="EditPen" @click="router.push('/teacher/exam-papers')">发布考试</el-button>
           <el-button type="warning" :icon="Calendar" @click="router.push('/teacher/reservations')">审核预约</el-button>
         </div>
       </div>
 
       <!-- Todo Statistics -->
       <div class="stat-cards">
         <div class="stat-card todo-reserve" @click="router.push('/teacher/reservations')">
           <div class="stat-icon"><el-icon :size="24"><Calendar /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ teacherStats.pendingReservations }}</span>
             <span class="stat-label">待审核预约</span>
           </div>
         </div>
         <div class="stat-card todo-report" @click="router.push('/teacher/reports')">
           <div class="stat-icon"><el-icon :size="24"><Document /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ teacherStats.pendingReports }}</span>
             <span class="stat-label">待批改报告</span>
           </div>
         </div>
         <div class="stat-card todo-exam" @click="router.push('/teacher/exam-papers')">
           <div class="stat-icon"><el-icon :size="24"><EditPen /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ teacherStats.pendingExams }}</span>
             <span class="stat-label">待发布考试</span>
           </div>
         </div>
         <div class="stat-card todo-student">
           <div class="stat-icon"><el-icon :size="24"><User /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ teacherStats.studentCount }}</span>
             <span class="stat-label">所教学生</span>
           </div>
         </div>
       </div>
 
       <div class="home-grid-2col">
         <!-- Appointment Trend Chart -->
         <div class="home-card chart-card">
           <div class="card-header">
             <h3><el-icon><DataLine /></el-icon> 预约量趋势</h3>
           </div>
           <div class="card-body chart-body">
             <div ref="teacherChartRef" class="chart-container"></div>
           </div>
         </div>
 
         <!-- Latest Activity -->
         <div class="home-card">
           <div class="card-header">
             <h3><el-icon><Clock /></el-icon> 最新动态</h3>
           </div>
           <div class="card-body">
             <div v-for="item in activities" :key="item.id" class="activity-item">
               <div class="activity-dot" :style="{ background: item.color || '#409eff' }"></div>
               <div class="activity-content">
                 <span class="activity-text">{{ item.text }}</span>
                 <span class="activity-time">{{ item.time }}</span>
               </div>
             </div>
             <div v-if="activities.length === 0" class="empty-hint">暂无最新动态</div>
           </div>
         </div>
       </div>
     </template>
 
     <!-- Admin Dashboard -->
     <template v-if="authStore.role === 'admin'">
       <!-- System Overview Stats -->
       <div class="stat-cards">
         <div class="stat-card admin-user">
           <div class="stat-icon"><el-icon :size="28"><User /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ adminStats.userCount }}</span>
             <span class="stat-label">注册用户</span>
           </div>
           <div class="stat-trend up">实时统计</div>
         </div>
         <div class="stat-card admin-course">
           <div class="stat-icon"><el-icon :size="28"><Reading /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ adminStats.courseCount }}</span>
             <span class="stat-label">课程总数</span>
           </div>
           <div class="stat-trend up">实时统计</div>
         </div>
         <div class="stat-card admin-experiment">
           <div class="stat-icon"><el-icon :size="28"><Monitor /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ adminStats.experimentCount }}</span>
             <span class="stat-label">实验项目</span>
           </div>
           <div class="stat-trend up">实时统计</div>
         </div>
         <div class="stat-card admin-exam">
           <div class="stat-icon"><el-icon :size="28"><EditPen /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ adminStats.examCount }}%</span>
             <span class="stat-label">考试通过率</span>
           </div>
           <div class="stat-trend up">实时统计</div>
         </div>
         <div class="stat-card admin-reservation">
           <div class="stat-icon"><el-icon :size="28"><Calendar /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ adminStats.reservationCount }}</span>
             <span class="stat-label">实验预约</span>
           </div>
           <div class="stat-trend up">实时统计</div>
         </div>
         <div class="stat-card admin-report">
           <div class="stat-icon"><el-icon :size="28"><Document /></el-icon></div>
           <div class="stat-info">
             <span class="stat-num">{{ adminStats.reportCount }}</span>
             <span class="stat-label">实验报告</span>
           </div>
           <div class="stat-trend up">实时统计</div>
         </div>
       </div>
 
       <div class="home-grid-3col">
         <!-- Announcement Quick Entry -->
         <div class="home-card">
           <div class="card-header">
             <h3><el-icon><Bell /></el-icon> 公告管理</h3>
             <el-button text type="primary" size="small" @click="router.push('/admin/notices')">发布公告</el-button>
           </div>
           <div class="card-body">
             <div v-for="item in adminAnnouncements" :key="item.id" class="announcement-item">
               <div class="announcement-tag">
                 <el-tag :type="item.priority === 'high' ? 'danger' : 'info'" size="small">
                   {{ item.priority === 'high' ? '置顶' : '普通' }}
                 </el-tag>
               </div>
               <div class="announcement-content">
                 <span class="announcement-title">{{ item.title }}</span>
                 <span class="announcement-time">{{ item.time }}</span>
               </div>
             </div>
             <div v-if="adminAnnouncements.length === 0" class="empty-hint">暂无公告</div>
           </div>
         </div>
 
         <!-- System Status -->
         <div class="home-card">
           <div class="card-header">
             <h3><el-icon><Monitor /></el-icon> 系统状态</h3>
           </div>
           <div class="card-body">
             <div class="status-item">
             <span class="status-name">API 服务</span>
             <el-tag size="small" type="success">正常</el-tag>
              <span class="status-uptime">已连接新后端</span>
             </div>
             <div class="status-item">
             <span class="status-name">数据库</span>
             <el-tag size="small" type="success">正常</el-tag>
              <span class="status-uptime">ogexpsafetyplatform</span>
             </div>
             <div class="status-item">
             <span class="status-name">文件存储</span>
              <el-tag size="small" type="info">未接入</el-tag>
              <span class="status-uptime">等待后端接口</span>
             </div>
             <div class="status-item">
             <span class="status-name">AI 服务</span>
              <el-tag size="small" type="info">未接入</el-tag>
              <span class="status-uptime">等待后端接口</span>
             </div>
             <div class="status-item">
             <span class="status-name">存储使用</span>
              <el-progress :percentage="0" :stroke-width="8" />
              <span class="status-uptime">暂无接口</span>
             </div>
           </div>
         </div>
 
         <!-- Recent Log Timeline -->
         <div class="home-card">
           <div class="card-header">
             <h3><el-icon><List /></el-icon> 最近日志</h3>
             <el-button text type="primary" size="small" @click="router.push('/admin/logs')">查看全部</el-button>
           </div>
           <div class="card-body">
             <el-timeline>
               <el-timeline-item
                 v-for="log in recentLogs"
                 :key="log.id"
                 :timestamp="log.time"
                 :type="log.type"
                 :size="log.type === 'primary' ? 'large' : 'default'"
               >
                 {{ log.content }}
               </el-timeline-item>
             </el-timeline>
             <div v-if="recentLogs.length === 0" class="empty-hint">暂无日志记录</div>
           </div>
         </div>
       </div>
     </template>
   </div>
 </template>
 
 <script setup>
 import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
 import { useRouter } from 'vue-router'
 import { useAuthStore } from '@/stores/authStore'
 import * as echarts from 'echarts'
 import { Plus, EditPen, Calendar, Document, Trophy, Bell, DataAnalysis, DataLine, Clock, StarFilled, User, Reading, Monitor, List } from '@element-plus/icons-vue'
 import {
   getCourseCompletion,
   getDashboardOverview,
   getHotResources,
   getReservationTrend,
   getResourceTypeDistribution,
 } from '@/api/dashboard'
 
 const router = useRouter()
 const authStore = useAuthStore()
 
 const studentChartRef = ref(null)
 const teacherChartRef = ref(null)
 const studentChartData = ref([])
 const teacherTrendData = ref([])
 let studentChart = null
 let teacherChart = null
 
 // Student stats
 const studentStats = reactive({
   examCount: 0,
   reserveCount: 0,
   reportCount: 0,
   avgScore: 0,
 })

 const studentProgress = reactive({
   percent: 0,
   courseDone: 0,
   courseTotal: 0,
   experimentDone: 0,
   experimentTotal: 0,
 })
 
 // Teacher stats
 const teacherStats = reactive({
   pendingReservations: 0,
   pendingReports: 0,
   pendingExams: 0,
   studentCount: 0,
   todoTotal: 0,
 })
 
 // Admin stats
 const adminStats = reactive({
   userCount: 0,
   courseCount: 0,
   experimentCount: 0,
   examCount: 0,
   reservationCount: 0,
   reportCount: 0,
 })
 
 // Announcements
 const announcements = reactive([])
 
 const adminAnnouncements = reactive([])
 
 // Recommendations
 const recommendations = reactive([])
 
 // Activities (teacher)
 const activities = reactive([])
 
 // Recent logs (admin)
 const recentLogs = reactive([])
 
 function handleRecommendClick(item) {
   if (item.type === 'resource') {
     router.push('/student/courses')
   }
 }

 async function loadHomeData() {
   await Promise.allSettled([
     loadOverview(),
     loadCourseProgress(),
     loadRecommendations(),
     loadChartData(),
   ])
 }

 async function loadOverview() {
   const data = await getDashboardOverview({})
   if (!data) return

   const courseCount = toNumber(data.courseCount)
   const experimentCount = toNumber(data.experimentCount)
   const studentCount = toNumber(data.studentCount)
   const monthReservationCount = toNumber(data.monthReservationCount)
   const pendingReservationCount = toNumber(data.pendingReservationCount)
   const pendingReportCount = toNumber(data.pendingReportCount)
   const examPassRate = Math.round(toNumber(data.examPassRate))

   studentStats.examCount = 0
   studentStats.reserveCount = pendingReservationCount
   studentStats.reportCount = pendingReportCount
   studentStats.avgScore = examPassRate

   studentProgress.courseTotal = courseCount
   studentProgress.experimentTotal = experimentCount

   teacherStats.pendingReservations = pendingReservationCount
   teacherStats.pendingReports = pendingReportCount
   teacherStats.pendingExams = 0
   teacherStats.studentCount = studentCount
   teacherStats.todoTotal = pendingReservationCount + pendingReportCount

   adminStats.userCount = studentCount
   adminStats.courseCount = courseCount
   adminStats.experimentCount = experimentCount
   adminStats.examCount = examPassRate
   adminStats.reservationCount = monthReservationCount
   adminStats.reportCount = pendingReportCount
 }

 async function loadCourseProgress() {
   const list = await getCourseCompletion({})
   if (!Array.isArray(list) || list.length === 0) {
     studentProgress.percent = 0
     studentProgress.courseDone = 0
     studentProgress.experimentDone = 0
     return
   }

   const rates = list.map((item) => toNumber(item.completionRate))
   studentProgress.percent = Math.round(rates.reduce((sum, rate) => sum + rate, 0) / rates.length)
   studentProgress.courseDone = rates.filter((rate) => rate >= 100).length
   studentProgress.courseTotal = list.length
 }

 async function loadRecommendations() {
   const list = await getHotResources({ limit: 4 })
   recommendations.splice(0, recommendations.length, ...(list || []).map((item) => ({
     id: item.resourceId,
     title: item.title,
     type: 'resource',
     score: toNumber(item.viewCount),
     reason: `${resourceTypeLabel(item.resourceType)}，浏览量 ${toNumber(item.viewCount)}`,
   })))
 }

 async function loadChartData() {
   const [resourceResult, trendResult] = await Promise.allSettled([
     getResourceTypeDistribution({}),
     getReservationTrend({ limit: 7 }),
   ])

   if (resourceResult.status === 'fulfilled') {
     studentChartData.value = (resourceResult.value || []).map((item) => ({
       value: toNumber(item.value),
       name: resourceTypeLabel(item.name),
     }))
   }

   if (trendResult.status === 'fulfilled') {
     teacherTrendData.value = trendResult.value || []
   }
 }

 function toNumber(value) {
   const number = Number(value || 0)
   return Number.isFinite(number) ? number : 0
 }

 function resourceTypeLabel(type) {
   const labels = {
     VIDEO: '视频资源',
     DOCUMENT: '文档资料',
     IMAGE: '图片资料',
     LINK: '外部链接',
     FILE: '文件资料',
     UNKNOWN: '未分类资源',
   }
   return labels[type] || type || '未分类资源'
 }
 
 // ECharts - Student learning stats pie
 function initStudentChart() {
   if (!studentChartRef.value) return
   studentChart = echarts.init(studentChartRef.value)
   const hasData = studentChartData.value.some((item) => item.value > 0)
   studentChart.setOption({
     title: hasData ? undefined : {
       text: '暂无数据',
       left: 'center',
       top: 'middle',
       textStyle: { color: '#98a2b3', fontSize: 14, fontWeight: 400 },
     },
     tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
     legend: {
       bottom: 0,
       textStyle: { fontSize: 12 },
       itemWidth: 10,
       itemHeight: 10,
     },
     series: [
       {
         type: 'pie',
         radius: ['45%', '70%'],
         center: ['50%', '45%'],
         avoidLabelOverlap: false,
         label: { show: false },
         emphasis: {
           label: { show: true, fontSize: 14, fontWeight: 'bold' },
           itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' },
         },
         data: hasData ? studentChartData.value : [],
         itemStyle: {
           borderRadius: 4,
           borderColor: '#fff',
           borderWidth: 2,
         },
       },
     ],
   })
 }
 
 // ECharts - Teacher appointment trend
 function initTeacherChart() {
   if (!teacherChartRef.value) return
   teacherChart = echarts.init(teacherChartRef.value)
   const labels = teacherTrendData.value.map((item) => item.statDate)
   const values = teacherTrendData.value.map((item) => toNumber(item.value))
   const hasData = values.some((item) => item > 0)
   teacherChart.setOption({
     title: hasData ? undefined : {
       text: '暂无数据',
       left: 'center',
       top: 'middle',
       textStyle: { color: '#98a2b3', fontSize: 14, fontWeight: 400 },
     },
     tooltip: { trigger: 'axis' },
     grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
     xAxis: {
       type: 'category',
       data: labels.length ? labels : ['暂无数据'],
       axisLabel: { fontSize: 11 },
       axisLine: { lineStyle: { color: '#eee' } },
     },
     yAxis: {
       type: 'value',
       splitLine: { lineStyle: { color: '#f5f5f5' } },
       axisLabel: { fontSize: 11 },
     },
     series: [
       {
         name: '预约量',
         type: 'line',
         smooth: true,
         symbol: 'circle',
         symbolSize: 8,
         lineStyle: { width: 3, color: '#409eff' },
         areaStyle: {
           color: {
             type: 'linear',
             x: 0, y: 0, x2: 0, y2: 1,
             colorStops: [
               { offset: 0, color: 'rgba(64,158,255,0.3)' },
               { offset: 1, color: 'rgba(64,158,255,0.02)' },
             ],
           },
         },
         itemStyle: { color: '#409eff' },
         data: labels.length ? values : [0],
       },
     ],
     legend: {
       bottom: 0,
       textStyle: { fontSize: 12 },
       itemWidth: 14,
       itemHeight: 10,
     },
   })
 }
 
 onMounted(async () => {
   await loadHomeData()
   nextTick(() => {
     if (authStore.role === 'student') initStudentChart()
     if (authStore.role === 'teacher') initTeacherChart()
   })
   window.addEventListener('resize', handleResize)
 })
 
 onUnmounted(() => {
   window.removeEventListener('resize', handleResize)
   studentChart?.dispose()
   teacherChart?.dispose()
 })
 
 function handleResize() {
   studentChart?.resize()
   teacherChart?.resize()
 }
 </script>
 
 <style scoped>
 .home-page {
   max-width: 1200px;
   margin: 0 auto;
 }
 
 /* Welcome Banner */
 .welcome-banner {
   background: linear-gradient(135deg, #1a3a5c 0%, #2d5f8a 100%);
   border-radius: 12px;
   padding: 24px 28px;
   display: flex;
   justify-content: space-between;
   align-items: center;
   gap: 24px;
   margin-bottom: 20px;
   color: #fff;
 }
 .teacher-banner {
   background: linear-gradient(135deg, #1d4e3a 0%, #3a7d5a 100%);
 }
 .welcome-title {
   font-size: 22px;
   font-weight: 600;
   margin-bottom: 6px;
 }
 .welcome-desc {
   font-size: 14px;
   opacity: 0.8;
 }
 .welcome-desc strong {
   color: #ffd666;
   font-weight: 600;
 }
 .welcome-progress {
   min-width: 280px;
 }
 .progress-label {
   display: flex;
   justify-content: space-between;
   font-size: 13px;
   margin-bottom: 6px;
   opacity: 0.9;
 }
 .progress-value {
   font-weight: 600;
   font-size: 15px;
 }
 .progress-detail {
   display: flex;
   justify-content: space-between;
   font-size: 12px;
   opacity: 0.7;
   margin-top: 6px;
 }
 .quick-actions {
   display: flex;
   gap: 8px;
   flex-shrink: 0;
 }
 
 /* Stat Cards */
 .stat-cards {
   display: grid;
   grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
   gap: 16px;
   margin-bottom: 20px;
 }
 .stat-card {
   background: #fff;
   border-radius: 12px;
   padding: 20px;
   display: flex;
   align-items: center;
   gap: 16px;
   cursor: pointer;
   transition: all 0.25s;
   border: 1px solid #f0f0f0;
   position: relative;
 }
 .stat-card:hover {
   transform: translateY(-2px);
   box-shadow: 0 6px 20px rgba(0,0,0,0.08);
   border-color: transparent;
 }
 .stat-icon {
   width: 48px;
   height: 48px;
   border-radius: 12px;
   display: flex;
   align-items: center;
   justify-content: center;
   flex-shrink: 0;
 }
 .todo-exam .stat-icon { background: #ecf5ff; color: #409eff; }
 .todo-reserve .stat-icon { background: #f0f9eb; color: #67c23a; }
 .todo-report .stat-icon { background: #fef0f0; color: #f56c6c; }
 .todo-grade .stat-icon { background: #fdf6ec; color: #e6a23c; }
 .todo-student .stat-icon { background: #f4f4f5; color: #909399; }
 .admin-user .stat-icon { background: #ecf5ff; color: #409eff; }
 .admin-course .stat-icon { background: #f0f9eb; color: #67c23a; }
 .admin-experiment .stat-icon { background: #fdf6ec; color: #e6a23c; }
 .admin-exam .stat-icon { background: #fef0f0; color: #f56c6c; }
 .admin-reservation .stat-icon { background: #f0f9eb; color: #67c23a; }
 .admin-report .stat-icon { background: #ecf5ff; color: #409eff; }
 .stat-info {
   display: flex;
   flex-direction: column;
 }
 .stat-num {
   font-size: 28px;
   font-weight: 700;
   color: #1a1a2e;
   line-height: 1.2;
 }
 .stat-label {
   font-size: 13px;
   color: #999;
 }
 .stat-trend {
   position: absolute;
   top: 12px;
   right: 12px;
   font-size: 11px;
   padding: 2px 8px;
   border-radius: 10px;
   font-weight: 500;
 }
 .stat-trend.up {
   color: #67c23a;
   background: #f0f9eb;
 }
 .stat-trend.down {
   color: #f56c6c;
   background: #fef0f0;
 }
 
 /* Grid Layouts */
 .home-grid-2col {
   display: grid;
   grid-template-columns: 1fr 1fr;
   gap: 16px;
   margin-bottom: 20px;
 }
 .home-grid-3col {
   display: grid;
   grid-template-columns: 1fr 1fr 1fr;
   gap: 16px;
   margin-bottom: 20px;
 }
 
 /* Home Card */
 .home-card {
   background: #fff;
   border-radius: 12px;
   border: 1px solid #f0f0f0;
   overflow: hidden;
   transition: box-shadow 0.25s;
 }
 .home-card:hover {
   box-shadow: 0 4px 16px rgba(0,0,0,0.04);
 }
 .card-header {
   display: flex;
   justify-content: space-between;
   align-items: center;
   padding: 16px 20px;
   border-bottom: 1px solid #f5f5f5;
 }
 .card-header h3 {
   font-size: 15px;
   font-weight: 600;
   color: #1a1a2e;
   display: flex;
   align-items: center;
   gap: 6px;
 }
 .card-header h3 .el-icon {
   color: #409eff;
 }
 .card-body {
   padding: 16px 20px;
 }
 .chart-body {
   padding: 8px 12px 4px;
 }
 .chart-container {
   width: 100%;
   height: 240px;
 }
 
 /* Announcement Items */
 .announcement-item {
   display: flex;
   align-items: center;
   gap: 10px;
   padding: 10px 0;
   border-bottom: 1px solid #f5f5f5;
   cursor: pointer;
   transition: background 0.2s;
 }
 .announcement-item:last-child {
   border-bottom: none;
 }
 .announcement-item:hover {
   background: #fafafa;
   margin: 0 -20px;
   padding-left: 20px;
   padding-right: 20px;
 }
 .announcement-tag {
   flex-shrink: 0;
 }
 .announcement-content {
   flex: 1;
   display: flex;
   justify-content: space-between;
   align-items: center;
   gap: 12px;
 }
 .announcement-title {
   font-size: 14px;
   color: #333;
 }
 .announcement-time {
   font-size: 12px;
   color: #999;
   flex-shrink: 0;
 }
 
 /* Recommended Resources */
 .recommend-grid {
   display: grid;
   grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
   gap: 12px;
 }
 .recommend-card {
   display: flex;
   flex-direction: column;
   gap: 6px;
   padding: 14px;
   border: 1px solid #f0f0f0;
   border-radius: 10px;
   cursor: pointer;
   transition: all 0.2s;
 }
 .recommend-card:hover {
   border-color: #409eff;
   background: #f8fbff;
 }
 .recommend-icon {
   width: 32px;
   height: 32px;
   border-radius: 8px;
   display: flex;
   align-items: center;
   justify-content: center;
 }
 .recommend-icon.resource { background: #ecf5ff; color: #409eff; }
 .recommend-icon.experiment { background: #f0f9eb; color: #67c23a; }
 .recommend-icon.knowledge { background: #fdf6ec; color: #e6a23c; }
 .recommend-info {
   display: flex;
   justify-content: space-between;
   align-items: center;
 }
 .recommend-title {
   font-size: 14px;
   font-weight: 500;
   color: #333;
 }
 .recommend-reason {
   font-size: 12px;
   color: #999;
   line-height: 1.4;
 }
 
 /* Activities */
 .activity-item {
   display: flex;
   align-items: flex-start;
   gap: 12px;
   padding: 10px 0;
   border-bottom: 1px solid #f5f5f5;
 }
 .activity-item:last-child {
   border-bottom: none;
 }
 .activity-dot {
   width: 8px;
   height: 8px;
   border-radius: 50%;
   margin-top: 6px;
   flex-shrink: 0;
 }
 .activity-content {
   flex: 1;
   display: flex;
   flex-direction: column;
   gap: 2px;
 }
 .activity-text {
   font-size: 14px;
   color: #333;
 }
 .activity-time {
   font-size: 12px;
   color: #999;
 }
 
 /* Status Items */
 .status-item {
   display: flex;
   align-items: center;
   gap: 12px;
   padding: 10px 0;
   border-bottom: 1px solid #f5f5f5;
 }
 .status-item:last-child {
   border-bottom: none;
   flex-wrap: wrap;
 }
 .status-name {
   font-size: 14px;
   color: #333;
   min-width: 80px;
 }
 .status-uptime {
   font-size: 12px;
   color: #999;
   margin-left: auto;
 }
 
 /* Empty Hint */
 .empty-hint {
   text-align: center;
   padding: 24px;
   color: #ccc;
   font-size: 14px;
 }
 
 /* Responsive */
 @media (max-width: 1024px) {
   .home-grid-2col,
   .home-grid-3col {
     grid-template-columns: 1fr;
   }
 }
 @media (max-width: 768px) {
   .welcome-banner {
     flex-direction: column;
     align-items: stretch;
     text-align: center;
   }
   .quick-actions {
     justify-content: center;
     flex-wrap: wrap;
   }
   .welcome-progress {
     min-width: auto;
   }
   .stat-cards {
     grid-template-columns: repeat(2, 1fr);
   }
   .recommend-grid {
     grid-template-columns: 1fr;
   }
 }
 </style>
