<template>
  <div class="experiment-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">油气工程实验教学与考核平台</p>
        <h1>油气实验路径</h1>
        <p class="page-desc">维护工程情境、预习资源、风险认知、安全准入、实验预约、报告评分和成绩反馈闭环。</p>
      </div>
      <el-button v-permission="'experiment:create'" type="primary" :icon="Plus" @click="openCreate">新增实验</el-button>
    </section>

    <section class="toolbar">
      <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="实验名称或编号" />
      <el-select v-model="filters.courseId" clearable placeholder="所属课程">
        <el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" />
      </el-select>
      <el-select v-model="filters.riskLevel" clearable placeholder="风险等级">
        <el-option label="低风险" value="LOW" />
        <el-option label="中风险" value="MEDIUM" />
        <el-option label="高风险" value="HIGH" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态">
        <el-option label="关闭" :value="0" />
        <el-option label="开放" :value="1" />
        <el-option label="维护" :value="2" />
      </el-select>
      <el-button :icon="Search" @click="loadExperiments">查询</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="experiments" stripe empty-text="暂无实验项目">
        <el-table-column prop="expCode" label="编号" width="120" />
        <el-table-column prop="expName" label="实验名称" min-width="180" />
        <el-table-column label="课程/方向" min-width="170">
          <template #default="{ row }">
            <div class="two-line">
              <strong>{{ courseName(row.courseId) }}</strong>
              <span>{{ row.direction || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="风险" width="95">
          <template #default="{ row }"><el-tag :type="riskMeta(row.riskLevel).type">{{ riskMeta(row.riskLevel).label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="时长/地点" width="130">
          <template #default="{ row }">{{ row.durationMinutes || 0 }} 分钟<br />{{ row.location || '-' }}</template>
        </el-table-column>
        <el-table-column label="预约" width="80">
          <template #default="{ row }"><el-tag :type="row.reservationEnabled === 1 ? 'success' : 'info'">{{ row.reservationEnabled === 1 ? '启用' : '关闭' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button v-permission="'experiment:update'" text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'experiment:update'" text type="success" :disabled="row.status === 1" @click="setStatus(row, 1)">开放</el-button>
            <el-button v-permission="'experiment:update'" text type="warning" :disabled="row.status === 2" @click="setStatus(row, 2)">维护</el-button>
            <el-button v-permission="'experiment:delete'" text type="danger" :icon="Delete" @click="removeExperiment(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="pageNum" layout="prev, pager, next" :page-size="pageSize" :total="total" @current-change="loadExperiments" />
      </div>
    </section>

    <el-dialog v-model="formVisible" :title="editingExperiment ? '编辑实验' : '新增实验'" width="900px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="112px">
        <div class="form-grid">
          <el-form-item label="所属课程" prop="courseId">
            <el-select v-model="form.courseId" placeholder="请选择课程" @change="fillDirection">
              <el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="实验编号" prop="expCode"><el-input v-model="form.expCode" /></el-form-item>
          <el-form-item label="实验名称" prop="expName"><el-input v-model="form.expName" /></el-form-item>
          <el-form-item label="专业方向"><el-input v-model="form.direction" /></el-form-item>
          <el-form-item label="风险等级" prop="riskLevel">
            <el-select v-model="form.riskLevel">
              <el-option label="低风险" value="LOW" />
              <el-option label="中风险" value="MEDIUM" />
              <el-option label="高风险" value="HIGH" />
            </el-select>
          </el-form-item>
          <el-form-item label="实验时长" prop="durationMinutes"><el-input-number v-model="form.durationMinutes" :min="1" :max="600" /></el-form-item>
          <el-form-item label="实验地点"><el-input v-model="form.location" /></el-form-item>
          <el-form-item label="适用班级"><el-input v-model="form.applicableClasses" /></el-form-item>
          <el-form-item label="准入考试"><el-switch v-model="form.examRequired" active-text="需要" inactive-text="不需要" /></el-form-item>
          <el-form-item label="通过分数"><el-input-number v-model="form.safetyPassScore" :min="0" :max="100" /></el-form-item>
          <el-form-item label="开放预约"><el-switch v-model="form.reservationEnabled" active-text="启用" inactive-text="关闭" /></el-form-item>
          <el-form-item label="状态"><el-segmented v-model="form.status" :options="statusOptions" /></el-form-item>
        </div>
        <el-form-item label="封面地址"><el-input v-model="form.coverUrl" placeholder="可选，留空使用实验路径默认图" /></el-form-item>
        <el-form-item label="情境导入"><el-input v-model="form.scenarioIntro" type="textarea" :rows="2" placeholder="用真实工程问题引出本实验" /></el-form-item>
        <el-form-item label="视觉主题"><el-input v-model="form.visualTheme" placeholder="如 procedure-map" /></el-form-item>
        <el-form-item label="实验简介"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="实验目标" prop="objective"><el-input v-model="form.objective" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="实验原理" prop="principle"><el-input v-model="form.principle" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="仪器设备" prop="equipment"><el-input v-model="form.equipment" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="材料"><el-input v-model="form.materials" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="危险源"><el-input v-model="form.hazardSources" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="风险类型"><el-input v-model="form.riskTypes" placeholder="如 高压、易燃、机械伤害" /></el-form-item>
        <el-form-item label="PPE要求"><el-input v-model="form.ppeRequirements" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="前置知识"><el-input v-model="form.prerequisiteKnowledge" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="安全要求"><el-input v-model="form.safetyRequirement" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="数据记录"><el-input v-model="form.dataRecordRequirement" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="异常处理"><el-input v-model="form.abnormalHandling" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="应急流程"><el-input v-model="form.emergencyProcedure" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="报告模板"><el-input v-model="form.reportTemplateUrl" /></el-form-item>
        <el-form-item label="评分标准"><el-input v-model="form.gradingCriteria" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveExperiment">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="实验项目详情" width="1040px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-visual" :style="{ backgroundImage: `url(${detail.experiment?.coverUrl || procedureSafety})` }">
            <div>
              <span>{{ detail.experiment?.scenarioIntro || '从真实工程现场问题导入实验学习' }}</span>
              <strong>{{ detail.experiment?.expName }}</strong>
            </div>
          </div>
          <div class="detail-head">
            <div>
              <h2>{{ detail.experiment?.expName }}</h2>
              <span>{{ detail.courseName }} · {{ detail.experiment?.expCode }}</span>
            </div>
            <el-tag :type="statusMeta(detail.experiment?.status).type">{{ statusMeta(detail.experiment?.status).label }}</el-tag>
          </div>
          <div class="summary-grid">
            <div><strong>{{ detail.resources?.length || 0 }}</strong><span>资源</span></div>
            <div><strong>{{ detail.examCount || 0 }}</strong><span>考试</span></div>
            <div><strong>{{ detail.reservationCount || 0 }}</strong><span>预约</span></div>
            <div><strong>{{ detail.reportCount || 0 }}</strong><span>报告</span></div>
          </div>
          <div class="path-check-grid">
            <div v-for="item in detailReadinessItems" :key="item.label" :class="{ done: item.done }">
              <strong>{{ item.label }}</strong>
              <span>{{ item.done ? '已配置' : item.tip }}</span>
            </div>
          </div>
          <el-tabs v-model="detailTab">
            <el-tab-pane label="规程信息" name="info">
              <div class="info-grid">
                <p><b>目标</b>{{ detail.experiment?.objective || '-' }}</p>
                <p><b>原理</b>{{ detail.experiment?.principle || '-' }}</p>
                <p><b>仪器设备</b>{{ detail.experiment?.equipment || '-' }}</p>
                <p><b>PPE</b>{{ detail.experiment?.ppeRequirements || '-' }}</p>
                <p><b>异常处理</b>{{ detail.experiment?.abnormalHandling || '-' }}</p>
                <p><b>应急流程</b>{{ detail.experiment?.emergencyProcedure || '-' }}</p>
              </div>
            </el-tab-pane>
            <el-tab-pane label="操作步骤" name="steps">
              <div class="tab-actions">
                <el-button v-permission="'experiment:update'" type="primary" :icon="Plus" @click="addStep">新增步骤</el-button>
                <el-button v-permission="'experiment:update'" type="success" :loading="stepSaving" @click="saveSteps">保存步骤</el-button>
              </div>
              <el-empty v-if="steps.length === 0" description="暂无实验步骤" />
              <div v-else class="step-list">
                <div v-for="(step, index) in steps" :key="index" class="step-card">
                  <div class="step-meta">
                    <el-input-number v-model="step.stepNo" :min="1" />
                    <el-input v-model="step.title" placeholder="步骤标题，将作为学生章节小标题" />
                    <el-select v-model="step.mediaType" placeholder="展示类型">
                      <el-option label="文本" value="TEXT" />
                      <el-option label="图片" value="IMAGE" />
                      <el-option label="视频" value="VIDEO" />
                      <el-option label="流程图" value="FLOWCHART" />
                    </el-select>
                    <el-input-number v-model="step.estimatedMinutes" :min="0" />
                    <el-button text type="danger" :icon="Delete" @click="steps.splice(index, 1)">删除</el-button>
                  </div>
                  <div class="step-columns">
                    <el-input v-model="step.content" type="textarea" :rows="4" placeholder="步骤文字说明，学生章节详情左侧将显示这里的内容" />
                    <el-input v-model="step.safetyTip" type="textarea" :rows="4" placeholder="风险提示/安全提醒" />
                  </div>
                  <div class="step-media">
                    <el-input v-model="step.mediaUrl" placeholder="步骤视频、图片或资料链接，选择视频类型时学生端会内嵌播放" />
                    <el-input v-model="step.flowchartData" placeholder="流程图数据" />
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="入口聚合" name="entrances">
              <div class="entrance-grid">
                <a v-for="item in detail.entrances || []" :key="item.type" :href="item.path">{{ item.title }}</a>
              </div>
            </el-tab-pane>
            <el-tab-pane label="学习任务" name="tasks">
              <div class="tab-actions">
                <span class="task-hint">建议按预习资源、风险认知、安全准入、准备清单的顺序配置任务。</span>
                <el-button v-permission="'experiment:update'" type="primary" :icon="Plus" @click="openTaskCreate">新增任务</el-button>
              </div>
              <el-table :data="learningTasks" stripe empty-text="暂无学习任务">
                <el-table-column prop="task.sort" label="顺序" width="70" />
                <el-table-column label="任务" min-width="180">
                  <template #default="{ row }">
                    <div class="two-line">
                      <strong>{{ row.task.taskName }}</strong>
                      <span>{{ taskTypeLabel(row.task.taskType) }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="目标" width="150">
                  <template #default="{ row }">{{ targetLabel(row.task) }}</template>
                </el-table-column>
                <el-table-column label="前置" width="90">
                  <template #default="{ row }">{{ row.task.prerequisiteTaskId || '-' }}</template>
                </el-table-column>
                <el-table-column label="完成分布" min-width="180">
                  <template #default="{ row }">
                    完成 {{ row.completedCount || 0 }}/{{ row.studentCount || 0 }}，
                    临期 {{ row.dueSoonCount || 0 }}，逾期 {{ row.overdueCount || 0 }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="80">
                  <template #default="{ row }"><el-tag :type="row.task.status === 1 ? 'success' : 'info'">{{ row.task.status === 1 ? '启用' : '停用' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right">
                  <template #default="{ row }">
                    <el-button text type="primary" :icon="Edit" @click="openTaskEdit(row.task)">编辑</el-button>
                    <el-button text type="warning" @click="disableTask(row.task)">停用</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="学习难点" name="hotspots">
              <el-empty v-if="timelineHotspots.length === 0" description="暂无课程内可见的问题或风险标记" />
              <div v-else class="hotspot-list">
                <article v-for="item in timelineHotspots" :key="item.resourceId" class="hotspot-card">
                  <div>
                    <h3>{{ item.resourceTitle }}</h3>
                    <p>{{ item.latestQuestion || '学生主要在该资源记录了笔记或风险点，可结合报告与错题继续判断。' }}</p>
                  </div>
                  <div class="hotspot-stats">
                    <span><b>{{ item.questionCount || 0 }}</b>问题</span>
                    <span><b>{{ item.riskCount || 0 }}</b>风险</span>
                    <span><b>{{ item.noteCount || 0 }}</b>记录</span>
                  </div>
                </article>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="taskFormVisible" :title="editingTask ? '编辑学习任务' : '新增学习任务'" width="720px">
      <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-width="112px">
        <div class="form-grid">
          <el-form-item label="任务名称" prop="taskName"><el-input v-model="taskForm.taskName" /></el-form-item>
          <el-form-item label="任务类型" prop="taskType">
            <el-select v-model="taskForm.taskType">
              <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="资源ID"><el-input v-model="taskForm.targetResourceId" /></el-form-item>
          <el-form-item label="试卷ID"><el-input v-model="taskForm.targetPaperId" /></el-form-item>
          <el-form-item label="安全知识ID"><el-input v-model="taskForm.targetKnowledgeId" /></el-form-item>
          <el-form-item label="前置任务ID"><el-input v-model="taskForm.prerequisiteTaskId" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="taskForm.sort" :min="0" /></el-form-item>
          <el-form-item label="必做"><el-switch v-model="taskForm.required" /></el-form-item>
          <el-form-item label="开放时间"><el-date-picker v-model="taskForm.openTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选" /></el-form-item>
          <el-form-item label="截止时间"><el-date-picker v-model="taskForm.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选" /></el-form-item>
          <el-form-item label="启用"><el-switch v-model="taskForm.enabled" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="taskFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskSaving" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Search, View } from '@element-plus/icons-vue'
import { getCourses } from '@/api/course'
import {
  createExperiment,
  deleteExperiment,
  getExperimentDetail,
  getExperiments,
  saveExperimentSteps,
  updateExperiment,
  updateExperimentStatus,
} from '@/api/experiment'
import {
  createLearningTask,
  disableLearningTask,
  getTaskDistribution,
  updateLearningTask,
} from '@/api/learningTask'
import { getResourceTimelineHotspots } from '@/api/resource'
import procedureSafety from '@/assets/amazing/procedure-safety.png'

const courses = ref([])
const experiments = ref([])
const detail = ref(null)
const steps = ref([])
const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const stepSaving = ref(false)
const taskSaving = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const taskFormVisible = ref(false)
const editingExperiment = ref(null)
const editingTask = ref(null)
const formRef = ref()
const taskFormRef = ref()
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailTab = ref('info')
const learningTasks = ref([])
const timelineHotspots = ref([])
let keywordTimer = null
const detailReadinessItems = computed(() => {
  const experiment = detail.value?.experiment || {}
  return [
    { label: '工程情境', done: Boolean(experiment.scenarioIntro || experiment.description || experiment.objective), tip: '补充情境导入或实验目标' },
    { label: '预习资源', done: (detail.value?.resources || []).length > 0, tip: '绑定视频、PDF、图片或外链资源' },
    { label: '风险认知', done: Boolean(experiment.hazardSources || experiment.riskTypes || experiment.ppeRequirements), tip: '维护危险源、风险类型和 PPE' },
    { label: '安全准入', done: experiment.examRequired === 0 || (detail.value?.examCount || 0) > 0 || learningTasks.value.some((item) => item.task?.taskType === 'EXAM'), tip: '配置准入考试或考试任务' },
    { label: '实验预约', done: experiment.reservationEnabled === 1, tip: '开启预约并维护实验时段' },
    { label: '报告反馈', done: Boolean(experiment.gradingCriteria || experiment.reportTemplateUrl), tip: '补充报告模板或评分标准' },
  ]
})

const filters = reactive({ keyword: '', courseId: '', riskLevel: '', status: '' })
const form = reactive(defaultForm())
const taskForm = reactive(defaultTaskForm())
const formRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  expCode: [{ required: true, message: '请输入实验编号', trigger: 'blur' }],
  expName: [{ required: true, message: '请输入实验名称', trigger: 'blur' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  durationMinutes: [{ required: true, message: '请输入实验时长', trigger: 'change' }],
}
const statusOptions = [
  { label: '关闭', value: 0 },
  { label: '开放', value: 1 },
  { label: '维护', value: 2 },
]
const taskTypeOptions = [
  { label: '阅读资源', value: 'READ_RESOURCE' },
  { label: '观看视频', value: 'WATCH_VIDEO' },
  { label: '学习安全知识', value: 'SAFETY_KNOWLEDGE' },
  { label: '完成练习', value: 'PRACTICE' },
  { label: '参加考试', value: 'EXAM' },
  { label: '确认准备清单', value: 'CHECKLIST' },
]
const taskRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
}

watch(() => filters.keyword, () => {
  pageNum.value = 1
  window.clearTimeout(keywordTimer)
  keywordTimer = window.setTimeout(loadExperiments, 300)
})
watch([() => filters.courseId, () => filters.riskLevel, () => filters.status], () => {
  pageNum.value = 1
  loadExperiments()
})

onMounted(async () => {
  await loadCourses()
  await loadExperiments()
})

async function loadCourses() {
  const result = await getCourses({ pageNum: 1, pageSize: 100 })
  courses.value = result?.records || []
}

async function loadExperiments() {
  loading.value = true
  try {
    const result = await getExperiments({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      courseId: filters.courseId || undefined,
      riskLevel: filters.riskLevel || undefined,
      status: filters.status === '' ? undefined : filters.status,
    })
    experiments.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingExperiment.value = null
  Object.assign(form, defaultForm())
  formVisible.value = true
}

function openEdit(row) {
  editingExperiment.value = row
  Object.assign(form, { ...defaultForm(), ...row, examRequired: row.examRequired !== 0, reservationEnabled: row.reservationEnabled === 1 })
  formVisible.value = true
}

async function saveExperiment() {
  await formRef.value?.validate()
  const payload = normalizeForm()
  saving.value = true
  try {
    if (editingExperiment.value) {
      await updateExperiment(editingExperiment.value.id, payload)
      ElMessage.success('实验已更新')
    } else {
      await createExperiment(payload)
      ElMessage.success('实验已创建')
    }
    formVisible.value = false
    await loadExperiments()
  } finally {
    saving.value = false
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailTab.value = 'info'
  try {
    detail.value = await getExperimentDetail(row.id)
    steps.value = (detail.value?.steps || []).map((item) => ({ mediaType: 'TEXT', ...item }))
    await loadLearningTasks(row.id)
    await loadTimelineHotspots(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function loadLearningTasks(experimentId) {
  learningTasks.value = await getTaskDistribution(experimentId)
}

async function loadTimelineHotspots(experimentId) {
  timelineHotspots.value = await getResourceTimelineHotspots(experimentId)
}

async function saveSteps() {
  if (!detail.value?.experiment?.id) return
  if (steps.value.some((item) => !item.stepNo || !item.title?.trim() || !item.content?.trim())) {
    ElMessage.warning('请完善步骤序号、标题和操作内容')
    return
  }
  stepSaving.value = true
  try {
    await saveExperimentSteps(detail.value.experiment.id, steps.value.map((item) => ({
      stepNo: Number(item.stepNo),
      title: item.title.trim(),
      content: item.content.trim(),
      safetyTip: item.safetyTip || undefined,
      mediaType: item.mediaType || 'TEXT',
      mediaUrl: item.mediaUrl || undefined,
      flowchartData: item.flowchartData || undefined,
      requiredFlag: item.requiredFlag ?? 1,
      estimatedMinutes: Number(item.estimatedMinutes || 0),
    })))
    ElMessage.success('步骤已保存')
    await openDetail(detail.value.experiment)
  } finally {
    stepSaving.value = false
  }
}

function addStep() {
  steps.value.push({
    stepNo: steps.value.length + 1,
    title: '',
    content: '',
    safetyTip: '',
    mediaType: 'TEXT',
    mediaUrl: '',
    flowchartData: '',
    requiredFlag: 1,
    estimatedMinutes: 0,
  })
}

function openTaskCreate() {
  editingTask.value = null
  Object.assign(taskForm, defaultTaskForm())
  taskFormVisible.value = true
}

function openTaskEdit(task) {
  editingTask.value = task
  Object.assign(taskForm, {
    taskName: task.taskName || '',
    taskType: task.taskType || 'READ_RESOURCE',
    targetResourceId: task.targetResourceId || '',
    targetKnowledgeId: task.targetKnowledgeId || '',
    targetPaperId: task.targetPaperId || '',
    prerequisiteTaskId: task.prerequisiteTaskId || '',
    required: task.requiredFlag !== 0,
    sort: task.sort || 0,
    openTime: task.openTime || '',
    deadline: task.deadline || '',
    enabled: task.status !== 0,
  })
  taskFormVisible.value = true
}

async function saveTask() {
  await taskFormRef.value?.validate()
  const payload = normalizeTaskForm()
  taskSaving.value = true
  try {
    if (editingTask.value) {
      await updateLearningTask(editingTask.value.id, payload)
      ElMessage.success('学习任务已更新')
    } else {
      await createLearningTask(payload)
      ElMessage.success('学习任务已创建')
    }
    taskFormVisible.value = false
    await loadLearningTasks(detail.value.experiment.id)
  } finally {
    taskSaving.value = false
  }
}

async function disableTask(task) {
  await ElMessageBox.confirm(`确认停用任务「${task.taskName}」吗？已有完成记录会保留。`, '停用任务', { type: 'warning' })
  await disableLearningTask(task.id)
  ElMessage.success('任务已停用')
  await loadLearningTasks(detail.value.experiment.id)
}

async function setStatus(row, status) {
  await updateExperimentStatus(row.id, status)
  ElMessage.success('状态已更新')
  await loadExperiments()
}

async function removeExperiment(row) {
  await ElMessageBox.confirm(`确认删除实验「${row.expName}」吗？已有业务历史时只能关闭。`, '删除实验', { type: 'warning' })
  await deleteExperiment(row.id)
  ElMessage.success('实验已删除')
  await loadExperiments()
}

function normalizeForm() {
  return {
    ...form,
    courseId: Number(form.courseId),
    durationMinutes: Number(form.durationMinutes || 0),
    safetyPassScore: Number(form.safetyPassScore || 0),
    examRequired: form.examRequired ? 1 : 0,
    reservationEnabled: form.reservationEnabled ? 1 : 0,
    status: Number(form.status),
    sort: Number(form.sort || 0),
  }
}

function normalizeTaskForm() {
  return {
    experimentId: detail.value.experiment.id,
    taskName: taskForm.taskName.trim(),
    taskType: taskForm.taskType,
    targetResourceId: taskForm.targetResourceId ? Number(taskForm.targetResourceId) : undefined,
    targetKnowledgeId: taskForm.targetKnowledgeId ? Number(taskForm.targetKnowledgeId) : undefined,
    targetPaperId: taskForm.targetPaperId ? Number(taskForm.targetPaperId) : undefined,
    prerequisiteTaskId: taskForm.prerequisiteTaskId ? Number(taskForm.prerequisiteTaskId) : undefined,
    requiredFlag: taskForm.required ? 1 : 0,
    sort: Number(taskForm.sort || 0),
    openTime: taskForm.openTime || undefined,
    deadline: taskForm.deadline || undefined,
    completionRule: 'AUTO',
    status: taskForm.enabled ? 1 : 0,
  }
}

function fillDirection() {
  const course = courses.value.find((item) => item.id === form.courseId)
  if (course && !form.direction) form.direction = course.direction || ''
}

function courseName(courseId) {
  return courses.value.find((item) => item.id === courseId)?.courseName || `课程 ${courseId}`
}

function defaultForm() {
  return {
    courseId: '',
    expName: '',
    expCode: '',
    direction: '',
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
    riskLevel: 'LOW',
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
    status: 0,
    sort: 0,
  }
}

function defaultTaskForm() {
  return {
    taskName: '',
    taskType: 'READ_RESOURCE',
    targetResourceId: '',
    targetKnowledgeId: '',
    targetPaperId: '',
    prerequisiteTaskId: '',
    required: true,
    sort: 0,
    openTime: '',
    deadline: '',
    enabled: true,
  }
}

function taskTypeLabel(type) {
  return taskTypeOptions.find((item) => item.value === type)?.label || type || '-'
}

function targetLabel(task) {
  if (task.targetResourceId) return `资源 ${task.targetResourceId}`
  if (task.targetPaperId) return `试卷 ${task.targetPaperId}`
  if (task.targetKnowledgeId) return `知识 ${task.targetKnowledgeId}`
  return '-'
}

function statusMeta(status) {
  if (status === 1) return { label: '开放', type: 'success' }
  if (status === 2) return { label: '维护', type: 'warning' }
  return { label: '关闭', type: 'info' }
}

function riskMeta(risk) {
  if (risk === 'HIGH') return { label: '高', type: 'danger' }
  if (risk === 'MEDIUM') return { label: '中', type: 'warning' }
  return { label: '低', type: 'success' }
}
</script>

<style scoped>
.experiment-page { max-width: 1280px; margin: 0 auto; }
.page-head, .pagination-row, .tab-actions { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.page-head { align-items: flex-end; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.toolbar, .panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: minmax(220px, 1fr) 180px 130px 110px auto; gap: 12px; padding: 14px; margin-bottom: 16px; }
.panel { padding: 14px; }
.two-line { display: grid; gap: 4px; }
.two-line span, .pagination-row { color: #667085; font-size: 13px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; column-gap: 12px; }
.detail-body { min-height: 240px; }
.detail-visual { min-height: 160px; border-radius: 8px; overflow: hidden; background-size: cover; background-position: center; display: flex; align-items: stretch; margin-bottom: 14px; }
.detail-visual div { width: min(560px, 100%); padding: 20px; background: linear-gradient(90deg, rgba(255,255,255,0.96), rgba(255,255,255,0.76), rgba(255,255,255,0)); display: flex; flex-direction: column; justify-content: center; }
.detail-visual span { color: #177e89; font-size: 13px; font-weight: 800; margin-bottom: 8px; }
.detail-visual strong { color: #13233a; font-size: 24px; line-height: 1.25; }
.detail-head { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.detail-head h2 { color: #13233a; font-size: 20px; margin-bottom: 4px; }
.detail-head span { color: #667085; }
.summary-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 14px; }
.summary-grid div { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.summary-grid strong { display: block; color: #13233a; font-size: 22px; }
.summary-grid span { color: #7b8794; font-size: 12px; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.info-grid p { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; color: #4b5b6b; line-height: 1.7; margin: 0; padding: 12px; white-space: pre-wrap; }
.info-grid b { color: #13233a; display: block; margin-bottom: 6px; }
.tab-actions { justify-content: flex-end; margin-bottom: 12px; }
.task-hint { color: #667085; font-size: 13px; margin-right: auto; }
.path-check-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 8px; margin-bottom: 14px; }
.path-check-grid div { min-height: 70px; border: 1px solid #edf1f5; border-radius: 8px; background: #f8fafc; padding: 10px; display: grid; align-content: center; gap: 4px; }
.path-check-grid div.done { border-color: #b7dfc5; background: #f0fbf4; }
.path-check-grid strong { color: #13233a; font-size: 13px; }
.path-check-grid span { color: #667085; font-size: 12px; line-height: 1.45; }
.step-list { display: grid; gap: 12px; }
.step-card { border: 1px solid #e7ebf0; border-radius: 8px; padding: 12px; }
.step-meta, .step-media { display: grid; grid-template-columns: 110px minmax(180px, 1fr) 120px 110px auto; gap: 10px; margin-bottom: 10px; }
.step-media { grid-template-columns: 1fr 1fr; }
.step-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.entrance-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.entrance-grid a { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; color: #1f6feb; padding: 14px; text-align: center; text-decoration: none; }
.hotspot-list { display: grid; gap: 12px; }
.hotspot-card { display: flex; justify-content: space-between; gap: 12px; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.hotspot-card h3 { color: #13233a; font-size: 16px; margin-bottom: 6px; }
.hotspot-card p { color: #667085; line-height: 1.6; margin: 0; }
.hotspot-stats { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.hotspot-stats span { min-width: 66px; border: 1px solid #edf1f5; border-radius: 8px; padding: 8px; color: #667085; text-align: center; }
.hotspot-stats b { display: block; color: #13233a; font-size: 18px; }
@media (max-width: 900px) {
  .toolbar, .form-grid, .summary-grid, .path-check-grid, .info-grid, .step-meta, .step-media, .step-columns, .entrance-grid { grid-template-columns: 1fr; }
  .page-head, .pagination-row, .hotspot-card { align-items: stretch; flex-direction: column; }
}
</style>
