<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Exam Papers</p>
        <h1>试卷管理</h1>
        <p class="page-desc">创建安全考试试卷，维护发布状态，并从题库挂接试题。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增试卷</el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="搜索试卷标题" />
        <el-input v-model="filters.courseId" clearable placeholder="课程ID" />
        <el-select v-model="filters.status" clearable placeholder="状态">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadPapers">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="papers" stripe>
        <el-table-column prop="title" label="试卷标题" min-width="180" />
        <el-table-column prop="courseId" label="课程ID" width="90" />
        <el-table-column prop="experimentId" label="实验ID" width="90" />
        <el-table-column prop="questionCount" label="题数" width="80" />
        <el-table-column label="分数" width="120">
          <template #default="{ row }">{{ row.passScore || 0 }} / {{ row.totalScore || 0 }}</template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="paperStatusMeta(row.status).type">{{ paperStatusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button text type="primary" :icon="Connection" @click="openQuestionDialog(row)">组题</el-button>
            <el-button text :type="row.status === 'PUBLISHED' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 'PUBLISHED' ? '关闭' : '发布' }}
            </el-button>
            <el-button text type="danger" :icon="Delete" @click="removePaper(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="pageNum" layout="prev, pager, next" :page-size="10" :total="total" @current-change="loadPapers" />
      </div>
    </section>

    <el-dialog v-model="formVisible" :title="editingPaper ? '编辑试卷' : '新增试卷'" width="660px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="试卷标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="课程ID" required><el-input v-model="form.courseId" /></el-form-item>
        <el-form-item label="实验ID"><el-input v-model="form.experimentId" /></el-form-item>
        <el-form-item label="总分"><el-input-number v-model="form.totalScore" :min="1" :max="500" /></el-form-item>
        <el-form-item label="及格分"><el-input-number v-model="form.passScore" :min="1" :max="500" /></el-form-item>
        <el-form-item label="考试时长"><el-input-number v-model="form.duration" :min="1" :max="300" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePaper">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="questionVisible" :title="`组题：${currentPaper?.title || ''}`" width="860px">
      <div class="toolbar">
        <el-input v-model="questionFilters.keyword" clearable placeholder="搜索题目" />
        <el-select v-model="questionFilters.type" clearable placeholder="题型">
          <el-option label="单选题" value="SINGLE" />
          <el-option label="多选题" value="MULTIPLE" />
          <el-option label="判断题" value="JUDGE" />
          <el-option label="简答题" value="SHORT_ANSWER" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadQuestions">查询题库</el-button>
      </div>
      <el-table ref="questionTableRef" v-loading="questionLoading" :data="questions" stripe @selection-change="selectedQuestions = $event">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="content" label="题目" min-width="260" show-overflow-tooltip />
        <el-table-column prop="type" label="题型" width="110" />
        <el-table-column prop="difficulty" label="难度" width="100" />
        <el-table-column prop="score" label="默认分" width="90" />
        <el-table-column prop="knowledgePoint" label="知识点" min-width="140" />
      </el-table>
      <div class="question-footer">
        <span>已选择 {{ selectedQuestions.length }} 题</span>
        <el-input-number v-model="defaultQuestionScore" :min="1" :max="100" />
      </div>
      <template #footer>
        <el-button @click="questionVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindingQuestions" @click="bindQuestions">加入试卷</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import {
  addPaperQuestions,
  createExamPaper,
  deleteExamPaper,
  getExamPapers,
  updateExamPaper,
  updateExamPaperStatus,
} from '@/api/examPaper'
import { getQuestions } from '@/api/question'

const filters = reactive({ keyword: '', courseId: '', status: '' })
const questionFilters = reactive({ keyword: '', type: '' })
const form = reactive({
  title: '',
  description: '',
  courseId: '',
  experimentId: '',
  totalScore: 100,
  passScore: 60,
  duration: 60,
  status: 'DRAFT',
})
const papers = ref([])
const questions = ref([])
const selectedQuestions = ref([])
const currentPaper = ref(null)
const editingPaper = ref(null)
const pageNum = ref(1)
const total = ref(0)
const defaultQuestionScore = ref(5)
const loading = ref(false)
const saving = ref(false)
const questionLoading = ref(false)
const bindingQuestions = ref(false)
const formVisible = ref(false)
const questionVisible = ref(false)

onMounted(loadPapers)

async function loadPapers() {
  loading.value = true
  try {
    const result = await getExamPapers({
      pageNum: pageNum.value,
      pageSize: 10,
      keyword: filters.keyword || undefined,
      courseId: filters.courseId || undefined,
      status: filters.status || undefined,
    })
    papers.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingPaper.value = null
  Object.assign(form, { title: '', description: '', courseId: '', experimentId: '', totalScore: 100, passScore: 60, duration: 60, status: 'DRAFT' })
  formVisible.value = true
}

function openEdit(row) {
  editingPaper.value = row
  Object.assign(form, {
    title: row.title || '',
    description: row.description || '',
    courseId: row.courseId || '',
    experimentId: row.experimentId || '',
    totalScore: row.totalScore || 100,
    passScore: row.passScore || 60,
    duration: row.duration || 60,
    status: row.status || 'DRAFT',
  })
  formVisible.value = true
}

async function savePaper() {
  if (!form.title.trim() || !form.courseId) {
    ElMessage.warning('请填写试卷标题和课程ID')
    return
  }
  const payload = {
    title: form.title.trim(),
    description: form.description || undefined,
    courseId: Number(form.courseId),
    experimentId: form.experimentId ? Number(form.experimentId) : undefined,
    totalScore: Number(form.totalScore),
    passScore: Number(form.passScore),
    duration: Number(form.duration),
    status: form.status,
  }
  saving.value = true
  try {
    if (editingPaper.value) {
      await updateExamPaper(editingPaper.value.id, payload)
      ElMessage.success('试卷已更新')
    } else {
      await createExamPaper(payload)
      ElMessage.success('试卷已创建')
    }
    formVisible.value = false
    await loadPapers()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const status = row.status === 'PUBLISHED' ? 'CLOSED' : 'PUBLISHED'
  await updateExamPaperStatus(row.id, status)
  ElMessage.success(status === 'PUBLISHED' ? '试卷已发布' : '试卷已关闭')
  await loadPapers()
}

async function removePaper(row) {
  await ElMessageBox.confirm(`确认删除试卷「${row.title}」吗？`, '删除试卷', { type: 'warning' })
  await deleteExamPaper(row.id)
  ElMessage.success('试卷已删除')
  await loadPapers()
}

async function openQuestionDialog(row) {
  currentPaper.value = row
  selectedQuestions.value = []
  questionVisible.value = true
  await loadQuestions()
}

async function loadQuestions() {
  questionLoading.value = true
  try {
    const result = await getQuestions({
      pageNum: 1,
      pageSize: 50,
      keyword: questionFilters.keyword || undefined,
      type: questionFilters.type || undefined,
      courseId: currentPaper.value?.courseId || undefined,
    })
    questions.value = result?.records || []
  } finally {
    questionLoading.value = false
  }
}

async function bindQuestions() {
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请选择题目')
    return
  }
  bindingQuestions.value = true
  try {
    await addPaperQuestions(currentPaper.value.id, {
      questionIds: selectedQuestions.value.map((item) => Number(item.id)),
      scores: selectedQuestions.value.map((item) => Number(item.score || defaultQuestionScore.value)),
    })
    ElMessage.success('题目已加入试卷')
    questionVisible.value = false
    await loadPapers()
  } finally {
    bindingQuestions.value = false
  }
}

function paperStatusMeta(status) {
  return {
    DRAFT: { label: '草稿', type: 'info' },
    PUBLISHED: { label: '已发布', type: 'success' },
    CLOSED: { label: '已关闭', type: 'warning' },
  }[status] || { label: status || '未知', type: 'info' }
}
</script>

<style scoped>
.business-page { max-width: 1240px; margin: 0 auto; }
.page-head, .pagination-row, .question-footer { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.page-head { align-items: flex-end; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 14px; }
.toolbar .el-input, .toolbar .el-select { max-width: 220px; }
.pagination-row, .question-footer { color: #667085; padding-top: 14px; }
@media (max-width: 720px) {
  .page-head, .toolbar, .pagination-row, .question-footer { align-items: stretch; flex-direction: column; }
  .toolbar .el-input, .toolbar .el-select { max-width: none; }
}
</style>
