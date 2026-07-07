<template>
  <div class="course-editor-page" v-loading="loading">
    <section class="page-head">
      <div>
        <p class="eyebrow">Course Builder</p>
        <h1>{{ course?.courseName || '课程建设' }}</h1>
        <p class="page-desc">{{ course?.courseCode || '-' }} · {{ course?.semester || '未设置学期' }}</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="loadDetail">刷新</el-button>
        <el-button :icon="Back" @click="router.push('/teacher/courses')">返回课程教学</el-button>
      </div>
    </section>

    <section class="builder-grid">
      <aside class="step-panel">
        <button v-for="item in steps" :key="item.key" :class="{ active: activeStep === item.key }" type="button" @click="activeStep = item.key">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </aside>

      <main class="editor-panel">
        <section class="summary-band">
          <div><strong>{{ detail?.experimentCount || 0 }}</strong><span>实验章节</span></div>
          <div><strong>{{ detail?.resourceCount || 0 }}</strong><span>学习资源</span></div>
          <div><strong>{{ detail?.studentCount || 0 }}</strong><span>学生</span></div>
          <div><strong>{{ Math.round(Number(detail?.averageProgress || 0)) }}%</strong><span>完成率</span></div>
        </section>

        <section v-if="activeStep === 'basic'" class="work-section">
          <div class="section-title">
            <h2>课程基本信息</h2>
            <div class="inline-actions">
              <el-button v-if="course?.status !== 1" type="success" :icon="Finished" :disabled="isArchived" @click="publishCurrentCourse">发布</el-button>
              <el-button type="warning" :disabled="isArchived" @click="archiveCurrentCourse">归档</el-button>
            </div>
          </div>
          <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="104px">
            <div class="form-grid">
              <el-form-item label="课程名称" prop="courseName"><el-input v-model="courseForm.courseName" :disabled="isArchived" /></el-form-item>
              <el-form-item label="课程编号" prop="courseCode"><el-input v-model="courseForm.courseCode" :disabled="isArchived" /></el-form-item>
              <el-form-item label="课程方向"><el-input v-model="courseForm.direction" :disabled="isArchived" /></el-form-item>
              <el-form-item label="开设学期"><el-input v-model="courseForm.semester" :disabled="isArchived" /></el-form-item>
              <el-form-item label="负责人ID" prop="teacherId"><el-input-number v-model="courseForm.teacherId" :min="1" :disabled="isArchived" /></el-form-item>
              <el-form-item label="状态"><el-tag :type="statusMeta(course?.status).type">{{ statusMeta(course?.status).label }}</el-tag></el-form-item>
              <el-form-item label="学分"><el-input-number v-model="courseForm.credit" :min="0" :max="20" :precision="1" :disabled="isArchived" /></el-form-item>
              <el-form-item label="学时"><el-input-number v-model="courseForm.hours" :min="0" :max="300" :disabled="isArchived" /></el-form-item>
            </div>
            <el-form-item label="考核方式"><el-input v-model="courseForm.assessmentMethod" :disabled="isArchived" /></el-form-item>
            <el-form-item label="封面地址"><el-input v-model="courseForm.coverUrl" :disabled="isArchived" placeholder="可选，留空使用 AmazingTeaching 默认封面" /></el-form-item>
            <el-form-item label="课程短标语"><el-input v-model="courseForm.tagline" :disabled="isArchived" maxlength="160" /></el-form-item>
            <el-form-item label="亮点标签"><el-input v-model="courseForm.highlightTags" :disabled="isArchived" placeholder="用逗号分隔，如 实验可视化,安全准入" /></el-form-item>
            <el-form-item label="视觉主题"><el-input v-model="courseForm.visualTheme" :disabled="isArchived" placeholder="如 oilfield-lab" /></el-form-item>
            <el-form-item label="课程简介"><el-input v-model="courseForm.description" type="textarea" :rows="3" :disabled="isArchived" /></el-form-item>
            <el-form-item label="学习要求"><el-input v-model="courseForm.learningRequirement" type="textarea" :rows="3" :disabled="isArchived" /></el-form-item>
          </el-form>
          <div class="course-visual-preview" :style="{ backgroundImage: `url(${courseVisual})` }">
            <div>
              <span>AmazingTeaching 学生视角</span>
              <strong>{{ courseForm.courseName || '课程名称' }}</strong>
              <p>{{ courseForm.tagline || '把油气工程实验拆成看得懂、能操作、可复盘的学习路径。' }}</p>
            </div>
          </div>
          <div class="footer-actions">
            <el-button type="primary" :icon="Check" :loading="savingCourse" :disabled="isArchived" @click="saveCourseInline">保存课程信息</el-button>
          </div>
        </section>

        <section v-if="activeStep === 'classes'" class="work-section">
          <div class="section-title">
            <h2>教学班和学生</h2>
            <el-button type="primary" :icon="Plus" :disabled="isArchived" @click="openClassCreate">新增教学班</el-button>
          </div>
          <el-table :data="classes" stripe empty-text="暂无教学班">
            <el-table-column prop="className" label="教学班" min-width="160" />
            <el-table-column prop="teacherName" label="任课教师" width="120" />
            <el-table-column prop="assistantName" label="助教" width="120" />
            <el-table-column prop="adminClass" label="行政班" min-width="140" />
            <el-table-column prop="studentCount" label="学生数" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button text type="primary" :icon="Edit" :disabled="isArchived" @click="openClassEdit(row)">编辑</el-button>
                <el-button text type="danger" :icon="Delete" :disabled="isArchived" @click="removeClass(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <section class="student-preview">
            <div class="section-title compact">
              <h3>学生名单预览</h3>
              <el-button text type="primary" @click="router.push('/teacher/courses')">批量名单维护</el-button>
            </div>
            <el-table :data="students.slice(0, 8)" size="small" stripe empty-text="暂无学生">
              <el-table-column prop="username" label="学号" width="130" />
              <el-table-column prop="realName" label="姓名" width="120" />
              <el-table-column prop="teachingClassName" label="教学班" min-width="150" />
              <el-table-column prop="groupName" label="小组" width="110" />
            </el-table>
          </section>
        </section>

        <section v-if="activeStep === 'experiments'" class="work-section">
          <div class="section-title">
            <h2>油气实验路径与考核闭环</h2>
            <el-button type="primary" :icon="Plus" :disabled="isArchived" @click="openExperimentCreate">新增实验章节</el-button>
          </div>
          <div class="readiness-band">
            <div>
              <strong>{{ pathReadyCount }}/{{ experiments.length }}</strong>
              <span>实验路径已具备教学与考核闭环</span>
            </div>
            <p>{{ courseReadinessText }}</p>
          </div>
          <div class="experiment-list">
            <article v-for="item in experiments" :key="item.id">
              <div>
                <strong>{{ item.expName }}</strong>
                <span>{{ item.expCode }} · {{ riskLabel(item.riskLevel) }} · {{ item.durationMinutes || 0 }} 分钟</span>
                <small>{{ experimentReadinessText(item) }}</small>
              </div>
              <div class="row-actions">
                <el-tag :type="item.status === 1 ? 'success' : 'info'">{{ item.status === 1 ? '开放' : '未开放' }}</el-tag>
                <el-button text type="primary" :icon="Edit" :disabled="isArchived" @click="openExperimentEdit(item)">编辑</el-button>
                <el-button text :type="item.status === 1 ? 'warning' : 'success'" :disabled="isArchived" @click="toggleExperiment(item)">
                  {{ item.status === 1 ? '停用' : '开放' }}
                </el-button>
              </div>
            </article>
            <el-empty v-if="experiments.length === 0" description="暂无实验章节" />
          </div>
          <div class="path-preview">
            <div class="section-title compact">
              <h3>学生视角路径预览</h3>
              <el-button text type="primary" :icon="View" @click="activeStep = 'resources'">检查资源与考试</el-button>
            </div>
            <div class="preview-steps">
              <span>预习资源</span>
              <span>风险认知</span>
              <span>安全准入</span>
              <span>实验预约</span>
              <span>实验报告</span>
              <span>成绩反馈</span>
            </div>
          </div>
        </section>

        <section v-if="activeStep === 'resources'" class="work-section">
          <h2>资源、准入考试和任务闭环</h2>
          <div class="requirement-block">
            <b>路径配置提醒</b>
            <p>为每个油气实验至少准备预习资源、风险与 PPE 信息、安全准入任务、预约规则和报告评分标准。</p>
          </div>
          <div class="action-grid">
            <button type="button" @click="router.push({ path: '/teacher/resources', query: { courseId } })"><el-icon><Folder /></el-icon><span>学习资源</span></button>
            <button type="button" @click="router.push({ path: '/teacher/experiments', query: { courseId } })"><el-icon><Operation /></el-icon><span>风险与准入</span></button>
            <button type="button" @click="router.push({ path: '/teacher/exam-papers', query: { courseId } })"><el-icon><EditPen /></el-icon><span>练习与考试</span></button>
          </div>
        </section>

        <section v-if="activeStep === 'reports'" class="work-section">
          <h2>报告要求、成绩反馈和发布</h2>
          <div class="requirement-block">
            <b>学习要求</b>
            <p>{{ courseForm.learningRequirement || '暂无学习要求。' }}</p>
          </div>
          <div class="requirement-block">
            <b>考核方式</b>
            <p>{{ courseForm.assessmentMethod || '暂无考核方式。' }}</p>
          </div>
          <div class="action-grid">
            <button type="button" @click="router.push('/teacher/reports')"><el-icon><Document /></el-icon><span>报告批改</span></button>
            <button type="button" @click="router.push('/teacher/dashboard')"><el-icon><DataBoard /></el-icon><span>预览学情</span></button>
          </div>
        </section>
      </main>
    </section>

    <el-dialog v-model="classDialogVisible" :title="editingClass ? '编辑教学班' : '新增教学班'" width="560px">
      <el-form ref="classFormRef" :model="classForm" :rules="classRules" label-width="96px">
        <el-form-item label="教学班" prop="className"><el-input v-model="classForm.className" /></el-form-item>
        <el-form-item label="任课教师ID" prop="teacherId"><el-input-number v-model="classForm.teacherId" :min="1" /></el-form-item>
        <el-form-item label="助教ID"><el-input-number v-model="classForm.assistantId" :min="1" /></el-form-item>
        <el-form-item label="行政班"><el-input v-model="classForm.adminClass" /></el-form-item>
        <el-form-item label="学期"><el-input v-model="classForm.semester" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="classForm.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingClass" @click="saveClassInline">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="experimentDialogVisible" :title="editingExperiment ? '编辑实验章节' : '新增实验章节'" width="780px">
      <el-form ref="experimentFormRef" :model="experimentForm" :rules="experimentRules" label-width="110px">
        <div class="form-grid">
          <el-form-item label="实验名称" prop="expName"><el-input v-model="experimentForm.expName" /></el-form-item>
          <el-form-item label="实验编号" prop="expCode"><el-input v-model="experimentForm.expCode" /></el-form-item>
          <el-form-item label="风险等级" prop="riskLevel">
            <el-select v-model="experimentForm.riskLevel">
              <el-option label="低风险" value="LOW" />
              <el-option label="中风险" value="MEDIUM" />
              <el-option label="高风险" value="HIGH" />
            </el-select>
          </el-form-item>
          <el-form-item label="实验时长"><el-input-number v-model="experimentForm.durationMinutes" :min="1" /></el-form-item>
          <el-form-item label="安全考试"><el-switch v-model="experimentForm.examRequired" active-text="需要" inactive-text="不需要" /></el-form-item>
          <el-form-item label="开放预约"><el-switch v-model="experimentForm.reservationEnabled" active-text="开放" inactive-text="关闭" /></el-form-item>
          <el-form-item label="开放状态"><el-switch v-model="experimentForm.enabled" active-text="开放" inactive-text="停用" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="experimentForm.sort" :min="0" /></el-form-item>
        </div>
        <el-form-item label="封面地址"><el-input v-model="experimentForm.coverUrl" placeholder="可选，留空使用实验路径默认图" /></el-form-item>
        <el-form-item label="情境导入"><el-input v-model="experimentForm.scenarioIntro" type="textarea" :rows="2" placeholder="用真实工程问题引出本实验" /></el-form-item>
        <el-form-item label="视觉主题"><el-input v-model="experimentForm.visualTheme" placeholder="如 procedure-map" /></el-form-item>
        <el-form-item label="实验目标"><el-input v-model="experimentForm.objective" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="实验原理"><el-input v-model="experimentForm.principle" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="仪器材料"><el-input v-model="experimentForm.equipment" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="HSE要求"><el-input v-model="experimentForm.safetyRequirement" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="报告要求"><el-input v-model="experimentForm.gradingCriteria" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="experimentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingExperiment" @click="saveExperimentInline">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back,
  Check,
  DataBoard,
  Delete,
  Document,
  Edit,
  EditPen,
  Finished,
  Folder,
  Operation,
  Plus,
  Reading,
  Refresh,
  User,
  View,
} from '@element-plus/icons-vue'
import {
  archiveCourse,
  createCourseClass,
  deleteCourseClass,
  getCourseDetail,
  publishCourse,
  updateCourse,
  updateCourseClass,
} from '@/api/course'
import { createExperiment, getExperimentDetail, updateExperiment, updateExperimentStatus } from '@/api/experiment'
import resourceCore from '@/assets/amazing/resource-core.png'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const savingCourse = ref(false)
const savingClass = ref(false)
const savingExperiment = ref(false)
const detail = ref(null)
const activeStep = ref('basic')
const classDialogVisible = ref(false)
const experimentDialogVisible = ref(false)
const editingClass = ref(null)
const editingExperiment = ref(null)
const courseFormRef = ref()
const classFormRef = ref()
const experimentFormRef = ref()

const courseId = computed(() => Number(route.params.courseId))
const course = computed(() => detail.value?.course || null)
const classes = computed(() => detail.value?.teachingClasses || [])
const students = computed(() => detail.value?.students || [])
const experiments = computed(() => detail.value?.experiments || [])
const isArchived = computed(() => course.value?.status === 2)

const courseForm = reactive(defaultCourseForm())
const classForm = reactive(defaultClassForm())
const experimentForm = reactive(defaultExperimentForm())
const courseVisual = computed(() => courseForm.coverUrl || course.value?.coverUrl || resourceCore)
const pathReadyCount = computed(() => experiments.value.filter((item) => experimentReadiness(item).ready).length)
const courseReadinessText = computed(() => {
  if (!experiments.value.length) return '请先创建实验章节，再配置资源、准入、预约和报告要求。'
  if (pathReadyCount.value === experiments.value.length) return '全部实验已具备资源学习、风险认知、安全准入、预约与报告反馈的主线配置。'
  return '仍有实验缺少关键配置，建议补齐后再发布给学生。'
})

const courseRules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseCode: [{ required: true, message: '请输入课程编号', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请输入负责人ID', trigger: 'change' }],
}

const classRules = {
  className: [{ required: true, message: '请输入教学班名称', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请输入任课教师ID', trigger: 'change' }],
}

const experimentRules = {
  expName: [{ required: true, message: '请输入实验名称', trigger: 'blur' }],
  expCode: [{ required: true, message: '请输入实验编号', trigger: 'blur' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
}

const steps = [
  { key: 'basic', label: '课程基本信息', icon: Reading },
  { key: 'classes', label: '教学班和学生', icon: User },
  { key: 'experiments', label: '实验章节和规程', icon: Operation },
  { key: 'resources', label: '资源与考试', icon: Folder },
  { key: 'reports', label: '报告与发布', icon: Document },
]

onMounted(loadDetail)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getCourseDetail(courseId.value)
    fillCourseForm()
  } finally {
    loading.value = false
  }
}

async function saveCourseInline() {
  await courseFormRef.value?.validate()
  savingCourse.value = true
  try {
    await updateCourse(courseId.value, {
      courseName: courseForm.courseName.trim(),
      courseCode: courseForm.courseCode.trim(),
      direction: courseForm.direction || undefined,
      teacherId: Number(courseForm.teacherId),
      coverUrl: courseForm.coverUrl || undefined,
      tagline: courseForm.tagline || undefined,
      highlightTags: courseForm.highlightTags || undefined,
      visualTheme: courseForm.visualTheme || undefined,
      description: courseForm.description || undefined,
      semester: courseForm.semester || undefined,
      status: course.value?.status === 1 ? 1 : 0,
      sort: Number(courseForm.sort || 0),
      credit: Number(courseForm.credit || 0),
      hours: Number(courseForm.hours || 0),
      assessmentMethod: courseForm.assessmentMethod || undefined,
      learningRequirement: courseForm.learningRequirement || undefined,
      allowEmptyPublish: courseForm.allowEmptyPublish ? 1 : 0,
    })
    ElMessage.success('课程信息已保存')
    await loadDetail()
  } finally {
    savingCourse.value = false
  }
}

async function publishCurrentCourse() {
  await publishCourse(courseId.value, courseForm.allowEmptyPublish)
  ElMessage.success('课程已发布')
  await loadDetail()
}

async function archiveCurrentCourse() {
  await ElMessageBox.confirm('确认归档当前课程吗？归档后课程建设页将以只读方式展示。', '归档课程', { type: 'warning' })
  await archiveCourse(courseId.value)
  ElMessage.success('课程已归档')
  await loadDetail()
}

function openClassCreate() {
  editingClass.value = null
  Object.assign(classForm, defaultClassForm())
  classDialogVisible.value = true
}

function openClassEdit(row) {
  editingClass.value = row
  Object.assign(classForm, {
    className: row.className || '',
    teacherId: row.teacherId || course.value?.teacherId || null,
    assistantId: row.assistantId || null,
    adminClass: row.adminClass || '',
    semester: row.semester || course.value?.semester || '',
    enabled: row.status === 1,
  })
  classDialogVisible.value = true
}

async function saveClassInline() {
  await classFormRef.value?.validate()
  savingClass.value = true
  try {
    const payload = {
      className: classForm.className.trim(),
      teacherId: Number(classForm.teacherId),
      assistantId: classForm.assistantId ? Number(classForm.assistantId) : undefined,
      adminClass: classForm.adminClass || undefined,
      semester: classForm.semester || undefined,
      status: classForm.enabled ? 1 : 0,
    }
    if (editingClass.value) {
      await updateCourseClass(courseId.value, editingClass.value.id, payload)
      ElMessage.success('教学班已更新')
    } else {
      await createCourseClass(courseId.value, payload)
      ElMessage.success('教学班已创建')
    }
    classDialogVisible.value = false
    await loadDetail()
  } finally {
    savingClass.value = false
  }
}

async function removeClass(row) {
  await ElMessageBox.confirm(`确认删除教学班“${row.className}”吗？`, '删除教学班', { type: 'warning' })
  await deleteCourseClass(courseId.value, row.id)
  ElMessage.success('教学班已删除')
  await loadDetail()
}

function openExperimentCreate() {
  editingExperiment.value = null
  Object.assign(experimentForm, defaultExperimentForm())
  experimentDialogVisible.value = true
}

async function openExperimentEdit(row) {
  editingExperiment.value = row
  const detailResult = await getExperimentDetail(row.id)
  fillExperimentForm(detailResult?.experiment || row)
  experimentDialogVisible.value = true
}

async function saveExperimentInline() {
  await experimentFormRef.value?.validate()
  savingExperiment.value = true
  try {
    const payload = experimentPayload()
    if (editingExperiment.value) {
      await updateExperiment(editingExperiment.value.id, payload)
      ElMessage.success('实验章节已更新')
    } else {
      await createExperiment(payload)
      ElMessage.success('实验章节已创建')
    }
    experimentDialogVisible.value = false
    await loadDetail()
  } finally {
    savingExperiment.value = false
  }
}

async function toggleExperiment(row) {
  await updateExperimentStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('实验状态已更新')
  await loadDetail()
}

function fillCourseForm() {
  const current = course.value
  if (!current) return
  Object.assign(courseForm, {
    courseName: current.courseName || '',
    courseCode: current.courseCode || '',
    direction: current.direction || '',
    teacherId: current.teacherId || null,
    coverUrl: current.coverUrl || '',
    tagline: current.tagline || '',
    highlightTags: current.highlightTags || '',
    visualTheme: current.visualTheme || '',
    description: current.description || '',
    semester: current.semester || '',
    sort: current.sort || 0,
    credit: Number(current.credit || 0),
    hours: Number(current.hours || 0),
    assessmentMethod: current.assessmentMethod || '',
    learningRequirement: current.learningRequirement || detail.value?.learningRequirement || '',
    allowEmptyPublish: current.allowEmptyPublish === 1,
  })
}

function fillExperimentForm(source) {
  Object.assign(experimentForm, {
    expName: source.expName || '',
    expCode: source.expCode || '',
    direction: source.direction || course.value?.direction || '',
    coverUrl: source.coverUrl || '',
    scenarioIntro: source.scenarioIntro || '',
    visualTheme: source.visualTheme || '',
    description: source.description || '',
    objective: source.objective || '',
    principle: source.principle || '',
    equipment: source.equipment || '',
    materials: source.materials || '',
    location: source.location || '',
    applicableClasses: source.applicableClasses || '',
    riskLevel: source.riskLevel || 'MEDIUM',
    hazardSources: source.hazardSources || '',
    riskTypes: source.riskTypes || '',
    ppeRequirements: source.ppeRequirements || '',
    prerequisiteKnowledge: source.prerequisiteKnowledge || '',
    safetyRequirement: source.safetyRequirement || '',
    examRequired: source.examRequired !== 0,
    durationMinutes: source.durationMinutes || 60,
    safetyPassScore: source.safetyPassScore || 60,
    dataRecordRequirement: source.dataRecordRequirement || '',
    abnormalHandling: source.abnormalHandling || '',
    emergencyProcedure: source.emergencyProcedure || '',
    reportTemplateUrl: source.reportTemplateUrl || '',
    gradingCriteria: source.gradingCriteria || '',
    reservationEnabled: source.reservationEnabled !== 0,
    enabled: source.status === 1,
    sort: source.sort || 0,
  })
}

function experimentPayload() {
  return {
    courseId: courseId.value,
    expName: experimentForm.expName.trim(),
    expCode: experimentForm.expCode.trim(),
    direction: experimentForm.direction || undefined,
    coverUrl: experimentForm.coverUrl || undefined,
    scenarioIntro: experimentForm.scenarioIntro || undefined,
    visualTheme: experimentForm.visualTheme || undefined,
    description: experimentForm.description || undefined,
    objective: experimentForm.objective || undefined,
    principle: experimentForm.principle || undefined,
    equipment: experimentForm.equipment || undefined,
    materials: experimentForm.materials || undefined,
    location: experimentForm.location || undefined,
    applicableClasses: experimentForm.applicableClasses || undefined,
    riskLevel: experimentForm.riskLevel,
    hazardSources: experimentForm.hazardSources || undefined,
    riskTypes: experimentForm.riskTypes || undefined,
    ppeRequirements: experimentForm.ppeRequirements || undefined,
    prerequisiteKnowledge: experimentForm.prerequisiteKnowledge || undefined,
    safetyRequirement: experimentForm.safetyRequirement || undefined,
    examRequired: experimentForm.examRequired ? 1 : 0,
    durationMinutes: Number(experimentForm.durationMinutes || 60),
    safetyPassScore: Number(experimentForm.safetyPassScore || 60),
    dataRecordRequirement: experimentForm.dataRecordRequirement || undefined,
    abnormalHandling: experimentForm.abnormalHandling || undefined,
    emergencyProcedure: experimentForm.emergencyProcedure || undefined,
    reportTemplateUrl: experimentForm.reportTemplateUrl || undefined,
    gradingCriteria: experimentForm.gradingCriteria || undefined,
    reservationEnabled: experimentForm.reservationEnabled ? 1 : 0,
    status: experimentForm.enabled ? 1 : 0,
    sort: Number(experimentForm.sort || 0),
  }
}

function defaultCourseForm() {
  return {
    courseName: '',
    courseCode: '',
    direction: '',
    teacherId: null,
    coverUrl: '',
    tagline: '',
    highlightTags: '',
    visualTheme: '',
    description: '',
    semester: '',
    sort: 0,
    credit: 0,
    hours: 0,
    assessmentMethod: '',
    learningRequirement: '',
    allowEmptyPublish: false,
  }
}

function defaultClassForm() {
  return {
    className: '',
    teacherId: course.value?.teacherId || null,
    assistantId: null,
    adminClass: '',
    semester: course.value?.semester || '',
    enabled: true,
  }
}

function defaultExperimentForm() {
  return {
    expName: '',
    expCode: '',
    direction: course.value?.direction || '',
    coverUrl: '',
    scenarioIntro: '',
    visualTheme: '',
    description: '',
    objective: '',
    principle: '',
    equipment: '',
    materials: '',
    location: '',
    applicableClasses: '',
    riskLevel: 'MEDIUM',
    hazardSources: '',
    riskTypes: '',
    ppeRequirements: '',
    prerequisiteKnowledge: '',
    safetyRequirement: '',
    examRequired: true,
    durationMinutes: 60,
    safetyPassScore: 60,
    dataRecordRequirement: '',
    abnormalHandling: '',
    emergencyProcedure: '',
    reportTemplateUrl: '',
    gradingCriteria: '',
    reservationEnabled: true,
    enabled: false,
    sort: experiments.value.length + 1,
  }
}

function statusMeta(status) {
  if (status === 1) return { label: '已发布', type: 'success' }
  if (status === 2) return { label: '已归档', type: 'warning' }
  return { label: '草稿', type: 'info' }
}

function riskLabel(risk) {
  return { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }[risk] || risk || '未分级'
}

function experimentReadiness(item) {
  const missing = []
  if (!item.objective && !item.description && !item.scenarioIntro) missing.push('工程情境')
  if (!item.hazardSources && !item.riskTypes && !item.safetyRequirement) missing.push('风险认知')
  if (item.examRequired !== 0 && !item.safetyPassScore) missing.push('准入分数')
  if (item.reservationEnabled !== 1) missing.push('预约')
  if (!item.gradingCriteria && !item.reportTemplateUrl) missing.push('报告标准')
  return { ready: missing.length === 0, missing }
}

function experimentReadinessText(item) {
  const result = experimentReadiness(item)
  if (result.ready) return '已具备预习、风险、准入、预约、报告与反馈路径'
  return `待补齐：${result.missing.join('、')}`
}
</script>

<style scoped>
.course-editor-page { max-width: 1280px; margin: 0 auto; }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 16px; }
.head-actions, .inline-actions, .footer-actions, .row-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; }
.builder-grid { display: grid; grid-template-columns: 240px minmax(0, 1fr); gap: 16px; }
.step-panel, .editor-panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.step-panel { display: grid; gap: 8px; align-content: start; }
.step-panel button { display: flex; align-items: center; gap: 8px; text-align: left; color: #344054; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; cursor: pointer; }
.step-panel button.active { color: #1f6feb; border-color: #409eff; background: #eef6ff; }
.summary-band { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 16px; }
.summary-band div, .requirement-block, .student-preview, .readiness-band { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.course-visual-preview { min-height: 180px; border-radius: 8px; overflow: hidden; background-size: cover; background-position: center; display: flex; align-items: stretch; }
.course-visual-preview div { width: min(520px, 100%); padding: 22px; background: linear-gradient(90deg, rgba(255,255,255,0.96), rgba(255,255,255,0.76), rgba(255,255,255,0)); display: flex; flex-direction: column; justify-content: center; }
.course-visual-preview span { color: #177e89; font-size: 12px; font-weight: 800; margin-bottom: 6px; }
.course-visual-preview strong { color: #13233a; font-size: 24px; line-height: 1.25; margin-bottom: 8px; }
.summary-band strong { display: block; color: #13233a; font-size: 24px; }
.summary-band span, .experiment-list span { color: #667085; }
.work-section { display: grid; gap: 14px; }
.section-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.section-title.compact { margin-bottom: 10px; }
.work-section h2 { color: #13233a; font-size: 18px; }
.student-preview h3 { color: #13233a; font-size: 15px; }
.path-preview { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.path-preview h3 { color: #13233a; font-size: 15px; }
.readiness-band { display: flex; align-items: center; justify-content: space-between; gap: 16px; border-color: #b7dfc5; background: #f0fbf4; }
.readiness-band strong { display: block; color: #2d6a4f; font-size: 24px; }
.readiness-band span, .readiness-band p { color: #536579; line-height: 1.6; margin: 0; }
.preview-steps { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 8px; }
.preview-steps span { min-height: 58px; display: flex; align-items: center; justify-content: center; text-align: center; color: #177e89; background: #eefafa; border: 1px solid #bfe4e8; border-radius: 8px; font-size: 13px; }
.work-section p { color: #667085; line-height: 1.7; margin: 0; white-space: pre-wrap; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 12px; }
.experiment-list { display: grid; gap: 10px; }
.experiment-list article { display: flex; align-items: center; justify-content: space-between; gap: 10px; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.experiment-list strong { display: block; color: #13233a; margin-bottom: 4px; }
.experiment-list small { display: block; color: #7b8794; line-height: 1.5; margin-top: 4px; }
.action-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.action-grid button { display: flex; align-items: center; gap: 8px; color: #344054; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; cursor: pointer; }
.requirement-block b { display: block; color: #13233a; margin-bottom: 6px; }
@media (max-width: 900px) {
  .builder-grid, .summary-band, .form-grid, .action-grid, .preview-steps { grid-template-columns: 1fr; }
  .page-head, .section-title, .experiment-list article, .readiness-band { align-items: stretch; flex-direction: column; }
}
</style>
