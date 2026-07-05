<template>
  <div class="course-list-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Student Courses</p>
        <h1>实验课程列表</h1>
        <p class="page-desc">浏览实验课程、查看实验项目进度，并继续进入相关安全学习与实验任务。</p>
      </div>
      <div class="head-stats">
        <div class="head-stat">
          <strong>{{ courseStats.total }}</strong>
          <span>课程总数</span>
        </div>
        <div class="head-stat">
          <strong>{{ courseStats.open }}</strong>
          <span>开放课程</span>
        </div>
        <div class="head-stat">
          <strong>{{ courseStats.experiments }}</strong>
          <span>实验项目</span>
        </div>
      </div>
    </section>

    <section class="filter-bar">
      <el-input
        v-model="filters.keyword"
        :prefix-icon="Search"
        clearable
        placeholder="搜索课程名称或编号"
        class="keyword-input"
      />
      <el-select v-model="filters.direction" placeholder="专业方向" clearable>
        <el-option v-for="item in directionOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="filters.semester" placeholder="开设学期" clearable>
        <el-option v-for="item in semesterOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-radio-group v-model="filters.status" class="status-tabs">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button :label="1">开放</el-radio-button>
        <el-radio-button :label="0">停用</el-radio-button>
      </el-radio-group>
    </section>

    <section class="content-grid">
      <div class="course-panel">
        <div v-loading="loading" class="course-grid">
          <article v-for="course in courses" :key="course.id" class="course-card">
            <div class="course-cover" :style="{ '--accent': accentForCourse(course) }">
              <div class="cover-icon">
                <el-icon :size="26"><Monitor /></el-icon>
              </div>
              <div class="cover-meta">
                <span>{{ course.courseCode || '-' }}</span>
                <el-tag size="small" :type="course.status === 1 ? 'success' : 'info'">
                  {{ course.status === 1 ? '开放' : '停用' }}
                </el-tag>
              </div>
            </div>

            <div class="course-body">
              <div class="course-title-row">
                <h2>{{ course.courseName }}</h2>
                <el-tag size="small" effect="plain">{{ course.direction || '未分类' }}</el-tag>
              </div>
              <p class="course-summary">{{ course.description || '暂无课程说明' }}</p>

              <div class="course-meta">
                <span><el-icon><User /></el-icon>{{ course.teacherName || '未分配教师' }}</span>
                <span><el-icon><Calendar /></el-icon>{{ course.semester || '未设置学期' }}</span>
                <span><el-icon><Collection /></el-icon>{{ course.resourceCount || 0 }} 个资源</span>
              </div>

              <div class="progress-box">
                <div class="progress-head">
                  <span>平均学习进度</span>
                  <strong>{{ progressOf(course) }}%</strong>
                </div>
                <el-progress :percentage="progressOf(course)" :stroke-width="8" />
                <div class="experiment-row">
                  <span>实验项目 {{ course.experimentCount || 0 }} 个</span>
                  <span>课程 ID {{ course.id }}</span>
                </div>
              </div>

              <div class="tag-row">
                <span>{{ course.direction || '实验课程' }}</span>
                <span>{{ course.semester || '未设置学期' }}</span>
                <span>{{ course.status === 1 ? '可学习' : '暂不可用' }}</span>
              </div>
            </div>

            <div class="course-actions">
              <el-button :icon="View" @click="openCourseDetail(course)">查看详情</el-button>
              <el-button type="primary" :icon="VideoPlay" @click="openCourseDetail(course)">
                继续学习
              </el-button>
            </div>
          </article>

          <el-empty v-if="!loading && courses.length === 0" description="暂无符合条件的课程" />
        </div>

        <div class="pagination-row">
          <span>共 {{ total }} 条记录</span>
          <el-pagination
            v-model:current-page="pageNum"
            layout="prev, pager, next"
            :page-size="pageSize"
            :total="total"
            @current-change="loadCourses"
          />
        </div>
      </div>

      <aside class="side-panel">
        <div class="panel-section">
          <div class="panel-title">
            <el-icon><TrendCharts /></el-icon>
            <span>课程概览</span>
          </div>
          <div v-for="item in courseOverview" :key="item.label" class="plan-item">
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.detail }}</span>
            </div>
            <el-tag size="small" :type="item.type">{{ item.value }}</el-tag>
          </div>
        </div>

        <div class="panel-section">
          <div class="panel-title">
            <el-icon><Warning /></el-icon>
            <span>安全提醒</span>
          </div>
          <div class="notice-box">
            高风险实验需要先完成安全知识学习与测评，达到及格分后再进入实验预约和实验步骤学习。
          </div>
        </div>

        <div class="panel-section">
          <div class="panel-title">
            <el-icon><StarFilled /></el-icon>
            <span>热门方向</span>
          </div>
          <button
            v-for="item in directionOptions.slice(0, 4)"
            :key="item"
            class="recommend-row"
            type="button"
            @click="filters.direction = item"
          >
            <span>{{ item }}</span>
            <small>点击筛选该方向课程</small>
          </button>
          <el-empty v-if="directionOptions.length === 0" description="暂无方向数据" :image-size="70" />
        </div>
      </aside>
    </section>

    <el-dialog v-model="detailVisible" title="课程详情" width="720px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="selectedDetail">
          <div class="detail-title">
            <h2>{{ selectedDetail.course?.courseName }}</h2>
            <el-tag>{{ selectedDetail.course?.courseCode }}</el-tag>
          </div>
          <p>{{ selectedDetail.course?.description || '暂无课程说明' }}</p>
          <div class="summary-grid">
            <div>
              <strong>{{ selectedDetail.experimentCount || 0 }}</strong>
              <span>实验项目</span>
            </div>
            <div>
              <strong>{{ selectedDetail.resourceCount || 0 }}</strong>
              <span>教学资源</span>
            </div>
            <div>
              <strong>{{ Number(selectedDetail.averageProgress || 0).toFixed(0) }}%</strong>
              <span>平均进度</span>
            </div>
          </div>
          <el-table :data="selectedDetail.experiments || []" stripe>
            <el-table-column prop="expName" label="实验名称" min-width="180" />
            <el-table-column prop="expCode" label="编号" width="120" />
            <el-table-column prop="riskLevel" label="风险等级" width="110" />
            <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  Collection,
  Monitor,
  Search,
  StarFilled,
  TrendCharts,
  User,
  VideoPlay,
  View,
  Warning,
} from '@element-plus/icons-vue'
import { getCourseDetail, getCourses } from '@/api/course'

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const courses = ref([])
const selectedDetail = ref(null)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(8)
let keywordTimer = null

const filters = reactive({
  keyword: '',
  direction: '',
  semester: '',
  status: '',
})

const directionOptions = computed(() => uniqueValues(courses.value.map((course) => course.direction)))
const semesterOptions = computed(() => uniqueValues(courses.value.map((course) => course.semester)))

const courseStats = computed(() => ({
  total: total.value,
  open: courses.value.filter((course) => course.status === 1).length,
  experiments: courses.value.reduce((sum, course) => sum + Number(course.experimentCount || 0), 0),
}))

const courseOverview = computed(() => [
  { label: '当前页资源', detail: '课程关联教学资源数量', value: courses.value.reduce((sum, course) => sum + Number(course.resourceCount || 0), 0), type: 'primary' },
  { label: '当前页实验', detail: '课程关联实验项目数量', value: courseStats.value.experiments, type: 'success' },
  { label: '开放课程', detail: '当前筛选结果中的开放课程', value: courseStats.value.open, type: 'warning' },
])

watch(() => filters.keyword, () => {
  pageNum.value = 1
  window.clearTimeout(keywordTimer)
  keywordTimer = window.setTimeout(loadCourses, 300)
})

watch([() => filters.direction, () => filters.semester, () => filters.status], () => {
  pageNum.value = 1
  loadCourses()
})

onMounted(loadCourses)

async function loadCourses() {
  loading.value = true
  try {
    const result = await getCourses({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      direction: filters.direction || undefined,
      semester: filters.semester || undefined,
      status: filters.status === '' ? undefined : filters.status,
    })
    courses.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

async function openCourseDetail(course) {
  detailVisible.value = true
  detailLoading.value = true
  selectedDetail.value = null
  try {
    selectedDetail.value = await getCourseDetail(course.id)
  } catch {
    ElMessage.warning('课程详情暂时无法加载')
  } finally {
    detailLoading.value = false
  }
}

function progressOf(course) {
  return Math.max(0, Math.min(100, Number(course.averageProgress || 0)))
}

function accentForCourse(course) {
  const colors = ['#2d6a4f', '#33658a', '#77567a', '#a44a3f', '#3d5a80']
  return colors[Number(course.id || 0) % colors.length]
}

function uniqueValues(values) {
  return [...new Set(values.filter(Boolean))]
}
</script>

<style scoped>
.course-list-page {
  max-width: 1240px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 18px;
}

.eyebrow {
  color: #6b7c8f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 6px;
}

.page-head h1 {
  color: #13233a;
  font-size: 26px;
  line-height: 1.2;
  margin-bottom: 8px;
}

.page-desc {
  color: #667085;
  line-height: 1.6;
}

.head-stats {
  display: grid;
  grid-template-columns: repeat(3, 92px);
  gap: 10px;
  flex-shrink: 0;
}

.head-stat {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 12px 10px;
  text-align: center;
}

.head-stat strong {
  display: block;
  color: #13233a;
  font-size: 22px;
  line-height: 1;
  margin-bottom: 6px;
}

.head-stat span {
  color: #7b8794;
  font-size: 12px;
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 150px 140px auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 14px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

.course-panel {
  min-width: 0;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  min-height: 240px;
}

.course-card {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 360px;
  transition: box-shadow 0.2s, transform 0.2s;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
}

.course-cover {
  min-height: 86px;
  padding: 16px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 88%, #ffffff 12%), color-mix(in srgb, var(--accent) 62%, #1b1b1b 38%));
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.cover-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
}

.course-body {
  padding: 16px;
  flex: 1;
}

.course-title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 8px;
}

.course-title-row h2 {
  color: #182230;
  font-size: 17px;
  line-height: 1.35;
}

.course-summary {
  min-height: 44px;
  color: #667085;
  line-height: 1.55;
  margin-bottom: 14px;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: #667085;
  font-size: 13px;
  margin-bottom: 14px;
}

.course-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.progress-box {
  background: #f8fafc;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.progress-head,
.experiment-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.progress-head {
  color: #344054;
  margin-bottom: 8px;
}

.progress-head strong {
  color: #13233a;
}

.experiment-row {
  color: #7b8794;
  font-size: 12px;
  margin-top: 8px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row span {
  color: #536579;
  background: #f2f5f8;
  border-radius: 999px;
  padding: 4px 9px;
  font-size: 12px;
}

.course-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px 16px;
  border-top: 1px solid #f0f2f5;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #667085;
  padding: 14px 0;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-section {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 16px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #13233a;
  font-weight: 700;
  margin-bottom: 12px;
}

.panel-title .el-icon {
  color: #409eff;
}

.plan-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 11px 0;
  border-bottom: 1px solid #f0f2f5;
}

.plan-item:last-child {
  border-bottom: none;
}

.plan-item strong,
.recommend-row span {
  display: block;
  color: #243447;
  font-size: 14px;
  margin-bottom: 4px;
}

.plan-item span,
.recommend-row small {
  color: #7b8794;
  font-size: 12px;
}

.notice-box {
  color: #7a4b00;
  background: #fff7e6;
  border: 1px solid #ffe3a3;
  border-radius: 8px;
  padding: 12px;
  line-height: 1.6;
}

.recommend-row {
  width: 100%;
  text-align: left;
  border: none;
  background: #f8fafc;
  border-radius: 8px;
  padding: 11px 12px;
  cursor: pointer;
  margin-bottom: 8px;
}

.recommend-row:hover {
  background: #eef6ff;
}

.detail-body {
  min-height: 180px;
}

.detail-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.detail-title h2 {
  color: #13233a;
  font-size: 20px;
}

.detail-body p {
  color: #667085;
  line-height: 1.6;
  margin-bottom: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.summary-grid div {
  background: #f8fafc;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 12px;
}

.summary-grid strong {
  display: block;
  color: #13233a;
  font-size: 22px;
}

.summary-grid span {
  color: #7b8794;
  font-size: 12px;
}

:deep(.progress-box .el-progress-bar__outer) {
  background: #e7edf3 !important;
}

@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 860px) {
  .page-head {
    align-items: stretch;
    flex-direction: column;
  }

  .head-stats,
  .side-panel,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    grid-template-columns: 1fr;
  }

  .status-tabs {
    overflow-x: auto;
  }
}
</style>
