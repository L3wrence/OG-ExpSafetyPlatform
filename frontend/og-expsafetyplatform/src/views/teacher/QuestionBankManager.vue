<template>
  <div class="question-page">
    <header class="page-head">
      <div><h1>题库管理</h1><p>{{ courseName }}课堂题库，仅维护单选、多选和判断题。</p></div>
      <div class="head-actions">
        <el-button :icon="Back" @click="router.push(`/teacher/courses/${courseId}/edit?step=exam`)">返回考试管理</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增题目</el-button>
      </div>
    </header>

    <section class="content-panel">
      <div class="filters">
        <el-select v-model="filters.experimentId" clearable placeholder="实验">
          <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
        </el-select>
        <el-select v-model="filters.type" clearable placeholder="题型">
          <el-option label="单选题" value="SINGLE" /><el-option label="多选题" value="MULTIPLE" /><el-option label="判断题" value="JUDGE" />
        </el-select>
        <el-select v-model="filters.difficulty" clearable placeholder="难度">
          <el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" />
        </el-select>
        <el-input v-model="filters.knowledgePoint" clearable placeholder="知识点" />
        <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索题干" @keyup.enter="loadQuestions" />
        <el-button type="primary" :icon="Search" @click="loadQuestions">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="questions" stripe>
        <el-table-column label="题型" width="90"><template #default="{ row }">{{ typeLabel(row.type) }}</template></el-table-column>
        <el-table-column prop="content" label="题干" min-width="280" show-overflow-tooltip />
        <el-table-column prop="knowledgePoint" label="知识点" width="150" />
        <el-table-column label="难度" width="90"><template #default="{ row }"><el-tag :type="difficultyTag(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag></template></el-table-column>
        <el-table-column prop="score" label="默认分值" width="100" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }"><el-button text type="primary" @click="openEdit(row)">编辑</el-button><el-button text type="danger" @click="removeQuestion(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑题目' : '新增题目'" width="720px">
      <el-form :model="form" label-width="88px">
        <el-form-item label="关联课堂"><el-input :model-value="courseName" disabled /></el-form-item>
        <el-form-item label="关联实验"><el-select v-model="form.experimentId" clearable placeholder="可选"><el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="题型" required>
          <el-radio-group v-model="form.type" @change="resetAnswer"><el-radio-button label="SINGLE">单选</el-radio-button><el-radio-button label="MULTIPLE">多选</el-radio-button><el-radio-button label="JUDGE">判断</el-radio-button></el-radio-group>
        </el-form-item>
        <el-form-item label="题干" required><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <div v-if="form.type !== 'JUDGE'" class="form-grid">
          <el-form-item v-for="option in optionRows" :key="option.key" :label="`选项${option.key}`"><el-input v-model="option.label" /></el-form-item>
        </div>
        <el-form-item label="正确答案" required>
          <el-select v-if="form.type === 'MULTIPLE'" v-model="multipleAnswers" multiple placeholder="选择正确选项"><el-option v-for="item in answerOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
          <el-select v-else v-model="form.answer" placeholder="选择正确答案"><el-option v-for="item in answerOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="知识点" required><el-input v-model="form.knowledgePoint" /></el-form-item>
          <el-form-item label="难度" required><el-select v-model="form.difficulty"><el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" /></el-select></el-form-item>
          <el-form-item label="默认分值" required><el-input-number v-model="form.score" :min="1" :max="100" /></el-form-item>
        </div>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveQuestion">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Plus, Search } from '@element-plus/icons-vue'
import { getCourseDetail } from '@/api/course'
import { getExperiments } from '@/api/experiment'
import { createQuestion, deleteQuestion, getQuestions, updateQuestion } from '@/api/question'

const route = useRoute()
const router = useRouter()
const courseId = computed(() => Number(route.params.courseId))
const courseName = ref('')
const experiments = ref([])
const questions = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const multipleAnswers = ref([])
const filters = reactive({ experimentId: '', type: '', difficulty: '', knowledgePoint: '', keyword: '' })
const form = reactive(defaultForm())
const optionRows = reactive([{ key: 'A', label: '' }, { key: 'B', label: '' }, { key: 'C', label: '' }, { key: 'D', label: '' }])
const answerOptions = computed(() => form.type === 'JUDGE'
  ? [{ label: '正确', value: 'TRUE' }, { label: '错误', value: 'FALSE' }]
  : optionRows.filter((item) => item.label.trim()).map((item) => ({ label: `${item.key}. ${item.label}`, value: item.key })))

onMounted(async () => {
  const [detail, experimentResult] = await Promise.all([getCourseDetail(courseId.value), getExperiments({ courseId: courseId.value, pageNum: 1, pageSize: 100 })])
  courseName.value = detail?.course?.courseName || `课堂 ${courseId.value}`
  experiments.value = experimentResult?.records || experimentResult || []
  await loadQuestions()
})

async function loadQuestions() {
  loading.value = true
  try {
    const params = Object.fromEntries(Object.entries({ ...filters, courseId: courseId.value, pageNum: 1, pageSize: 100 }).filter(([, value]) => value !== ''))
    const result = await getQuestions(params)
    questions.value = (result?.records || []).filter((item) => ['SINGLE', 'MULTIPLE', 'JUDGE'].includes(item.type))
  } finally { loading.value = false }
}

function openCreate() { editing.value = null; Object.assign(form, defaultForm(), { courseId: courseId.value }); setOptions([]); multipleAnswers.value = []; dialogVisible.value = true }
function openEdit(row) {
  editing.value = row
  Object.assign(form, { ...defaultForm(), ...row, courseId: courseId.value, experimentId: row.experimentId || '' })
  setOptions(parseOptions(row.options)); multipleAnswers.value = row.type === 'MULTIPLE' ? String(row.answer || '').split(',').filter(Boolean) : []
  dialogVisible.value = true
}
async function saveQuestion() {
  if (!form.content.trim() || !form.knowledgePoint.trim()) return ElMessage.warning('请填写题干和知识点')
  const answer = form.type === 'MULTIPLE' ? multipleAnswers.value.join(',') : form.answer
  if (!answer) return ElMessage.warning('请选择正确答案')
  const payload = { ...form, courseId: courseId.value, experimentId: form.experimentId || undefined, answer, score: Number(form.score), options: form.type === 'JUDGE' ? JSON.stringify([{ key: 'TRUE', label: '正确' }, { key: 'FALSE', label: '错误' }]) : JSON.stringify(optionRows.filter((item) => item.label.trim())) }
  saving.value = true
  try {
    if (editing.value) await updateQuestion(editing.value.id, payload); else await createQuestion(payload)
    ElMessage.success(editing.value ? '题目已更新' : '题目已创建'); dialogVisible.value = false; await loadQuestions()
  } finally { saving.value = false }
}
async function removeQuestion(row) { await ElMessageBox.confirm(`确认删除题目“${row.content}”吗？`, '删除题目', { type: 'warning' }); await deleteQuestion(row.id); ElMessage.success('题目已删除'); await loadQuestions() }
function resetAnswer() { form.answer = ''; multipleAnswers.value = [] }
function setOptions(options) { optionRows.forEach((item, index) => { item.label = options[index]?.label || '' }) }
function parseOptions(raw) { try { return raw ? JSON.parse(raw) : [] } catch { return [] } }
function defaultForm() { return { type: 'SINGLE', content: '', options: '', answer: '', score: 5, analysis: '', knowledgePoint: '', difficulty: 'MEDIUM', courseId: '', experimentId: '' } }
function typeLabel(value) { return { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断' }[value] || value }
function difficultyLabel(value) { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[value] || value }
function difficultyTag(value) { return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[value] || 'info' }
</script>

<style scoped>
.question-page { display: grid; gap: 16px; min-height: calc(100vh - 104px); }
.page-head, .content-panel { padding: 20px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; }
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-head h1 { margin: 0; font-size: 22px; color: #102a43; letter-spacing: 0; }.page-head p { margin: 6px 0 0; color: #64748b; }
.head-actions, .filters { display: flex; align-items: center; gap: 10px; }.filters { margin-bottom: 18px; flex-wrap: wrap; }.filters .el-select { width: 150px; }.filters .el-input { width: 190px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
@media (max-width: 760px) { .page-head, .head-actions { align-items: stretch; flex-direction: column; }.filters .el-select, .filters .el-input { width: 100%; }.form-grid { grid-template-columns: 1fr; } }
</style>
