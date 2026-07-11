<template>
  <div class="exam-review-page">
    <header class="review-header">
      <div>
        <el-button text :icon="Back" @click="goBack">返回考试记录</el-button>
        <h1>{{ reviewTitle }}</h1>
        <p>得分 {{ record?.totalScore ?? '-' }} · {{ examStatus(record?.status).label }}</p>
      </div>
      <el-tag :type="resultMeta.type">{{ resultMeta.label }}</el-tag>
    </header>

    <main v-loading="loading" class="review-content">
      <el-empty v-if="!loading && answers.length === 0" description="暂无答题明细" />
      <article
        v-for="(item, index) in answers"
        :key="item.question?.id || item.questionId || index"
        class="question-review"
        :class="{ right: answerCorrect(item), wrong: answerWrong(item) }"
      >
        <div class="question-title">
          <div>
            <span class="question-index">第 {{ index + 1 }} 题</span>
            <h2>{{ item.question?.content || '题目内容缺失' }}</h2>
          </div>
          <el-tag :type="answerCorrect(item) ? 'success' : answerWrong(item) ? 'danger' : 'info'">
            {{ answerCorrect(item) ? '正确' : answerWrong(item) ? '错误' : '待批改' }}
          </el-tag>
        </div>

        <template v-if="answerWrong(item)">
          <div class="answer-compare">
            <p><span>我的答案</span>{{ formatAnswer(item, item.studentAnswer) }}</p>
            <p><span>正确答案</span>{{ formatAnswer(item, item.correctAnswer) }}</p>
          </div>
          <div class="analysis-box">
            <span>错题解析</span>
            <p>{{ item.analysis || '暂无解析' }}</p>
          </div>
          <div v-if="item.answerId && item.correctAnswer !== null && item.correctAnswer !== undefined" class="ai-diagnosis">
            <el-button
              type="primary"
              plain
              :loading="aiExplainLoading[item.answerId]"
              @click="explainWrong(item)"
            >AI 分析错误原因</el-button>
            <div v-if="aiExplainResults[item.answerId]" class="ai-result">
              <el-alert v-if="aiExplainResults[item.answerId].fallback" title="本地知识库降级诊断" type="warning" :closable="false" />
              <p><b>认知偏差：</b>{{ aiExplainResults[item.answerId].misconception }}</p>
              <p><b>错误原因：</b>{{ aiExplainResults[item.answerId].whyWrong }}</p>
              <p><b>风险后果：</b>{{ aiExplainResults[item.answerId].riskConsequence }}</p>
              <div v-if="aiExplainResults[item.answerId].correctReasoning?.length">
                <b>正确推理过程</b>
                <ul><li v-for="point in aiExplainResults[item.answerId].correctReasoning" :key="point">{{ point }}</li></ul>
              </div>
              <div v-if="aiExplainResults[item.answerId].reviewPlan?.length">
                <b>复习计划</b>
                <ul><li v-for="point in aiExplainResults[item.answerId].reviewPlan" :key="point">{{ point }}</li></ul>
              </div>
              <div v-if="aiExplainResults[item.answerId].sources?.length">
                <b>推荐资源</b>
                <ul><li v-for="source in aiExplainResults[item.answerId].sources" :key="source.resourceId">{{ source.title }}</li></ul>
              </div>
              <small>{{ aiExplainResults[item.answerId].disclaimer }}</small>
            </div>
          </div>
        </template>
        <div v-if="item.gradingComment" class="analysis-box">
          <span>教师批注</span>
          <p>{{ item.gradingComment }}</p>
        </div>
      </article>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import { getExamRecordDetail } from '@/api/exam'
import { explainExamAnswer } from '@/api/ai'

const route = useRoute()
const router = useRouter()
const courseId = computed(() => Number(route.params.courseId))
const recordId = computed(() => Number(route.params.recordId))

const loading = ref(false)
const detail = ref(null)
const aiExplainLoading = reactive({})
const aiExplainResults = reactive({})

const record = computed(() => detail.value?.record || null)
const answers = computed(() => detail.value?.answers || [])
const resultMeta = computed(() => {
  if (record.value?.status === 'PENDING_REVIEW' || record.value?.passed === null || record.value?.passed === undefined) return { label: '待批改', type: 'warning' }
  return record.value.passed ? { label: '通过', type: 'success' } : { label: '未通过', type: 'danger' }
})
const reviewTitle = computed(() => record.value?.paperTitle || `试卷 ${record.value?.paperId || ''}`.trim() || '试卷回看')

onMounted(loadDetail)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getExamRecordDetail(recordId.value)
  } catch {
    ElMessage.error('加载试卷回看失败')
    goBack()
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push(`/classrooms/${courseId.value}/learn?module=exam&examTab=records`)
}

async function explainWrong(item) {
  if (!item?.answerId) return
  aiExplainLoading[item.answerId] = true
  try {
    aiExplainResults[item.answerId] = await explainExamAnswer(item.answerId)
  } finally {
    aiExplainLoading[item.answerId] = false
  }
}

function answerCorrect(item) {
  return item?.isCorrect === true || item?.isCorrect === 1
}

function answerWrong(item) {
  return item?.isCorrect === false || item?.isCorrect === 0
}

function formatAnswer(item, answer) {
  if (answer === null || answer === undefined || answer === '') return '未作答'
  const optionMap = optionLabelMap(item?.question?.options)
  return String(answer)
    .split(',')
    .map((value) => {
      const key = value.trim()
      return optionMap[key] ? `${key}. ${optionMap[key]}` : key
    })
    .join('，')
}

function optionLabelMap(options) {
  if (!options) return {}
  try {
    const parsed = typeof options === 'string' ? JSON.parse(options) : options
    if (Array.isArray(parsed)) {
      return parsed.reduce((acc, item, index) => {
        const key = item?.key || item?.value || String.fromCharCode(65 + index)
        acc[key] = item?.label || item?.text || item?.content || ''
        return acc
      }, {})
    }
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function examStatus(status) {
  return {
    IN_PROGRESS: { label: '进行中', type: 'warning' },
    PENDING_REVIEW: { label: '待批改', type: 'primary' },
    GRADED: { label: '已评分', type: 'success' },
    EXPIRED: { label: '已超时', type: 'danger' },
    SUBMITTED: { label: '已提交', type: 'primary' },
    REVIEWED: { label: '已复核', type: 'success' },
  }[status] || { label: status || '未知', type: 'info' }
}
</script>

<style scoped>
.exam-review-page { max-width: 1180px; margin: 0 auto; display: grid; gap: 16px; }
.review-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 16px; }
.review-header h1 { margin: 6px 0; color: #13233a; font-size: 24px; }
.review-header p { color: #667085; }
.review-content { min-height: 520px; display: grid; gap: 12px; align-content: start; }
.question-review { display: grid; gap: 12px; background: #fff; border: 1px solid #edf1f5; border-left-width: 4px; border-radius: 8px; padding: 16px; }
.question-review.right { border-left-color: #67c23a; }
.question-review.wrong { border-left-color: #f56c6c; }
.question-title { display: flex; justify-content: space-between; align-items: flex-start; gap: 14px; }
.question-index { color: #667085; font-size: 12px; }
.question-title h2 { margin-top: 6px; color: #13233a; font-size: 17px; line-height: 1.7; }
.answer-compare { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.answer-compare p, .analysis-box { background: #f8fafc; border-radius: 8px; padding: 12px; color: #344054; line-height: 1.7; }
.answer-compare span, .analysis-box span { display: block; color: #667085; font-size: 12px; margin-bottom: 4px; }
.analysis-box p { white-space: pre-wrap; }
.ai-diagnosis { display: grid; justify-items: start; gap: 12px; }
.ai-result { width: 100%; display: grid; gap: 10px; background: #f6f9ff; border: 1px solid #d9e6ff; border-radius: 8px; padding: 14px; color: #344054; line-height: 1.7; }
.ai-result p, .ai-result ul { margin: 0; }
.ai-result ul { padding-left: 22px; }
.ai-result small { color: #667085; }
@media (max-width: 760px) {
  .review-header, .question-title { flex-direction: column; }
  .answer-compare { grid-template-columns: 1fr; }
}
</style>
