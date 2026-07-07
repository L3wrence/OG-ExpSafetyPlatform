<template>
  <div class="teacher-course-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Course Organization</p>
        <h1>实验课程与教学组织</h1>
        <p class="page-desc">按实验课程、实验项目和学习任务组织教学，维护教学班、学生名单与完成进度。</p>
      </div>
      <el-button v-permission="'course:create'" type="primary" :icon="Plus" @click="openCreate">新增课程</el-button>
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
      <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索课程名称或编号" />
      <el-select v-model="filters.direction" clearable placeholder="课程方向">
        <el-option v-for="item in directionOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="filters.semester" clearable placeholder="开设学期">
        <el-option v-for="item in semesterOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已归档" :value="2" />
      </el-select>
    </section>

    <section class="table-card">
      <el-table :data="courses" v-loading="loading" stripe empty-text="暂无课程">
        <el-table-column prop="courseCode" label="课程编号" width="130" />
        <el-table-column prop="courseName" label="课程名称" min-width="190" />
        <el-table-column label="方向/学期" min-width="150">
          <template #default="{ row }">
            <div class="two-line">
              <strong>{{ row.direction || '-' }}</strong>
              <span>{{ row.semester || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="学分/学时" width="110">
          <template #default="{ row }">{{ row.credit || 0 }} / {{ row.hours || 0 }}</template>
        </el-table-column>
        <el-table-column label="班级/学生" width="120">
          <template #default="{ row }">{{ row.teachingClassCount || 0 }} / {{ row.studentCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="实验/资源" width="120">
          <template #default="{ row }">{{ row.experimentCount || 0 }} / {{ row.resourceCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="完成率" width="150">
          <template #default="{ row }">
            <el-progress :percentage="progressOf(row)" :stroke-width="7" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="390" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button v-permission="'course:update'" text type="primary" :icon="Edit" :disabled="row.status === 2" @click="openEditor(row)">建设</el-button>
            <el-button v-permission="'course:update'" text type="primary" :icon="Edit" :disabled="row.status === 2" @click="openEdit(row)">基础</el-button>
            <el-button v-permission="'course:publish'" text type="success" :icon="Finished" :disabled="row.status === 1 || row.status === 2" @click="publish(row)">发布</el-button>
            <el-button v-permission="'course:archive'" text type="warning" :icon="FolderChecked" :disabled="row.status === 2" @click="archive(row)">归档</el-button>
            <el-button v-permission="'course:delete'" text type="danger" :icon="Delete" :disabled="row.status === 2" @click="removeCourse(row)">删除</el-button>
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

    <el-dialog v-model="formVisible" :title="editingCourse ? '编辑课程' : '新增课程'" width="720px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="104px">
        <div class="form-grid">
          <el-form-item label="课程名称" prop="courseName">
            <el-input v-model="form.courseName" placeholder="请输入课程名称" />
          </el-form-item>
          <el-form-item label="课程编号" prop="courseCode">
            <el-input v-model="form.courseCode" placeholder="请输入唯一课程编号" />
          </el-form-item>
          <el-form-item label="课程方向">
            <el-input v-model="form.direction" placeholder="如 油气储运、石油工程" />
          </el-form-item>
          <el-form-item label="开设学期">
            <el-input v-model="form.semester" placeholder="如 2026 秋季学期" />
          </el-form-item>
          <el-form-item label="学分">
            <el-input-number v-model="form.credit" :min="0" :max="20" :precision="1" />
          </el-form-item>
          <el-form-item label="学时">
            <el-input-number v-model="form.hours" :min="0" :max="300" />
          </el-form-item>
          <el-form-item label="负责人ID" prop="teacherId">
            <el-input-number v-model="form.teacherId" :min="1" />
          </el-form-item>
          <el-form-item label="显示顺序">
            <el-input-number v-model="form.sort" :min="0" :max="9999" />
          </el-form-item>
        </div>
        <el-form-item label="状态">
          <el-segmented v-model="form.status" :options="statusOptions" />
        </el-form-item>
        <el-form-item label="允许空发布">
          <el-switch v-model="form.allowEmptyPublish" active-text="允许" inactive-text="不允许" />
        </el-form-item>
        <el-form-item label="考核方式">
          <el-input v-model="form.assessmentMethod" placeholder="如 必学资源 30% + 安全考试 30% + 报告 40%" />
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="form.coverUrl" placeholder="可选，课程封面图片 URL" />
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入课程简介和教学目标" />
        </el-form-item>
        <el-form-item label="学习要求">
          <el-input v-model="form.learningRequirement" type="textarea" :rows="3" placeholder="请输入预习、实验纪律、报告提交等要求" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="课程详情" width="1040px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="courseDetail">
          <div class="detail-head">
            <div>
              <h2>{{ activeCourse?.courseName }}</h2>
              <span>{{ activeCourse?.courseCode }} · {{ activeCourse?.semester || '未设置学期' }}</span>
            </div>
            <el-tag :type="statusMeta(activeCourse?.status).type">{{ statusMeta(activeCourse?.status).label }}</el-tag>
          </div>
          <p class="detail-desc">{{ activeCourse?.description || '暂无课程简介' }}</p>
          <div class="summary-grid">
            <div>
              <strong>{{ courseDetail.experimentCount || 0 }}</strong>
              <span>实验项目</span>
            </div>
            <div>
              <strong>{{ courseDetail.studentCount || 0 }}</strong>
              <span>学生人数</span>
            </div>
            <div>
              <strong>{{ Math.round(Number(courseDetail.averageProgress || 0)) }}%</strong>
              <span>整体完成率</span>
            </div>
            <div>
              <strong>{{ courseDetail.teachingClassCount || 0 }}</strong>
              <span>教学班</span>
            </div>
          </div>

          <el-tabs v-model="detailTab">
            <el-tab-pane label="实验项目" name="experiments">
              <el-empty v-if="(courseDetail.experiments || []).length === 0" description="暂无实验项目" />
              <el-table v-else :data="courseDetail.experiments" stripe>
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
            </el-tab-pane>

            <el-tab-pane label="教学班" name="classes">
              <div class="tab-actions">
                <el-button v-permission="'course:class:manage'" type="primary" :icon="Plus" :disabled="isArchived" @click="openClassCreate">新增教学班</el-button>
              </div>
              <el-table :data="classes" stripe empty-text="暂无教学班">
                <el-table-column prop="className" label="教学班" min-width="160" />
                <el-table-column prop="teacherName" label="任课教师" width="120" />
                <el-table-column prop="assistantName" label="助教" width="120" />
                <el-table-column prop="adminClass" label="行政班" min-width="140" />
                <el-table-column prop="studentCount" label="学生数" width="90" />
                <el-table-column label="状态" width="90">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="160">
                  <template #default="{ row }">
                    <el-button v-permission="'course:class:manage'" text type="primary" :icon="Edit" :disabled="isArchived" @click="openClassEdit(row)">编辑</el-button>
                    <el-button v-permission="'course:class:manage'" text type="danger" :icon="Delete" :disabled="isArchived" @click="removeClass(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="学生名单" name="students">
              <div class="tab-actions student-actions">
                <el-input v-model="studentFilters.keyword" :prefix-icon="Search" clearable placeholder="搜索学号、姓名或行政班" />
                <el-select v-model="studentFilters.teachingClassId" clearable placeholder="教学班">
                  <el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" />
                </el-select>
                <el-input v-model="studentFilters.groupName" clearable placeholder="小组" />
                <el-button :icon="Search" @click="loadStudents">查询</el-button>
                <el-button v-permission="'course:student:manage'" type="primary" :icon="Upload" :disabled="isArchived" @click="openImport">导入名单</el-button>
                <el-button v-permission="'course:student:manage'" type="danger" :icon="Delete" :disabled="isArchived || selectedStudents.length === 0" @click="removeSelectedStudents">移出</el-button>
              </div>
              <el-table :data="students" stripe empty-text="暂无学生" @selection-change="selectedStudents = $event">
                <el-table-column type="selection" width="44" />
                <el-table-column prop="username" label="学号" width="130" />
                <el-table-column prop="realName" label="姓名" width="110" />
                <el-table-column prop="className" label="行政班" min-width="140" />
                <el-table-column prop="teachingClassName" label="教学班" min-width="140" />
                <el-table-column prop="groupName" label="小组" width="110" />
                <el-table-column prop="phone" label="联系方式" width="140" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="要求与公告" name="requirements">
              <div class="requirement-block">
                <h3>学习要求</h3>
                <p>{{ courseDetail.learningRequirement || '暂无学习要求' }}</p>
              </div>
              <div class="requirement-block">
                <h3>考核方式</h3>
                <p>{{ activeCourse?.assessmentMethod || '暂无考核方式' }}</p>
              </div>
              <el-empty v-if="(courseDetail.announcements || []).length === 0" description="暂无公告" />
              <el-timeline v-else>
                <el-timeline-item v-for="item in courseDetail.announcements" :key="item">{{ item }}</el-timeline-item>
              </el-timeline>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="classVisible" :title="editingClass ? '编辑教学班' : '新增教学班'" width="560px">
      <el-form ref="classFormRef" :model="classForm" :rules="classRules" label-width="96px">
        <el-form-item label="教学班" prop="className">
          <el-input v-model="classForm.className" placeholder="如 油气工程实验 1 班" />
        </el-form-item>
        <el-form-item label="任课教师ID" prop="teacherId">
          <el-input-number v-model="classForm.teacherId" :min="1" />
        </el-form-item>
        <el-form-item label="助教ID">
          <el-input-number v-model="classForm.assistantId" :min="1" />
        </el-form-item>
        <el-form-item label="行政班">
          <el-input v-model="classForm.adminClass" placeholder="可填写多个行政班，用逗号分隔" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="classForm.semester" placeholder="默认继承课程学期" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="classForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classVisible = false">取消</el-button>
        <el-button type="primary" :loading="classSaving" @click="saveClass">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="批量导入学生" width="720px">
      <el-form label-width="96px">
        <el-form-item label="教学班">
          <el-select v-model="importForm.teachingClassId" clearable placeholder="不指定教学班">
            <el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认小组">
          <el-input v-model="importForm.defaultGroupName" placeholder="行内未填写小组时使用" />
        </el-form-item>
        <el-form-item label="学生名单">
          <el-input
            v-model="importForm.text"
            type="textarea"
            :rows="8"
            placeholder="每行一名学生：学号,姓名,专业,行政班,小组,手机号"
          />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="importResult"
        :title="`导入完成：成功 ${importResult.successCount || 0} 条，失败 ${importResult.failCount || 0} 条`"
        type="info"
        show-icon
        :closable="false"
      />
      <el-table v-if="(importResult?.failures || []).length" :data="importResult.failures" size="small" class="failure-table">
        <el-table-column prop="rowIndex" label="行号" width="80" />
        <el-table-column prop="username" label="学号" width="140" />
        <el-table-column prop="reason" label="失败原因" />
      </el-table>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Collection,
  Delete,
  Edit,
  Finished,
  FolderChecked,
  Monitor,
  Plus,
  Reading,
  Search,
  TrendCharts,
  Upload,
  View,
} from '@element-plus/icons-vue'
import {
  archiveCourse,
  createCourse,
  createCourseClass,
  deleteCourse,
  deleteCourseClass,
  getCourseDetail,
  getCourseStudents,
  getCourses,
  importCourseStudents,
  publishCourse,
  removeCourseStudents,
  updateCourse,
  updateCourseClass,
} from '@/api/course'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const classSaving = ref(false)
const importing = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const classVisible = ref(false)
const importVisible = ref(false)
const editingCourse = ref(null)
const editingClass = ref(null)
const courseDetail = ref(null)
const courses = ref([])
const classes = ref([])
const students = ref([])
const selectedStudents = ref([])
const importResult = ref(null)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailTab = ref('experiments')
const formRef = ref()
const classFormRef = ref()
let keywordTimer = null

const filters = reactive({ keyword: '', direction: '', semester: '', status: '' })
const studentFilters = reactive({ keyword: '', teachingClassId: '', groupName: '' })

const form = reactive({
  courseName: '',
  courseCode: '',
  direction: '',
  teacherId: null,
  coverUrl: '',
  description: '',
  semester: '',
  status: 0,
  sort: 0,
  credit: 0,
  hours: 0,
  assessmentMethod: '',
  learningRequirement: '',
  allowEmptyPublish: false,
})

const classForm = reactive({
  className: '',
  teacherId: null,
  assistantId: null,
  adminClass: '',
  semester: '',
  enabled: true,
})

const importForm = reactive({
  teachingClassId: '',
  defaultGroupName: '',
  text: '',
})

const formRules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseCode: [{ required: true, message: '请输入课程编号', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请输入负责人ID', trigger: 'change' }],
}

const classRules = {
  className: [{ required: true, message: '请输入教学班名称', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请输入任课教师ID', trigger: 'change' }],
}

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '发布', value: 1 },
]

const activeCourse = computed(() => courseDetail.value?.course || editingCourse.value)
const isArchived = computed(() => activeCourse.value?.status === 2)
const directionOptions = computed(() => uniqueValues(courses.value.map((item) => item.direction)))
const semesterOptions = computed(() => uniqueValues(courses.value.map((item) => item.semester)))

const metrics = computed(() => [
  { label: '课程总数', value: total.value, icon: Reading, color: '#1f6feb', bg: '#edf5ff' },
  { label: '已发布', value: courses.value.filter((item) => item.status === 1).length, icon: Monitor, color: '#2f9e44', bg: '#ecf9f0' },
  { label: '教学班', value: courses.value.reduce((sum, item) => sum + Number(item.teachingClassCount || 0), 0), icon: Collection, color: '#b7791f', bg: '#fff7e6' },
  { label: '平均完成率', value: `${pageAverageProgress.value}%`, icon: TrendCharts, color: '#c2410c', bg: '#fff1e8' },
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
  const detail = await getCourseDetail(course.id)
  const fullCourse = detail?.course || course
  editingCourse.value = fullCourse
  Object.assign(form, {
    courseName: fullCourse.courseName || '',
    courseCode: fullCourse.courseCode || '',
    direction: fullCourse.direction || '',
    teacherId: fullCourse.teacherId || currentUserId(),
    coverUrl: fullCourse.coverUrl || '',
    description: fullCourse.description || '',
    semester: fullCourse.semester || '',
    status: fullCourse.status === 1 ? 1 : 0,
    sort: fullCourse.sort || 0,
    credit: Number(fullCourse.credit || 0),
    hours: Number(fullCourse.hours || 0),
    assessmentMethod: fullCourse.assessmentMethod || '',
    learningRequirement: fullCourse.learningRequirement || '',
    allowEmptyPublish: fullCourse.allowEmptyPublish === 1,
  })
  formVisible.value = true
}

function openEditor(course) {
  router.push(`/teacher/courses/${course.id}/edit`)
}

async function openDetail(course) {
  detailVisible.value = true
  detailLoading.value = true
  detailTab.value = 'experiments'
  courseDetail.value = null
  students.value = []
  selectedStudents.value = []
  try {
    courseDetail.value = await getCourseDetail(course.id)
    classes.value = courseDetail.value?.teachingClasses || []
    students.value = courseDetail.value?.students || []
  } finally {
    detailLoading.value = false
  }
}

async function saveCourse() {
  await formRef.value?.validate()
  const payload = {
    courseName: form.courseName.trim(),
    courseCode: form.courseCode.trim(),
    direction: form.direction || undefined,
    teacherId: Number(form.teacherId),
    coverUrl: form.coverUrl || undefined,
    description: form.description || undefined,
    semester: form.semester || undefined,
    status: form.status,
    sort: Number(form.sort || 0),
    credit: Number(form.credit || 0),
    hours: Number(form.hours || 0),
    assessmentMethod: form.assessmentMethod || undefined,
    learningRequirement: form.learningRequirement || undefined,
    allowEmptyPublish: form.allowEmptyPublish ? 1 : 0,
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

async function publish(course) {
  try {
    await ElMessageBox.confirm(`确认发布课程「${course.courseName}」吗？`, '发布课程', {
      type: 'info',
      confirmButtonText: '发布',
      cancelButtonText: '取消',
    })
    await publishCourse(course.id, course.allowEmptyPublish === 1)
    ElMessage.success('课程已发布')
    await loadCourses()
  } catch (error) {
    ignoreCancel(error)
  }
}

async function archive(course) {
  try {
    await ElMessageBox.confirm(`归档后课程原则上只读，确认归档「${course.courseName}」吗？`, '归档课程', {
      type: 'warning',
      confirmButtonText: '归档',
      cancelButtonText: '取消',
    })
    await archiveCourse(course.id)
    ElMessage.success('课程已归档')
    await loadCourses()
  } catch (error) {
    ignoreCancel(error)
  }
}

async function removeCourse(course) {
  try {
    await ElMessageBox.confirm(`确认删除课程「${course.courseName}」吗？有关联业务时请使用归档。`, '删除课程', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteCourse(course.id)
    ElMessage.success('课程已删除')
    await loadCourses()
  } catch (error) {
    ignoreCancel(error)
  }
}

function openClassCreate() {
  editingClass.value = null
  resetClassForm()
  classVisible.value = true
}

function openClassEdit(row) {
  editingClass.value = row
  Object.assign(classForm, {
    className: row.className || '',
    teacherId: row.teacherId || currentUserId(),
    assistantId: row.assistantId || null,
    adminClass: row.adminClass || '',
    semester: row.semester || '',
    enabled: row.status === 1,
  })
  classVisible.value = true
}

async function saveClass() {
  await classFormRef.value?.validate()
  const payload = {
    className: classForm.className.trim(),
    teacherId: Number(classForm.teacherId),
    assistantId: classForm.assistantId ? Number(classForm.assistantId) : undefined,
    adminClass: classForm.adminClass || undefined,
    semester: classForm.semester || undefined,
    status: classForm.enabled ? 1 : 0,
  }
  classSaving.value = true
  try {
    if (editingClass.value) {
      await updateCourseClass(activeCourse.value.id, editingClass.value.id, payload)
      ElMessage.success('教学班已更新')
    } else {
      await createCourseClass(activeCourse.value.id, payload)
      ElMessage.success('教学班已创建')
    }
    classVisible.value = false
    await refreshDetail()
  } finally {
    classSaving.value = false
  }
}

async function removeClass(row) {
  try {
    await ElMessageBox.confirm(`确认删除教学班「${row.className}」吗？`, '删除教学班', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteCourseClass(activeCourse.value.id, row.id)
    ElMessage.success('教学班已删除')
    await refreshDetail()
  } catch (error) {
    ignoreCancel(error)
  }
}

async function loadStudents() {
  if (!activeCourse.value?.id) return
  students.value = await getCourseStudents(activeCourse.value.id, {
    keyword: studentFilters.keyword || undefined,
    teachingClassId: studentFilters.teachingClassId || undefined,
    groupName: studentFilters.groupName || undefined,
  })
}

function openImport() {
  importResult.value = null
  Object.assign(importForm, { teachingClassId: '', defaultGroupName: '', text: '' })
  importVisible.value = true
}

async function submitImport() {
  const studentsToImport = parseImportRows(importForm.text)
  if (studentsToImport.length === 0) {
    ElMessage.warning('请填写学生名单')
    return
  }
  importing.value = true
  try {
    importResult.value = await importCourseStudents(activeCourse.value.id, {
      teachingClassId: importForm.teachingClassId || undefined,
      defaultGroupName: importForm.defaultGroupName || undefined,
      students: studentsToImport,
    })
    await refreshDetail()
  } finally {
    importing.value = false
  }
}

async function removeSelectedStudents() {
  try {
    await ElMessageBox.confirm(`确认将选中的 ${selectedStudents.value.length} 名学生移出课程吗？`, '移出学生', {
      type: 'warning',
      confirmButtonText: '移出',
      cancelButtonText: '取消',
    })
    await removeCourseStudents(activeCourse.value.id, selectedStudents.value.map((item) => item.studentId))
    ElMessage.success('学生已移出')
    await refreshDetail()
  } catch (error) {
    ignoreCancel(error)
  }
}

async function refreshDetail() {
  if (!activeCourse.value?.id) return
  courseDetail.value = await getCourseDetail(activeCourse.value.id)
  classes.value = courseDetail.value?.teachingClasses || []
  students.value = courseDetail.value?.students || []
  selectedStudents.value = []
  await loadCourses()
}

function resetForm() {
  Object.assign(form, {
    courseName: '',
    courseCode: '',
    direction: '',
    teacherId: currentUserId(),
    coverUrl: '',
    description: '',
    semester: '',
    status: 0,
    sort: 0,
    credit: 0,
    hours: 0,
    assessmentMethod: '',
    learningRequirement: '',
    allowEmptyPublish: false,
  })
}

function resetClassForm() {
  Object.assign(classForm, {
    className: '',
    teacherId: currentUserId(),
    assistantId: null,
    adminClass: '',
    semester: activeCourse.value?.semester || '',
    enabled: true,
  })
}

function parseImportRows(text) {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const parts = line.split(/[,，\t]/).map((part) => part.trim())
      return {
        username: parts[0],
        realName: parts[1],
        major: parts[2],
        className: parts[3],
        groupName: parts[4],
        phone: parts[5],
      }
    })
}

function progressOf(course) {
  return Math.max(0, Math.min(100, Math.round(Number(course.averageProgress || 0))))
}

function statusMeta(status) {
  if (status === 1) return { label: '已发布', type: 'success' }
  if (status === 2) return { label: '已归档', type: 'warning' }
  return { label: '草稿', type: 'info' }
}

function uniqueValues(values) {
  return [...new Set(values.filter(Boolean))]
}

function currentUserId() {
  return Number(authStore.userInfo?.id || 0) || null
}

function ignoreCancel(error) {
  if (error !== 'cancel' && error !== 'close') {
    throw error
  }
}
</script>

<style scoped>
.teacher-course-page {
  max-width: 1280px;
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
  letter-spacing: 0;
  text-transform: uppercase;
  margin-bottom: 6px;
}

.page-head h1 {
  color: #13233a;
  font-size: 26px;
  line-height: 1.2;
  margin-bottom: 8px;
}

.page-desc,
.detail-desc {
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

.metric-card span,
.two-line span {
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

.two-line {
  display: grid;
  gap: 4px;
}

.two-line strong {
  color: #24364b;
  font-weight: 600;
}

.pagination-row,
.tab-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pagination-row {
  color: #667085;
  padding-top: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 12px;
}

.detail-body {
  min-height: 220px;
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

.detail-head span {
  color: #667085;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin: 14px 0;
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

.tab-actions {
  justify-content: flex-end;
  margin-bottom: 12px;
}

.student-actions {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 160px 130px auto auto auto;
}

.requirement-block {
  border-bottom: 1px solid #edf1f5;
  padding: 0 0 14px;
  margin-bottom: 14px;
}

.requirement-block h3 {
  color: #13233a;
  font-size: 15px;
  margin-bottom: 8px;
}

.requirement-block p {
  color: #4b5b6b;
  line-height: 1.7;
  white-space: pre-wrap;
}

.failure-table {
  margin-top: 12px;
}

:deep(.el-progress-bar__outer) {
  background: #e7edf3 !important;
}

@media (max-width: 980px) {
  .metric-grid,
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar,
  .student-actions,
  .form-grid {
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
  .summary-grid,
  .student-actions,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
