<template>
  <div v-loading="loading" class="editor-page">
    <header class="page-head">
      <div><h1>{{ paper?.title || '试卷编辑' }}</h1><p>{{ paper?.description || '配置试卷题目与分值结构。' }}</p></div>
      <div class="head-actions">
        <el-button :icon="Back" @click="router.push(`/teacher/courses/${courseId}/edit?step=exam`)">返回试卷列表</el-button>
        <el-button :icon="Setting" @click="basicDialogVisible = true">基本信息</el-button>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </header>

    <section v-if="paper" class="score-panel">
      <div class="score-summary">
        <span>试卷总分<strong>{{ paper.totalScore }}</strong></span>
        <span :class="{ exceeded: objectiveAssigned > scorePlan.objectiveScore }">客观题<strong>{{ objectiveAssigned }}/{{ scorePlan.objectiveScore }}</strong></span>
        <span :class="{ exceeded: subjectiveAssigned > scorePlan.subjectiveScore }">主观题<strong>{{ subjectiveAssigned }}/{{ scorePlan.subjectiveScore }}</strong></span>
        <el-tag v-if="scorePlanChanged" type="warning">分值设置待保存</el-tag>
      </div>
      <el-form :inline="true" class="score-form">
        <el-form-item label="客观题分数"><el-input-number v-model="scorePlan.objectiveScore" :min="0" :max="paper.totalScore" /></el-form-item>
        <el-form-item label="主观题分数"><el-input-number v-model="scorePlan.subjectiveScore" :min="0" :max="paper.totalScore" /></el-form-item>
        <el-button type="primary" :loading="scoreSaving" @click="saveScorePlan">保存分值设置</el-button>
      </el-form>
    </section>

    <section class="question-section">
      <div class="section-head"><div><h2>从课堂题库选择客观题</h2><p>题目加入试卷后沿用题库默认分值，不能单独修改。</p></div><el-button type="success" plain @click="openSmart">随机抽题</el-button></div>
      <div class="filters">
        <el-select v-model="filters.experimentId" clearable placeholder="实验"><el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" /></el-select>
        <el-select v-model="filters.type" clearable placeholder="题型"><el-option label="单选" value="SINGLE" /><el-option label="多选" value="MULTIPLE" /><el-option label="判断" value="JUDGE" /></el-select>
        <el-select v-model="filters.difficulty" clearable placeholder="难度"><el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" /></el-select>
        <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索题干" @keyup.enter="loadBankQuestions" />
        <el-button :icon="Search" @click="loadBankQuestions">查询</el-button>
        <span class="selected-score">已选 {{ selectedQuestions.length }} 题，共 {{ selectedScore }} 分</span>
        <el-button type="primary" :disabled="!selectedQuestions.length" @click="addSelected">加入试卷</el-button>
      </div>
      <el-table v-loading="bankLoading" :data="availableBankQuestions" row-key="id" max-height="300" @selection-change="selectedQuestions = $event">
        <el-table-column type="selection" width="46" /><el-table-column label="题型" width="90"><template #default="{ row }">{{ typeLabel(row.type) }}</template></el-table-column>
        <el-table-column prop="content" label="题干" min-width="260" show-overflow-tooltip /><el-table-column prop="knowledgePoint" label="知识点" width="140" /><el-table-column prop="score" label="题库分值" width="100" />
      </el-table>
    </section>

    <section class="question-section">
      <div class="section-head"><div><h2>试卷题目</h2><p>客观题分值来自题库；主观题由教师创建并设置分值。</p></div><el-button type="warning" plain :icon="Plus" @click="openSubjective">新增主观题</el-button></div>
      <el-table :data="paperQuestions" stripe empty-text="暂无题目">
        <el-table-column prop="orderNum" label="#" width="60" /><el-table-column label="题型" width="100"><template #default="{ row }">{{ typeLabel(row.question?.type) }}</template></el-table-column>
        <el-table-column label="题干" min-width="300" show-overflow-tooltip><template #default="{ row }">{{ row.question?.content }}</template></el-table-column>
        <el-table-column label="来源" width="100"><template #default="{ row }">{{ isSubjective(row) ? '教师自定义' : '课堂题库' }}</template></el-table-column>
        <el-table-column label="分值" width="90"><template #default="{ row }"><strong>{{ row.score }}</strong></template></el-table-column>
        <el-table-column label="操作" width="90"><template #default="{ row }"><el-button text type="danger" @click="removeQuestion(row)">移除</el-button></template></el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="subjectiveDialogVisible" title="新增主观题" width="680px">
      <el-form :model="subjectiveForm" label-width="96px">
        <el-form-item label="题干" required><el-input v-model="subjectiveForm.content" type="textarea" :rows="4" /></el-form-item>
        <div class="form-grid"><el-form-item label="分值" required><el-input-number v-model="subjectiveForm.score" :min="1" :max="Math.max(1, subjectiveRemaining)" /></el-form-item><el-form-item label="知识点"><el-input v-model="subjectiveForm.knowledgePoint" /></el-form-item></div>
        <el-form-item label="参考答案"><el-input v-model="subjectiveForm.answer" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="批改要点"><el-input v-model="subjectiveForm.analysis" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="subjectiveDialogVisible = false">取消</el-button><el-button type="primary" :loading="subjectiveSaving" @click="saveSubjective">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="smartDialogVisible" title="随机抽取客观题" width="520px">
      <el-form :model="smartForm" label-width="90px">
        <el-form-item label="题型"><el-select v-model="smartForm.type" clearable placeholder="不限"><el-option label="单选" value="SINGLE" /><el-option label="多选" value="MULTIPLE" /><el-option label="判断" value="JUDGE" /></el-select></el-form-item>
        <el-form-item label="难度"><el-select v-model="smartForm.difficulty" clearable placeholder="不限"><el-option label="简单" value="EASY" /><el-option label="中等" value="MEDIUM" /><el-option label="困难" value="HARD" /></el-select></el-form-item>
        <el-form-item label="目标分值"><el-input-number v-model="smartForm.targetScore" :min="1" :max="Math.max(1, objectiveRemaining)" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="smartDialogVisible = false">取消</el-button><el-button type="primary" :loading="smartSaving" @click="smartAssemble">生成并加入</el-button></template>
    </el-dialog>

    <el-dialog v-model="basicDialogVisible" title="编辑试卷基本信息" width="680px">
      <el-form v-if="paper" :model="basicForm" label-width="96px">
        <el-form-item label="试卷标题" required><el-input v-model="basicForm.title" /></el-form-item>
        <el-form-item label="关联实验"><el-select v-model="basicForm.experimentId" clearable><el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" /></el-select></el-form-item>
        <div class="form-grid"><el-form-item label="及格分"><el-input-number v-model="basicForm.passScore" :min="1" :max="paper.totalScore" /></el-form-item><el-form-item label="时长"><el-input-number v-model="basicForm.duration" :min="1" :max="300" /></el-form-item></div>
        <el-form-item label="说明"><el-input v-model="basicForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="basicDialogVisible = false">取消</el-button><el-button type="primary" :loading="basicSaving" @click="saveBasic">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Plus, Refresh, Search, Setting } from '@element-plus/icons-vue'
import { getExperiments } from '@/api/experiment'
import { getQuestions } from '@/api/question'
import { addPaperQuestions, addSubjectiveQuestion, getExamPaperDetail, removePaperQuestion, smartAssemblePaper, updateExamPaper } from '@/api/examPaper'

const route = useRoute(), router = useRouter()
const courseId = computed(() => Number(route.params.courseId)), paperId = computed(() => Number(route.params.paperId))
const paper = ref(null), paperQuestions = ref([]), experiments = ref([]), bankQuestions = ref([]), selectedQuestions = ref([])
const loading = ref(false), bankLoading = ref(false), scoreSaving = ref(false), subjectiveSaving = ref(false), smartSaving = ref(false), basicSaving = ref(false)
const subjectiveDialogVisible = ref(false), smartDialogVisible = ref(false), basicDialogVisible = ref(false)
const filters = reactive({ experimentId: '', type: '', difficulty: '', keyword: '' })
const scorePlan = reactive({ objectiveScore: 0, subjectiveScore: 0 })
const subjectiveForm = reactive(defaultSubjective()), smartForm = reactive({ type: '', difficulty: '', targetScore: 1 }), basicForm = reactive({})
const objectiveQuestions = computed(() => paperQuestions.value.filter((item) => !isSubjective(item)))
const objectiveAssigned = computed(() => objectiveQuestions.value.reduce((sum, item) => sum + Number(item.score || 0), 0))
const subjectiveAssigned = computed(() => paperQuestions.value.filter(isSubjective).reduce((sum, item) => sum + Number(item.score || 0), 0))
const objectiveRemaining = computed(() => Math.max(0, Number(paper.value?.objectiveScore || 0) - objectiveAssigned.value))
const subjectiveRemaining = computed(() => Math.max(0, Number(paper.value?.subjectiveScore || 0) - subjectiveAssigned.value))
const selectedScore = computed(() => selectedQuestions.value.reduce((sum, item) => sum + Number(item.score || 0), 0))
const scorePlanChanged = computed(() => Number(scorePlan.objectiveScore) !== Number(paper.value?.objectiveScore || 0)
  || Number(scorePlan.subjectiveScore) !== Number(paper.value?.subjectiveScore || 0))
const existingIds = computed(() => new Set(paperQuestions.value.map((item) => Number(item.question?.id))))
const availableBankQuestions = computed(() => bankQuestions.value.filter((item) => !existingIds.value.has(Number(item.id))))

onMounted(loadPage)
async function loadPage() {
  loading.value = true
  try {
    const detail = await getExamPaperDetail(paperId.value)
    if (!detail?.paper || Number(detail.paper.courseId) !== courseId.value) { ElMessage.error('试卷不存在或不属于当前课堂'); return router.push(`/teacher/courses/${courseId.value}/edit?step=exam`) }
    paper.value = detail.paper; paperQuestions.value = detail.questions || []
    Object.assign(scorePlan, { objectiveScore: Number(paper.value.objectiveScore || 0), subjectiveScore: Number(paper.value.subjectiveScore || 0) })
    Object.assign(basicForm, paper.value)
    const experimentResult = await getExperiments({ courseId: courseId.value, pageNum: 1, pageSize: 100 })
    experiments.value = experimentResult?.records || experimentResult || []
    await loadBankQuestions()
  } finally { loading.value = false }
}
async function loadBankQuestions() {
  bankLoading.value = true
  try {
    const params = Object.fromEntries(Object.entries({ ...filters, courseId: courseId.value, pageNum: 1, pageSize: 100 }).filter(([, value]) => value !== '') )
    const result = await getQuestions(params); bankQuestions.value = (result?.records || []).filter((item) => ['SINGLE', 'MULTIPLE', 'JUDGE'].includes(item.type)); selectedQuestions.value = []
  } finally { bankLoading.value = false }
}
async function saveScorePlan() {
  const objective = Number(scorePlan.objectiveScore), subjective = Number(scorePlan.subjectiveScore)
  if (objective + subjective !== Number(paper.value.totalScore)) return ElMessage.warning('客观题分数和主观题分数之和必须等于试卷总分')
  if (objectiveAssigned.value > objective) return ElMessage.error('已选客观题分值总和超过设定的客观题分数')
  if (subjectiveAssigned.value > subjective) return ElMessage.error('主观题分值总和超过设定的主观题分数')
  scoreSaving.value = true
  try { await updateExamPaper(paperId.value, paperPayload({ ...paper.value, objectiveScore: objective, subjectiveScore: subjective })); ElMessage.success('分值设置已保存'); await loadPage() } finally { scoreSaving.value = false }
}
async function addSelected() {
  if (objectiveAssigned.value + selectedScore.value > Number(paper.value.objectiveScore)) return ElMessage.error('所选客观题分值总和超过设定的客观题分数')
  await addPaperQuestions(paperId.value, { questionIds: selectedQuestions.value.map((item) => item.id) }); ElMessage.success('客观题已加入试卷'); await loadPage()
}
async function removeQuestion(row) { await removePaperQuestion(paperId.value, row.question?.id); ElMessage.success('题目已移除'); await loadPage() }
function openSubjective() { if (subjectiveRemaining.value <= 0) return ElMessage.warning('主观题分数已配置完毕'); Object.assign(subjectiveForm, defaultSubjective(), { score: Math.min(10, subjectiveRemaining.value) }); subjectiveDialogVisible.value = true }
async function saveSubjective() {
  const score = Number(subjectiveForm.score || 0); if (!subjectiveForm.content.trim()) return ElMessage.warning('请填写主观题题干'); if (score > subjectiveRemaining.value) return ElMessage.error('主观题分值总和不能超过设定的主观题分数')
  subjectiveSaving.value = true
  try { await addSubjectiveQuestion(paperId.value, { ...subjectiveForm, score }); subjectiveDialogVisible.value = false; ElMessage.success('主观题已加入试卷'); await loadPage() } finally { subjectiveSaving.value = false }
}
function openSmart() { if (objectiveRemaining.value <= 0) return ElMessage.warning('客观题分数已配置完毕'); Object.assign(smartForm, { type: '', difficulty: '', targetScore: objectiveRemaining.value }); smartDialogVisible.value = true }
async function smartAssemble() {
  if (Number(smartForm.targetScore) > objectiveRemaining.value) return ElMessage.error('随机抽题目标分值超过客观题剩余分数')
  smartSaving.value = true
  try { const result = await smartAssemblePaper(paperId.value, { ...smartForm, targetScore: Number(smartForm.targetScore) }); smartDialogVisible.value = false; ElMessage.success(`已加入 ${result?.actualCount || 0} 道题，共 ${result?.actualScore || 0} 分`); await loadPage() } finally { smartSaving.value = false }
}
async function saveBasic() { if (!String(basicForm.title || '').trim()) return ElMessage.warning('请填写试卷标题'); basicSaving.value = true; try { await updateExamPaper(paperId.value, paperPayload({ ...paper.value, ...basicForm })); basicDialogVisible.value = false; ElMessage.success('基本信息已保存'); await loadPage() } finally { basicSaving.value = false } }
function paperPayload(source) { return { ...source, courseId: courseId.value, experimentId: source.experimentId || undefined, totalScore: Number(source.totalScore), objectiveScore: Number(source.objectiveScore), subjectiveScore: Number(source.subjectiveScore), passScore: Number(source.passScore), duration: Number(source.duration), attemptLimit: Number(source.attemptLimit || 1) } }
function defaultSubjective() { return { content: '', score: 10, knowledgePoint: '', answer: '', analysis: '' } }
function isSubjective(item) { return item.question?.type === 'SHORT_ANSWER' }
function typeLabel(value) { return { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断', SHORT_ANSWER: '主观题' }[value] || value || '-' }
</script>

<style scoped>
.editor-page { display: grid; gap: 16px; min-height: calc(100vh - 104px); }.page-head,.score-panel,.question-section { padding: 20px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; }
.page-head,.section-head,.score-summary,.score-form,.filters,.head-actions { display: flex; align-items: center; gap: 12px; }.page-head,.section-head { justify-content: space-between; }.page-head h1,.section-head h2 { margin: 0; color: #102a43; letter-spacing: 0; }.page-head h1 { font-size: 22px; }.section-head h2 { font-size: 17px; }.page-head p,.section-head p { margin: 6px 0 0; color: #64748b; }
.score-panel { display: flex; align-items: center; justify-content: space-between; gap: 20px; }.score-summary span { min-width: 110px; color: #64748b; }.score-summary strong { display: block; margin-top: 4px; font-size: 21px; color: #102a43; }.score-summary .exceeded,.score-summary .exceeded strong { color: #c03639; }.score-form { margin: 0; }.score-form :deep(.el-form-item) { margin-bottom: 0; }
.filters { margin: 16px 0; flex-wrap: wrap; }.filters .el-select { width: 130px; }.filters .el-input { width: 220px; }.selected-score { margin-left: auto; color: #8a2f3c; font-weight: 600; }.form-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 0 18px; }
@media (max-width: 900px) { .page-head,.section-head,.score-panel,.score-form,.head-actions { align-items: stretch; flex-direction: column; }.score-summary { flex-wrap: wrap; }.selected-score { margin-left: 0; }.filters .el-select,.filters .el-input { width: 100%; }.form-grid { grid-template-columns: 1fr; } }
</style>
