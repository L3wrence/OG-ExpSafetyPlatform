<template>
  <div class="dashboard-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Teaching Dashboard</p>
        <h1>教师工作台</h1>
        <p class="page-desc">集中查看预约审核、报告批改、安全考试和学习活跃情况。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">刷新</el-button>
    </section>

    <section class="filter-bar">
      <el-input v-model="filters.courseId" clearable placeholder="课程ID" />
      <el-input v-model="filters.experimentId" clearable placeholder="实验ID" />
      <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-button type="primary" :icon="Search" @click="loadDashboard">筛选</el-button>
    </section>

    <el-alert v-if="error" class="state-alert" type="error" :closable="false" :title="error" />

    <section class="metric-grid" v-loading="loading">
      <button class="metric-card" type="button" @click="router.push('/teacher/reservations')">
        <span>待审核预约</span><strong>{{ overview.pendingReservationCount || 0 }}</strong>
      </button>
      <button class="metric-card" type="button" @click="router.push('/teacher/reports')">
        <span>待批改报告</span><strong>{{ overview.pendingReportCount || 0 }}</strong>
      </button>
      <button class="metric-card" type="button" @click="router.push('/teacher/exam-papers')">
        <span>待批改主观题</span><strong>{{ overview.pendingSubjectiveCount || 0 }}</strong>
      </button>
      <div class="metric-card static">
        <span>安全考试通过率</span><strong>{{ Number(overview.examPassRate || 0).toFixed(1) }}%</strong>
      </div>
      <div class="metric-card static">
        <span>课程数</span><strong>{{ overview.courseCount || 0 }}</strong>
      </div>
      <div class="metric-card static">
        <span>实验数</span><strong>{{ overview.experimentCount || 0 }}</strong>
      </div>
    </section>

    <section class="chart-grid" v-loading="loading">
      <div class="chart-panel">
        <div class="panel-title"><h2>课程学习完成率</h2></div>
        <div v-if="courseCompletion.length" ref="courseChartRef" class="chart-box"></div>
        <el-empty v-else description="暂无课程学习数据" />
      </div>
      <div class="chart-panel">
        <div class="panel-title"><h2>安全考试通过率</h2></div>
        <div v-if="examPassRate.length" ref="passChartRef" class="chart-box"></div>
        <el-empty v-else description="暂无考试数据" />
      </div>
      <div class="chart-panel">
        <div class="panel-title"><h2>错题知识点 Top 10</h2></div>
        <div v-if="wrongKnowledge.length" ref="wrongChartRef" class="chart-box"></div>
        <el-empty v-else description="暂无错题数据" />
      </div>
      <div class="chart-panel">
        <div class="panel-title"><h2>报告成绩分布</h2></div>
        <div v-if="scoreDistribution.length" ref="scoreChartRef" class="chart-box"></div>
        <el-empty v-else description="暂无报告评分数据" />
      </div>
      <div class="chart-panel wide">
        <div class="panel-title"><h2>最近 30 天学习活跃趋势</h2></div>
        <div v-if="activityTrend.length" ref="activityChartRef" class="chart-box"></div>
        <el-empty v-else description="暂无学习活跃数据" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  getCourseCompletion,
  getDashboardOverview,
  getExamPassRate,
  getLearningActivityTrend,
  getReportScoreDistribution,
  getWrongKnowledgeRanking,
} from '@/api/dashboard'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const dateRange = ref([])
const filters = reactive({ courseId: '', experimentId: '' })
const overview = reactive({})
const courseCompletion = ref([])
const examPassRate = ref([])
const wrongKnowledge = ref([])
const scoreDistribution = ref([])
const activityTrend = ref([])

const courseChartRef = ref(null)
const passChartRef = ref(null)
const wrongChartRef = ref(null)
const scoreChartRef = ref(null)
const activityChartRef = ref(null)
const charts = []

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  loadDashboard()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})

function queryParams() {
  return {
    courseId: filters.courseId || undefined,
    experimentId: filters.experimentId || undefined,
    startDate: dateRange.value?.[0] || undefined,
    endDate: dateRange.value?.[1] || undefined,
    limit: 10,
  }
}

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const params = queryParams()
    const [overviewData, courseData, passData, wrongData, scoreData, trendData] = await Promise.all([
      getDashboardOverview(params),
      getCourseCompletion(params),
      getExamPassRate(params),
      getWrongKnowledgeRanking(params),
      getReportScoreDistribution(params),
      getLearningActivityTrend(params),
    ])
    Object.assign(overview, overviewData || {})
    courseCompletion.value = courseData || []
    examPassRate.value = passData || []
    wrongKnowledge.value = wrongData || []
    scoreDistribution.value = scoreData || []
    activityTrend.value = trendData || []
    await nextTick()
    renderCharts()
  } catch (err) {
    error.value = err?.message || '教师工作台数据加载失败'
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  disposeCharts()
  if (courseCompletion.value.length && courseChartRef.value) {
    renderBar(courseChartRef.value, courseCompletion.value.map(item => item.courseName), courseCompletion.value.map(item => Number(item.completionRate || 0)), '完成率')
  }
  if (examPassRate.value.length && passChartRef.value) {
    renderBar(passChartRef.value, examPassRate.value.map(item => item.expName), examPassRate.value.map(item => Number(item.passRate || 0)), '通过率')
  }
  if (wrongKnowledge.value.length && wrongChartRef.value) {
    renderBar(wrongChartRef.value, wrongKnowledge.value.map(item => item.knowledgePoint), wrongKnowledge.value.map(item => Number(item.wrongCount || 0)), '错题数')
  }
  if (scoreDistribution.value.length && scoreChartRef.value) {
    const chart = echarts.init(scoreChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['45%', '72%'], data: scoreDistribution.value.map(item => ({ name: item.name, value: Number(item.value || 0) })) }],
    })
    charts.push(chart)
  }
  if (activityTrend.value.length && activityChartRef.value) {
    const chart = echarts.init(activityChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: activityTrend.value.map(item => item.statDate) },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, areaStyle: {}, data: activityTrend.value.map(item => Number(item.value || 0)) }],
      grid: { left: 42, right: 18, top: 28, bottom: 42 },
    })
    charts.push(chart)
  }
}

function renderBar(el, labels, values, name) {
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: labels, axisLabel: { interval: 0, rotate: labels.length > 4 ? 25 : 0 } },
    yAxis: { type: 'value' },
    series: [{ name, type: 'bar', data: values, barMaxWidth: 34 }],
    grid: { left: 42, right: 18, top: 28, bottom: 64 },
  })
  charts.push(chart)
}

function resizeCharts() {
  charts.forEach(chart => chart.resize())
}

function disposeCharts() {
  while (charts.length) {
    charts.pop()?.dispose()
  }
}
</script>

<style scoped>
.dashboard-page { max-width: 1280px; margin: 0 auto; }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 16px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.filter-bar { display: flex; gap: 10px; align-items: center; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 12px; margin-bottom: 14px; }
.filter-bar .el-input { max-width: 160px; }
.state-alert { margin-bottom: 14px; }
.metric-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.metric-card { min-height: 96px; text-align: left; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; cursor: pointer; }
.metric-card.static { cursor: default; }
.metric-card span { display: block; color: #667085; margin-bottom: 12px; }
.metric-card strong { color: #13233a; font-size: 28px; line-height: 1; }
.chart-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.chart-panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; min-height: 340px; }
.chart-panel.wide { grid-column: 1 / -1; }
.panel-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.panel-title h2 { color: #13233a; font-size: 16px; }
.chart-box { width: 100%; height: 280px; }
.wide .chart-box { height: 300px; }
@media (max-width: 1080px) { .metric-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 820px) { .chart-grid, .metric-grid { grid-template-columns: 1fr; } .filter-bar, .page-head { align-items: stretch; flex-direction: column; } .filter-bar .el-input { max-width: none; } }
</style>
