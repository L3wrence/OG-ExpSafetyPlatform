<template>
  <div class="exam-manager">
    <header class="page-head">
      <div>
        <h1>课堂安全题库与组卷</h1>
        <p>维护当前课堂安全题库，按知识点、难度和题型组建课堂安全考试。</p>
      </div>
      <div class="head-actions">
        <el-button v-if="scopedCourseId" :icon="Back" @click="router.push(`/teacher/courses/${scopedCourseId}/edit?step=exam`)">返回课堂详细</el-button>
        <el-button :icon="Refresh" @click="reloadAll">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openQuestionCreate">新增题目</el-button>
        <el-button type="success" :icon="DocumentAdd" @click="openPaperCreate">创建试卷</el-button>
      </div>
    </header>

    <section class="workspace">
      <aside class="filter-pane">
        <h2>筛选</h2>
        <el-form label-position="top">
          <el-form-item v-if="scopedCourseId" label="课堂">
            <el-tag size="large">{{ scopedCourseName }}</el-tag>
          </el-form-item>
          <el-form-item v-else label="课堂">
            <el-select v-model="filters.courseId" filterable clearable placeholder="选择课堂" @change="handleCourseChange">
              <el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="实验">
            <el-select v-model="filters.experimentId" filterable clearable placeholder="选择实验">
              <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="filters.type" clearable placeholder="全部题型">
              <el-option label="单选题" value="SINGLE" />
              <el-option label="多选题" value="MULTIPLE" />
              <el-option label="判断题" value="JUDGE" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="filters.difficulty" clearable placeholder="全部难度">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识点">
            <el-input v-model="filters.knowledgePoint" clearable placeholder="如 高压管线" />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" clearable placeholder="搜索题干" @keyup.enter="loadQuestions" />
          </el-form-item>
          <el-button type="primary" :icon="Search" @click="loadQuestions">查询题库</el-button>
        </el-form>
      </aside>

      <main class="content-pane">
        <el-tabs v-model="activeTab" class="exam-tabs" @tab-change="handleTabChange">
          <el-tab-pane label="安全题库" name="questions">
            <div class="toolbar">
              <strong>题目列表</strong>
              <el-button :disabled="!selectedQuestions.length || !activePaper" type="primary" plain @click="addSelectedToPaper">
                加入当前试卷
              </el-button>
            </div>
            <el-table v-loading="questionLoading" :data="questions" row-key="id" @selection-change="selectedQuestions = $event">
              <el-table-column type="selection" width="44" />
              <el-table-column label="题型" width="90">
                <template #default="{ row }">{{ typeLabel(row.type) }}</template>
              </el-table-column>
              <el-table-column prop="content" label="题干" min-width="260" show-overflow-tooltip />
              <el-table-column prop="knowledgePoint" label="知识点" width="130" />
              <el-table-column label="难度" width="90">
                <template #default="{ row }">
                  <el-tag :type="difficultyTag(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="score" label="默认分" width="90" />
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openQuestionEdit(row)">编辑</el-button>
                  <el-button text type="danger" @click="removeQuestion(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="试卷管理" name="papers">
            <div class="paper-layout">
              <section class="paper-list">
                <div class="toolbar">
                  <strong>试卷列表</strong>
                  <el-button :icon="Plus" type="primary" @click="openPaperCreate">创建试卷</el-button>
                </div>
                <el-table v-loading="paperLoading" :data="papers" row-key="id" highlight-current-row @current-change="selectPaper">
                  <el-table-column prop="title" label="试卷" min-width="190" />
                  <el-table-column label="状态" width="90">
                    <template #default="{ row }">
                      <el-tag :type="paperStatusTag(row.status)">{{ paperStatusLabel(row.status) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="totalScore" label="总分" width="70" />
                  <el-table-column prop="passScore" label="及格" width="70" />
                  <el-table-column label="操作" width="170" fixed="right">
                    <template #default="{ row }">
                      <el-button text type="primary" @click.stop="openPaperEdit(row)">编辑</el-button>
                      <el-button text :type="row.status === 'PUBLISHED' ? 'warning' : 'success'" @click.stop="togglePaper(row)">
                        {{ row.status === 'PUBLISHED' ? '关闭' : '发布' }}
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </section>

              <section class="paper-detail">
                <template v-if="activePaper">
                  <div class="detail-head">
                    <div>
                      <h2>{{ activePaper.title }}</h2>
                      <p>{{ activePaper.description || '暂无试卷说明' }}</p>
                    </div>
                    <el-button type="success" plain @click="smartDialogVisible = true">智能组卷</el-button>
                  </div>
                  <el-table :data="paperQuestions" empty-text="暂无题目，可从题库手动加入或使用智能组卷">
                    <el-table-column prop="orderNum" label="#" width="58" />
                    <el-table-column label="题型" width="90">
                      <template #default="{ row }">{{ typeLabel(row.question?.type) }}</template>
                    </el-table-column>
                    <el-table-column label="题干" min-width="220" show-overflow-tooltip>
                      <template #default="{ row }">{{ row.question?.content }}</template>
                    </el-table-column>
                    <el-table-column label="知识点" width="120">
                      <template #default="{ row }">{{ row.question?.knowledgePoint || '-' }}</template>
                    </el-table-column>
                    <el-table-column prop="score" label="分值" width="80" />
                    <el-table-column label="操作" width="90">
                      <template #default="{ row }">
                        <el-button text type="danger" @click="removeFromPaper(row)">移除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
                <el-empty v-else description="请选择一份试卷进行编辑" />
              </section>
            </div>
          </el-tab-pane>
        </el-tabs>
      </main>
    </section>

    <el-dialog v-model="questionDialogVisible" :title="editingQuestion ? '编辑题目' : '新增题目'" width="720px">
      <el-form :model="questionForm" label-width="88px">
        <el-form-item label="关联课堂" required>
          <el-input v-if="scopedCourseId" :model-value="scopedCourseName" disabled />
          <el-select v-else v-model="questionForm.courseId" filterable placeholder="选择课堂" @change="loadExperiments(questionForm.courseId)">
            <el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联实验">
          <el-select v-model="questionForm.experimentId" filterable clearable placeholder="可选">
            <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" required>
          <el-radio-group v-model="questionForm.type" @change="resetQuestionAnswer">
            <el-radio-button label="SINGLE">单选</el-radio-button>
            <el-radio-button label="MULTIPLE">多选</el-radio-button>
            <el-radio-button label="JUDGE">判断</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" required><el-input v-model="questionForm.content" type="textarea" :rows="3" /></el-form-item>
        <div v-if="questionForm.type !== 'JUDGE'" class="option-grid">
          <el-form-item v-for="option in optionRows" :key="option.key" :label="`选项${option.key}`">
            <el-input v-model="option.label" />
          </el-form-item>
        </div>
        <el-form-item label="正确答案" required>
          <el-select v-if="questionForm.type === 'MULTIPLE'" v-model="questionAnswerArray" multiple placeholder="选择正确选项">
            <el-option v-for="option in answerOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-select v-else v-model="questionForm.answer" placeholder="选择正确答案">
            <el-option v-for="option in answerOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="知识点" required><el-input v-model="questionForm.knowledgePoint" /></el-form-item>
          <el-form-item label="难度" required>
            <el-select v-model="questionForm.difficulty">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="默认分"><el-input-number v-model="questionForm.score" :min="1" :max="100" /></el-form-item>
        </div>
        <el-form-item label="解析"><el-input v-model="questionForm.analysis" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingQuestion" @click="saveQuestion">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paperDialogVisible" :title="editingPaper ? '编辑试卷' : '创建试卷'" width="680px">
      <el-form :model="paperForm" label-width="96px">
        <el-form-item label="试卷标题" required><el-input v-model="paperForm.title" /></el-form-item>
        <el-form-item label="关联课堂" required>
          <el-input v-if="scopedCourseId" :model-value="scopedCourseName" disabled />
          <el-select v-else v-model="paperForm.courseId" filterable placeholder="选择课堂" @change="loadExperiments(paperForm.courseId)">
            <el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联实验">
          <el-select v-model="paperForm.experimentId" filterable clearable placeholder="可选">
            <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="总分"><el-input-number v-model="paperForm.totalScore" :min="1" :max="500" /></el-form-item>
          <el-form-item label="及格分"><el-input-number v-model="paperForm.passScore" :min="1" :max="500" /></el-form-item>
          <el-form-item label="时长"><el-input-number v-model="paperForm.duration" :min="1" :max="300" /></el-form-item>
          <el-form-item label="次数"><el-input-number v-model="paperForm.attemptLimit" :min="1" :max="10" /></el-form-item>
        </div>
        <el-form-item label="交卷后答案"><el-switch v-model="paperForm.showAnswerAfterSubmit" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="paperForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paperDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPaper" @click="savePaper">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="smartDialogVisible" title="智能组卷" width="520px">
      <el-form :model="smartForm" label-width="86px">
        <el-form-item label="题型">
          <el-select v-model="smartForm.type" clearable placeholder="不限题型">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTIPLE" />
            <el-option label="判断题" value="JUDGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="smartForm.difficulty" clearable placeholder="不限难度">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点"><el-input v-model="smartForm.knowledgePoint" clearable /></el-form-item>
        <el-form-item label="题数"><el-input-number v-model="smartForm.count" :min="1" :max="200" /></el-form-item>
        <el-form-item label="每题分值"><el-input-number v-model="smartForm.score" :min="1" :max="100" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="smartDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="smartSaving" @click="runSmartAssemble">生成并加入试卷</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, DocumentAdd, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { getCourses } from '@/api/course'
import { getExperiments } from '@/api/experiment'
import { createQuestion, deleteQuestion, getQuestions, updateQuestion } from '@/api/question'
import {
  addPaperQuestions,
  createExamPaper,
  deleteExamPaper,
  getExamPaperDetail,
  getExamPapers,
  removePaperQuestion,
  smartAssemblePaper,
  updateExamPaper,
  updateExamPaperStatus,
} from '@/api/examPaper'

const activeTab = ref('questions')
const route = useRoute()
const router = useRouter()
const courses = ref([])
const experiments = ref([])
const questions = ref([])
const papers = ref([])
const activePaper = ref(null)
const paperQuestions = ref([])
const selectedQuestions = ref([])

const questionLoading = ref(false)
const paperLoading = ref(false)
const savingQuestion = ref(false)
const savingPaper = ref(false)
const smartSaving = ref(false)
const questionDialogVisible = ref(false)
const paperDialogVisible = ref(false)
const smartDialogVisible = ref(false)
const editingQuestion = ref(null)
const editingPaper = ref(null)
const questionAnswerArray = ref([])

const filters = reactive({ courseId: '', experimentId: '', type: '', difficulty: '', knowledgePoint: '', keyword: '' })
const questionForm = reactive(defaultQuestionForm())
const paperForm = reactive(defaultPaperForm())
const smartForm = reactive({ type: '', difficulty: '', knowledgePoint: '', count: 10, score: 5 })
const optionRows = reactive([{ key: 'A', label: '' }, { key: 'B', label: '' }, { key: 'C', label: '' }, { key: 'D', label: '' }])

const scopedCourseId = computed(() => {
  const id = route.params.courseId
  return id ? Number(id) : null
})
const scopedCourseName = computed(() => {
  const course = courses.value.find((item) => Number(item.id) === Number(scopedCourseId.value))
  return course?.courseName || `课堂 ${scopedCourseId.value}`
})
const answerOptions = computed(() => {
  if (questionForm.type === 'JUDGE') {
    return [{ label: '正确', value: 'TRUE' }, { label: '错误', value: 'FALSE' }]
  }
  return optionRows.filter((item) => item.label.trim()).map((item) => ({ label: `${item.key}. ${item.label}`, value: item.key }))
})

onMounted(async () => {
  if (route.query.tab === 'papers') activeTab.value = 'papers'
  await loadCourses()
  await reloadAll()
  if (route.query.create === 'paper') openPaperCreate()
})

async function loadCourses() {
  const result = await getCourses({ pageNum: 1, pageSize: 100 })
  courses.value = result?.records || result || []
  if (scopedCourseId.value) filters.courseId = scopedCourseId.value
  else if (!filters.courseId && courses.value[0]) filters.courseId = courses.value[0].id
  await loadExperiments()
}

function activeCourseId() {
  return scopedCourseId.value || filters.courseId || questionForm.courseId || paperForm.courseId || ''
}

async function loadExperiments(targetCourseId) {
  const courseId = targetCourseId || activeCourseId()
  if (!courseId) {
    experiments.value = []
    return
  }
  const result = await getExperiments({ courseId, pageNum: 1, pageSize: 100 })
  experiments.value = result?.records || result || []
}

async function handleCourseChange() {
  if (scopedCourseId.value) {
    filters.courseId = scopedCourseId.value
    return
  }
  filters.experimentId = ''
  await loadExperiments()
  await reloadAll()
}

async function reloadAll() {
  await Promise.allSettled([loadQuestions(), loadPapers()])
}

async function handleTabChange() {
  if (activeTab.value === 'questions') await loadQuestions()
  if (activeTab.value === 'papers') await loadPapers()
}

async function loadQuestions() {
  questionLoading.value = true
  try {
    const result = await getQuestions({ ...cleanParams(filters), pageNum: 1, pageSize: 100 })
    questions.value = result?.records || []
  } finally {
    questionLoading.value = false
  }
}

async function loadPapers() {
  paperLoading.value = true
  try {
    const result = await getExamPapers({ courseId: filters.courseId || undefined, pageNum: 1, pageSize: 100 })
    papers.value = result?.records || []
    if (activePaper.value) {
      const refreshed = papers.value.find((item) => Number(item.id) === Number(activePaper.value.id))
      if (refreshed) await selectPaper(refreshed)
    }
  } finally {
    paperLoading.value = false
  }
}

async function selectPaper(row) {
  activePaper.value = row
  if (!row) {
    paperQuestions.value = []
    return
  }
  const detail = await getExamPaperDetail(row.id)
  activePaper.value = detail?.paper || row
  paperQuestions.value = detail?.questions || []
}

function openQuestionCreate() {
  editingQuestion.value = null
  Object.assign(questionForm, defaultQuestionForm(), { courseId: activeCourseId() || courses.value[0]?.id || '' })
  loadExperiments(questionForm.courseId)
  setOptions([])
  questionAnswerArray.value = []
  questionDialogVisible.value = true
}

function openQuestionEdit(row) {
  editingQuestion.value = row
  Object.assign(questionForm, {
    type: row.type || 'SINGLE',
    content: row.content || '',
    options: row.options || '',
    answer: row.answer || '',
    score: row.score || 5,
    analysis: row.analysis || '',
    knowledgePoint: row.knowledgePoint || '',
    difficulty: row.difficulty || 'MEDIUM',
    courseId: scopedCourseId.value || row.courseId || filters.courseId,
    experimentId: row.experimentId || '',
  })
  loadExperiments(questionForm.courseId)
  setOptions(parseOptions(row.options))
  questionAnswerArray.value = questionForm.type === 'MULTIPLE' ? String(row.answer || '').split(',').filter(Boolean) : []
  questionDialogVisible.value = true
}

async function saveQuestion() {
  if (scopedCourseId.value) questionForm.courseId = scopedCourseId.value
  if (!questionForm.courseId || !questionForm.content.trim() || !questionForm.knowledgePoint.trim()) {
    ElMessage.warning('请填写课堂、题干和知识点')
    return
  }
  const payload = {
    ...questionForm,
    options: questionForm.type === 'JUDGE' ? JSON.stringify([{ key: 'TRUE', label: '正确' }, { key: 'FALSE', label: '错误' }]) : JSON.stringify(optionRows.filter((item) => item.label.trim())),
    answer: questionForm.type === 'MULTIPLE' ? questionAnswerArray.value.join(',') : questionForm.answer,
    score: Number(questionForm.score || 5),
  }
  if (!payload.answer) {
    ElMessage.warning('请选择正确答案')
    return
  }
  savingQuestion.value = true
  try {
    if (editingQuestion.value) {
      await updateQuestion(editingQuestion.value.id, payload)
      ElMessage.success('题目已更新')
    } else {
      await createQuestion(payload)
      ElMessage.success('题目已创建')
    }
    questionDialogVisible.value = false
    await loadQuestions()
  } finally {
    savingQuestion.value = false
  }
}

async function removeQuestion(row) {
  await ElMessageBox.confirm(`确认删除题目“${row.content}”吗？`, '删除题目', { type: 'warning' })
  await deleteQuestion(row.id)
  ElMessage.success('题目已删除')
  await loadQuestions()
}

function openPaperCreate() {
  editingPaper.value = null
  Object.assign(paperForm, defaultPaperForm(), { courseId: activeCourseId() || courses.value[0]?.id || '' })
  loadExperiments(paperForm.courseId)
  paperDialogVisible.value = true
}

function openPaperEdit(row) {
  editingPaper.value = row
  Object.assign(paperForm, { ...defaultPaperForm(), ...row, courseId: scopedCourseId.value || row.courseId || filters.courseId })
  loadExperiments(paperForm.courseId)
  paperDialogVisible.value = true
}

async function savePaper() {
  if (scopedCourseId.value) paperForm.courseId = scopedCourseId.value
  if (!paperForm.title.trim() || !paperForm.courseId) {
    ElMessage.warning('请填写试卷标题和关联课堂')
    return
  }
  savingPaper.value = true
  try {
    const payload = { ...paperForm, courseId: Number(paperForm.courseId), experimentId: paperForm.experimentId || undefined }
    if (editingPaper.value) {
      await updateExamPaper(editingPaper.value.id, payload)
      ElMessage.success('试卷已更新')
    } else {
      const created = await createExamPaper(payload)
      ElMessage.success('试卷已创建')
      if (created?.id) activePaper.value = { id: created.id, ...payload, status: 'DRAFT' }
    }
    paperDialogVisible.value = false
    await loadPapers()
  } finally {
    savingPaper.value = false
  }
}

async function togglePaper(row) {
  const status = row.status === 'PUBLISHED' ? 'CLOSED' : 'PUBLISHED'
  await updateExamPaperStatus(row.id, status)
  ElMessage.success(status === 'PUBLISHED' ? '试卷已发布' : '试卷已关闭')
  await loadPapers()
}

async function addSelectedToPaper() {
  const ids = selectedQuestions.value.map((item) => item.id)
  const scores = selectedQuestions.value.map((item) => Number(item.score || 5))
  await addPaperQuestions(activePaper.value.id, { questionIds: ids, scores })
  ElMessage.success('已加入当前试卷')
  await selectPaper(activePaper.value)
}

async function removeFromPaper(row) {
  await removePaperQuestion(activePaper.value.id, row.question?.id)
  ElMessage.success('题目已移除')
  await selectPaper(activePaper.value)
}

async function runSmartAssemble() {
  smartSaving.value = true
  try {
    const result = await smartAssemblePaper(activePaper.value.id, cleanParams(smartForm))
    ElMessage.success(`已加入 ${result?.actualCount || 0} 道题`)
    smartDialogVisible.value = false
    await selectPaper(activePaper.value)
  } finally {
    smartSaving.value = false
  }
}

function resetQuestionAnswer() {
  questionForm.answer = ''
  questionAnswerArray.value = []
}

function setOptions(options) {
  optionRows.forEach((item, index) => {
    item.label = options[index]?.label || ''
  })
}

function parseOptions(raw) {
  if (!raw) return []
  try {
    return JSON.parse(raw)
  } catch {
    return []
  }
}

function cleanParams(source) {
  return Object.fromEntries(Object.entries(source).filter(([, value]) => value !== '' && value !== null && value !== undefined))
}

function defaultQuestionForm() {
  return { type: 'SINGLE', content: '', options: '', answer: '', score: 5, analysis: '', knowledgePoint: '', difficulty: 'MEDIUM', courseId: '', experimentId: '' }
}

function defaultPaperForm() {
  return { title: '', description: '', courseId: '', experimentId: '', totalScore: 100, passScore: 60, duration: 30, attemptLimit: 1, showAnswerAfterSubmit: 1, admissionValidityDays: 180, multipleScorePolicy: 'ALL_OR_NOTHING', randomEnabled: 0, randomCount: 0 }
}

function typeLabel(type) {
  return { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断' }[type] || type || '-'
}

function difficultyLabel(value) {
  return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[value] || value || '-'
}

function difficultyTag(value) {
  return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[value] || 'info'
}

function paperStatusLabel(value) {
  return { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已关闭' }[value] || value || '-'
}

function paperStatusTag(value) {
  return { DRAFT: 'info', PUBLISHED: 'success', CLOSED: 'warning' }[value] || 'info'
}
</script>

<style scoped>
.exam-manager {
  display: grid;
  gap: 16px;
  height: calc(100vh - 104px);
  min-height: 680px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e4eaee;
  border-radius: 8px;
}

.page-head h1,
.paper-detail h2 {
  margin: 0;
  color: #102033;
}

.page-head p,
.paper-detail p {
  margin: 6px 0 0;
  color: #667085;
}

.head-actions,
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.workspace {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  min-height: 0;
}

.filter-pane,
.content-pane,
.paper-list,
.paper-detail {
  min-height: 0;
  overflow: auto;
  background: #fff;
  border: 1px solid #e4eaee;
  border-radius: 8px;
}

.filter-pane {
  padding: 16px;
}

.filter-pane h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.content-pane {
  padding: 14px;
}

.exam-tabs {
  min-height: 100%;
}

.toolbar,
.detail-head {
  justify-content: space-between;
  margin-bottom: 12px;
}

.paper-layout {
  display: grid;
  grid-template-columns: minmax(420px, 0.95fr) minmax(420px, 1.05fr);
  gap: 14px;
  height: calc(100vh - 245px);
  min-height: 470px;
}

.paper-list,
.paper-detail {
  padding: 14px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.form-grid,
.option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
}

@media (max-width: 1100px) {
  .exam-manager {
    height: auto;
  }

  .workspace,
  .paper-layout {
    grid-template-columns: 1fr;
    height: auto;
  }
}
</style>
