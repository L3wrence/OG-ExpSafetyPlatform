<template>
  <div class="course-list-page">
    <div class="classroom-upper">
      <section class="classroom-nav">
        <div class="classroom-tabs">
          <button
            v-for="tab in classroomTabs"
            :key="tab.name"
            :class="{ active: viewFilter === tab.name }"
            type="button"
            @click="viewFilter = tab.name"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="head-stats">
          <div class="head-stat">
            <strong>{{ courseStats.total }}</strong>
            <span>我的课堂</span>
          </div>
          <div class="head-stat">
            <strong>{{ courseStats.managed }}</strong>
            <span>我管理的</span>
          </div>
          <div class="head-stat">
            <strong>{{ courseStats.learning }}</strong>
            <span>我学习的</span>
          </div>
        </div>
      </section>

      <section class="filter-bar">
        <el-input
          v-model="filters.keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索课堂名称"
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
          <el-radio-button :label="1">正在进行</el-radio-button>
          <el-radio-button :label="2">已结束</el-radio-button>
        </el-radio-group>
      </section>

      <section class="action-strip">
        <div class="join-panel">
          <div>
            <strong>加入教学课堂</strong>
            <span>输入教师分享的邀请码，加入后即成为该课堂学生。</span>
          </div>
          <div class="join-actions">
            <el-input ref="inviteInputRef" v-model="inviteCode" clearable placeholder="请输入课堂邀请码" @keyup.enter="joinClassroom" />
            <el-button type="primary" :loading="joining" @click="joinClassroom">加入</el-button>
          </div>
        </div>
        <div class="create-panel">
          <div>
            <strong>创建教学课堂</strong>
            <span>{{ canManageClassroom ? '建设实验章节、任务、考核与报告。' : '完成教师认证后可创建和管理课堂。' }}</span>
          </div>
          <el-button :type="canManageClassroom ? 'success' : 'warning'" :icon="EditPen" @click="handleCreateClassroom">
            {{ canManageClassroom ? '创建课堂' : '去认证' }}
          </el-button>
        </div>
      </section>
    </div>

    <section class="content-grid">
      <div class="course-panel">
        <div v-loading="loading" class="course-grid">
          <article v-for="course in visibleCourses" :key="course.id" class="course-card">
            <div class="course-cover" :style="coverStyle(course)">
              <div class="cover-icon">
                <el-icon :size="26"><Monitor /></el-icon>
              </div>
              <div class="cover-meta">
                <span>{{ course.courseCode || '-' }}</span>
                <el-tag size="small" :type="courseRelationMeta(course).type">{{ courseRelationMeta(course).label }}</el-tag>
              </div>
            </div>

            <div class="course-body">
              <div class="course-title-row">
                <h2>{{ course.courseName }}</h2>
                <el-tag size="small" effect="plain">{{ course.direction || '未分类' }}</el-tag>
              </div>
              <p class="course-summary"><b>课程简介：</b>{{ course.description || '暂无课程简介' }}</p>
              <p class="course-tagline">{{ course.tagline || '从真实实验场景进入，完成资源学习、准入测评、预约操作与报告复盘。' }}</p>

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
                <span v-for="tag in courseTags(course)" :key="tag">{{ tag }}</span>
              </div>
            </div>

            <div class="course-actions">
              <template v-if="isManagedCourse(course)">
                <el-button :icon="View" @click="enterClassroom(course)">课堂详细</el-button>
                <el-button type="danger" plain :icon="Delete" @click="removeCourse(course)">删除</el-button>
              </template>
              <template v-else>
                <el-button :icon="View" @click="openCourseDetail(course)">查看详情</el-button>
                <el-button type="primary" :icon="VideoPlay" @click="enterClassroom(course)">进入学习</el-button>
              </template>
            </div>
          </article>

          <el-empty v-if="!loading && visibleCourses.length === 0" description="暂无符合条件的课堂" />
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
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button text type="primary" @click="goExperiment(row)">学习</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  Collection,
  Connection,
  Delete,
  EditPen,
  Monitor,
  Plus,
  Search,
  StarFilled,
  TrendCharts,
  User,
  VideoPlay,
  View,
  Warning,
} from '@element-plus/icons-vue'
import { deleteCourse, getCourseDetail, getCourses, joinCourseByInvite } from '@/api/course'
import { useAuthStore } from '@/stores/authStore'
import resourceCore from '@/assets/amazing/resource-core.png'

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const router = useRouter()
const authStore = useAuthStore()
const courses = ref([])
const selectedDetail = ref(null)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(8)
const inviteCode = ref('')
const inviteInputRef = ref(null)
const joining = ref(false)
const viewFilter = ref('all')
let keywordTimer = null

const filters = reactive({
  keyword: '',
  direction: '',
  semester: '',
  status: '',
})

const classroomTabs = [
  { name: 'all', label: '全部' },
  { name: 'managed', label: '我管理的' },
  { name: 'learning', label: '我学习的' },
  { name: 'hidden', label: '已隐藏' },
]

const directionOptions = computed(() => uniqueValues(courses.value.map((course) => course.direction)))
const semesterOptions = computed(() => uniqueValues(courses.value.map((course) => course.semester)))
const canManageClassroom = computed(() => authStore.hasPermission('course:create'))
const visibleCourses = computed(() => courses.value.filter((course) => {
  if (viewFilter.value === 'managed') return isManagedCourse(course)
  if (viewFilter.value === 'learning') return isLearningCourse(course)
  if (viewFilter.value === 'hidden') return false
  return true
}))

const courseStats = computed(() => ({
  total: courses.value.length,
  managed: courses.value.filter(isManagedCourse).length,
  learning: courses.value.filter(isLearningCourse).length,
  experiments: courses.value.reduce((sum, course) => sum + Number(course.experimentCount || 0), 0),
}))

const courseOverview = computed(() => [
  { label: '当前页资源', detail: '课程关联教学资源数量', value: courses.value.reduce((sum, course) => sum + Number(course.resourceCount || 0), 0), type: 'primary' },
  { label: '当前页实验', detail: '课程关联实验项目数量', value: courseStats.value.experiments, type: 'success' },
  { label: '进行中课堂', detail: '当前筛选结果中的进行中课堂', value: courses.value.filter((course) => course.status === 1).length, type: 'warning' },
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

function goExperiment(experiment) {
  const courseId = selectedDetail.value?.course?.id
  if (!courseId) return
  router.push({ path: `/classrooms/${courseId}/learn`, query: { experimentId: experiment.id } })
}

async function joinClassroom() {
  const code = inviteCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入课堂邀请码')
    return
  }
  joining.value = true
  try {
    const result = await joinCourseByInvite(code)
    ElMessage.success(`已加入课堂：${result?.courseName || '课堂'}`)
    inviteCode.value = ''
    await loadCourses()
  } finally {
    joining.value = false
  }
}

function handleCreateClassroom() {
  if (canManageClassroom.value) {
    router.push({ path: '/teacher/courses', query: { create: '1' } })
    return
  }
  ElMessage.info('请先完成教师认证，认证通过后可创建教学课堂')
  router.push({ path: '/profile', query: { panel: 'teacher-certification' } })
}

function focusInvite() {
  inviteInputRef.value?.focus?.()
}

function isManagedCourse(course) {
  return course?.relationType === 'MANAGED'
}

function isLearningCourse(course) {
  return course?.relationType === 'LEARNING'
}

function enterClassroom(course) {
  if (isManagedCourse(course)) {
    router.push(`/teacher/courses/${course.id}/edit`)
    return
  }
  router.push(`/classrooms/${course.id}/learn`)
}

async function removeCourse(course) {
  await ElMessageBox.confirm(`确认删除课堂“${course.courseName}”吗？删除后该课堂建设与学习入口将不可用。`, '删除课堂', { type: 'warning' })
  await deleteCourse(course.id)
  ElMessage.success('课堂已删除')
  await loadCourses()
}

function courseRelationMeta(course) {
  if (isManagedCourse(course)) return { label: '我管理的', type: 'success' }
  if (isLearningCourse(course)) return { label: '我学习的', type: 'primary' }
  return { label: course.status === 1 ? '正在进行' : '已结束', type: course.status === 1 ? 'success' : 'info' }
}

function progressOf(course) {
  return Math.max(0, Math.min(100, Number(course.averageProgress || 0)))
}

function accentForCourse(course) {
  const colors = ['#177e89', '#2d6a4f', '#8a5a44', '#5a6f33', '#9c6644']
  return colors[Number(course.id || 0) % colors.length]
}

function coverStyle(course) {
  const image = course.coverUrl || resourceCore
  return {
    '--accent': accentForCourse(course),
    backgroundImage: `linear-gradient(135deg, rgba(14, 44, 51, 0.78), rgba(20, 80, 73, 0.28)), url(${image})`,
  }
}

function courseTags(course) {
  const configured = String(course.highlightTags || '').split(/[,，]/).map((item) => item.trim()).filter(Boolean)
  return (configured.length ? configured : [
    course.direction || '实验课程',
    course.semester || '未设置学期',
    course.status === 1 ? '可学习' : '暂不可用',
  ]).slice(0, 4)
}

function uniqueValues(values) {
  return [...new Set(values.filter(Boolean))]
}
</script>

<style scoped>
.course-list-page {
  max-width: 1240px;
  margin: 0 auto;
  height: calc(100vh - 92px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.classroom-upper {
  flex: 0 0 auto;
  background: #f5f7fa;
  position: sticky;
  top: 0;
  z-index: 5;
  padding-top: 4px;
}

.learning-hero {
  min-height: 220px;
  border-radius: 8px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  margin-bottom: 18px;
  overflow: hidden;
}

.learning-hero > div {
  max-width: 600px;
  padding: 28px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0));
}

.learning-hero p {
  color: #177e89;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 8px;
}

.learning-hero h1 {
  color: #13233a;
  font-size: 32px;
  line-height: 1.15;
  margin-bottom: 10px;
}

.learning-hero span {
  color: #344054;
  line-height: 1.7;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 14px;
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
  grid-template-columns: repeat(3, 76px);
  gap: 8px;
  flex-shrink: 0;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.classroom-nav {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  border-bottom: 1px solid #e7ebf0;
  padding-bottom: 8px;
  margin-bottom: 8px;
}

.classroom-tabs {
  display: flex;
  gap: 24px;
  overflow-x: auto;
}

.classroom-tabs button {
  border: 0;
  background: transparent;
  color: #344054;
  padding: 8px 0;
  font-size: 15px;
  cursor: pointer;
  border-bottom: 3px solid transparent;
}

.classroom-tabs button.active {
  color: #13233a;
  border-bottom-color: #13233a;
  font-weight: 700;
}

.action-strip {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
  gap: 10px;
  margin-bottom: 10px;
}

.create-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 10px 14px;
}

.create-panel strong {
  display: block;
  color: #13233a;
  margin-bottom: 4px;
}

.create-panel span {
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.head-stat {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 7px 8px;
  text-align: center;
}

.head-stat strong {
  display: block;
  color: #13233a;
  font-size: 19px;
  line-height: 1;
  margin-bottom: 4px;
}

.head-stat span {
  color: #7b8794;
  font-size: 12px;
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(260px, 420px) 150px 150px auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 9px 12px;
}

.join-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 0;
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 10px 14px;
}

.join-panel strong {
  display: block;
  color: #13233a;
  margin-bottom: 4px;
}

.join-panel span {
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.join-actions {
  display: flex;
  gap: 10px;
  flex: 0 0 330px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: stretch;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  padding-bottom: 20px;
}

.course-panel {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
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
  min-height: 118px;
  padding: 16px;
  background-size: cover;
  background-position: center;
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
  min-height: 42px;
  color: #667085;
  line-height: 1.55;
  margin-bottom: 8px;
}

.course-tagline {
  color: #177e89;
  font-size: 13px;
  line-height: 1.5;
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
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
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
  .learning-hero > div {
    padding: 22px;
    background: rgba(255, 255, 255, 0.88);
  }

  .page-head {
    align-items: stretch;
    flex-direction: column;
  }

  .classroom-nav,
  .action-strip {
    grid-template-columns: 1fr;
  }

  .head-stats,
  .side-panel,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    grid-template-columns: 1fr;
  }

  .join-panel,
  .join-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .join-actions {
    flex-basis: auto;
  }

  .status-tabs {
    overflow-x: auto;
  }
}
</style>
