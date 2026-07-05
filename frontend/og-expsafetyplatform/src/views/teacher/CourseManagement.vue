<template>
  <div class="teacher-course-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Course Management</p>
        <h1>课程管理</h1>
        <p class="page-desc">维护本人负责的实验课程，管理课程基础信息、开放状态和关联实验概览。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增课程</el-button>
    </section>

    <section class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <div class="metric-icon" :style="{ color: item.color, background: item.bg }">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="toolbar">
      <el-input
        v-model="filters.keyword"
        :prefix-icon="Search"
        clearable
        placeholder="搜索课程名称或编号"
      />
      <el-select v-model="filters.direction" clearable placeholder="专业方向">
        <el-option v-for="item in directionOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="filters.semester" clearable placeholder="开设学期">
        <el-option v-for="item in semesterOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态">
        <el-option label="开放" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
    </section>

    <section class="table-card">
      <el-table :data="courses" v-loading="loading" stripe>
        <el-table-column prop="courseCode" label="课程编号" width="130" />
        <el-table-column prop="courseName" label="课程名称" min-width="180" />
        <el-table-column label="专业方向" min-width="130">
          <template #default="{ row }">{{ row.direction || '-' }}</template>
        </el-table-column>
        <el-table-column label="学期" min-width="120">
          <template #default="{ row }">{{ row.semester || '-' }}</template>
        </el-table-column>
        <el-table-column label="实验/资源" width="120">
          <template #default="{ row }">{{ row.experimentCount || 0 }} / {{ row.resourceCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="平均进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="progressOf(row)" :stroke-width="7" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '开放' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button text :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '开放' }}
            </el-button>
            <el-button text type="danger" :icon="Delete" @click="removeCourse(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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
    </section>

    <el-dialog v-model="formVisible" :title="editingCourse ? '编辑课程' : '新增课程'" width="620px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="课程名称" required>
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程编号" required>
          <el-input v-model="form.courseCode" placeholder="请输入唯一课程编号" />
        </el-form-item>
        <el-form-item label="专业方向">
          <el-input v-model="form.direction" placeholder="如 油气储运、石油工程" />
        </el-form-item>
        <el-form-item label="开设学期">
          <el-input v-model="form.semester" placeholder="如 2026 秋季学期" />
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="form.coverUrl" placeholder="可选，课程封面图片 URL" />
        </el-form-item>
        <el-form-item label="显示顺序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="开放" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="课程说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入课程简介、教学目标或实验要求"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="课程详情" width="760px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="courseDetail">
          <div class="detail-head">
            <div>
              <h2>{{ courseDetail.course?.courseName }}</h2>
              <span>{{ courseDetail.course?.courseCode }}</span>
            </div>
            <el-tag :type="courseDetail.course?.status === 1 ? 'success' : 'info'">
              {{ courseDetail.course?.status === 1 ? '开放' : '停用' }}
            </el-tag>
          </div>
          <p class="detail-desc">{{ courseDetail.course?.description || '暂无课程说明' }}</p>
          <div class="summary-grid">
            <div>
              <strong>{{ courseDetail.experimentCount || 0 }}</strong>
              <span>实验项目</span>
            </div>
            <div>
              <strong>{{ courseDetail.resourceCount || 0 }}</strong>
              <span>教学资源</span>
            </div>
            <div>
              <strong>{{ Math.round(Number(courseDetail.averageProgress || 0)) }}%</strong>
              <span>平均进度</span>
            </div>
          </div>
          <el-table :data="courseDetail.experiments || []" stripe>
            <el-table-column prop="expCode" label="实验编号" width="130" />
            <el-table-column prop="expName" label="实验名称" min-width="180" />
            <el-table-column prop="riskLevel" label="风险等级" width="110" />
            <el-table-column prop="durationMinutes" label="时长(分钟)" width="110" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '开放' : '停用' }}
                </el-tag>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Collection,
  Delete,
  Edit,
  Monitor,
  Plus,
  Reading,
  Search,
  TrendCharts,
  View,
} from '@element-plus/icons-vue'
import {
  createCourse,
  deleteCourse,
  getCourseDetail,
  getCourses,
  updateCourse,
  updateCourseStatus,
} from '@/api/course'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const editingCourse = ref(null)
const courseDetail = ref(null)
const courses = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
let keywordTimer = null

const filters = reactive({
  keyword: '',
  direction: '',
  semester: '',
  status: '',
})

const form = reactive({
  courseName: '',
  courseCode: '',
  direction: '',
  coverUrl: '',
  description: '',
  semester: '',
  sort: 0,
  enabled: true,
})

const directionOptions = computed(() => uniqueValues(courses.value.map((item) => item.direction)))
const semesterOptions = computed(() => uniqueValues(courses.value.map((item) => item.semester)))

const metrics = computed(() => [
  { label: '课程总数', value: total.value, icon: Reading, color: '#409eff', bg: '#ecf5ff' },
  { label: '开放课程', value: courses.value.filter((item) => item.status === 1).length, icon: Monitor, color: '#67c23a', bg: '#f0f9eb' },
  { label: '实验项目', value: courses.value.reduce((sum, item) => sum + Number(item.experimentCount || 0), 0), icon: Collection, color: '#e6a23c', bg: '#fdf6ec' },
  { label: '平均进度', value: `${pageAverageProgress.value}%`, icon: TrendCharts, color: '#f56c6c', bg: '#fef0f0' },
])

const pageAverageProgress = computed(() => {
  if (courses.value.length === 0) return 0
  const totalProgress = courses.value.reduce((sum, item) => sum + Number(item.averageProgress || 0), 0)
  return Math.round(totalProgress / courses.value.length)
})

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

function openCreate() {
  editingCourse.value = null
  resetForm()
  formVisible.value = true
}

async function openEdit(course) {
  try {
    const detail = await getCourseDetail(course.id)
    const fullCourse = detail?.course || course
    editingCourse.value = fullCourse
    Object.assign(form, {
      courseName: fullCourse.courseName || '',
      courseCode: fullCourse.courseCode || '',
      direction: fullCourse.direction || '',
      coverUrl: fullCourse.coverUrl || '',
      description: fullCourse.description || '',
      semester: fullCourse.semester || '',
      sort: fullCourse.sort || 0,
      enabled: fullCourse.status === 1,
    })
    formVisible.value = true
  } catch {
    ElMessage.warning('课程详情暂时无法加载')
  }
}

async function openDetail(course) {
  detailVisible.value = true
  detailLoading.value = true
  courseDetail.value = null
  try {
    courseDetail.value = await getCourseDetail(course.id)
  } finally {
    detailLoading.value = false
  }
}

async function saveCourse() {
  if (!form.courseName.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  if (!form.courseCode.trim()) {
    ElMessage.warning('请输入课程编号')
    return
  }
  const teacherId = Number(authStore.userInfo?.id)
  if (!teacherId) {
    ElMessage.error('当前账号缺少教师ID，请重新登录')
    return
  }

  const payload = {
    courseName: form.courseName.trim(),
    courseCode: form.courseCode.trim(),
    direction: form.direction || undefined,
    teacherId,
    coverUrl: form.coverUrl || undefined,
    description: form.description || undefined,
    semester: form.semester || undefined,
    status: form.enabled ? 1 : 0,
    sort: Number(form.sort || 0),
  }

  saving.value = true
  try {
    if (editingCourse.value) {
      await updateCourse(editingCourse.value.id, payload)
      ElMessage.success('课程已更新')
    } else {
      await createCourse(payload)
      ElMessage.success('课程已创建')
    }
    formVisible.value = false
    await loadCourses()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(course) {
  const nextStatus = course.status === 1 ? 0 : 1
  await updateCourseStatus(course.id, nextStatus)
  ElMessage.success(nextStatus === 1 ? '课程已开放' : '课程已停用')
  await loadCourses()
}

async function removeCourse(course) {
  try {
    await ElMessageBox.confirm(`确认删除课程「${course.courseName}」吗？`, '删除课程', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteCourse(course.id)
    ElMessage.success('课程已删除')
    await loadCourses()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error
    }
  }
}

function resetForm() {
  Object.assign(form, {
    courseName: '',
    courseCode: '',
    direction: '',
    coverUrl: '',
    description: '',
    semester: '',
    sort: 0,
    enabled: true,
  })
}

function progressOf(course) {
  return Math.max(0, Math.min(100, Math.round(Number(course.averageProgress || 0))))
}

function uniqueValues(values) {
  return [...new Set(values.filter(Boolean))]
}
</script>

<style scoped>
.teacher-course-page {
  max-width: 1240px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
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

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.metric-card,
.toolbar,
.table-card {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
}

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.metric-card strong {
  display: block;
  color: #13233a;
  font-size: 24px;
  line-height: 1;
  margin-bottom: 6px;
}

.metric-card span {
  color: #7b8794;
  font-size: 13px;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 150px 150px 130px;
  gap: 12px;
  padding: 14px;
  margin-bottom: 16px;
}

.table-card {
  padding: 14px;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #667085;
  padding-top: 14px;
}

.detail-body {
  min-height: 180px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-head h2 {
  color: #13233a;
  font-size: 20px;
  line-height: 1.3;
  margin-bottom: 4px;
}

.detail-head span,
.detail-desc {
  color: #667085;
}

.detail-desc {
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

:deep(.el-progress-bar__outer) {
  background: #e7edf3 !important;
}

@media (max-width: 980px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 620px) {
  .page-head {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid,
  .toolbar,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
