<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">AI Assistant</p>
        <h1>AI实验助手</h1>
        <p class="page-desc">围绕安全问答、错误解析和报告建议进行辅助学习。</p>
      </div>
      <el-button :icon="Refresh" @click="loadRecords">刷新记录</el-button>
    </section>

    <section class="grid">
      <div class="panel">
        <div class="section-title"><h2>提问</h2></div>
        <el-form :model="askForm" label-position="top">
          <el-form-item label="场景">
            <el-select v-model="askForm.scene">
              <el-option label="安全知识问答" value="SAFETY_QA" />
              <el-option label="错题解析" value="ERROR_EXPLAIN" />
              <el-option label="报告建议" value="REPORT_SUGGEST" />
            </el-select>
          </el-form-item>
          <el-form-item label="实验ID">
            <el-input v-model="askForm.experimentId" clearable placeholder="可选，用于获取实验相关建议" />
          </el-form-item>
          <el-form-item label="问题">
            <el-input v-model="askForm.question" type="textarea" :rows="8" placeholder="请输入需要辅助的问题" />
          </el-form-item>
          <el-button type="primary" :icon="MagicStick" :loading="asking" @click="handleAsk">发送问题</el-button>
        </el-form>
        <div v-if="answer" class="answer-box">
          <strong>AI回复</strong>
          <p>{{ answer }}</p>
        </div>
      </div>

      <div class="panel">
        <div class="section-title">
          <h2>学习推荐</h2>
          <el-button :icon="Refresh" @click="loadRecommendations">刷新</el-button>
        </div>
        <el-input v-model="recommendExperimentId" clearable placeholder="按实验ID获取推荐" class="recommend-input" />
        <div v-loading="recommendLoading" class="recommend-list">
          <div v-for="item in recommendations" :key="item.id || item.resourceId || item.title" class="recommend-item">
            <strong>{{ item.title || item.resourceTitle || '推荐资源' }}</strong>
            <span>{{ item.reason || item.description || '根据学习记录推荐' }}</span>
          </div>
          <el-empty v-if="!recommendLoading && recommendations.length === 0" description="暂无推荐资源" />
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>问答记录</h2>
        <el-select v-model="recordScene" clearable placeholder="场景" @change="loadRecords">
          <el-option label="安全知识问答" value="SAFETY_QA" />
          <el-option label="错题解析" value="ERROR_EXPLAIN" />
          <el-option label="报告建议" value="REPORT_SUGGEST" />
        </el-select>
      </div>
      <el-table v-loading="recordLoading" :data="records" stripe>
        <el-table-column prop="scene" label="场景" width="140" />
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="answer" label="回复" min-width="260" show-overflow-tooltip />
        <el-table-column prop="manualRevision" label="人工修订" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openFeedback(row)">修订</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ recordTotal }} 条记录</span>
        <el-pagination v-model:current-page="recordPage" layout="prev, pager, next" :page-size="10" :total="recordTotal" @current-change="loadRecords" />
      </div>
    </section>

    <el-dialog v-model="feedbackVisible" title="人工修订" width="560px">
      <el-input v-model="manualRevision" type="textarea" :rows="6" placeholder="补充或修订AI回复" />
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackSaving" @click="submitFeedback">保存修订</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Refresh } from '@element-plus/icons-vue'
import { askAi, feedbackAiRecord, getAiRecords, getRecommendedResources } from '@/api/ai'

const askForm = reactive({ scene: 'SAFETY_QA', experimentId: '', question: '' })
const answer = ref('')
const asking = ref(false)
const recordLoading = ref(false)
const recommendLoading = ref(false)
const feedbackSaving = ref(false)
const records = ref([])
const recommendations = ref([])
const recordScene = ref('')
const recordPage = ref(1)
const recordTotal = ref(0)
const recommendExperimentId = ref('')
const feedbackVisible = ref(false)
const currentRecord = ref(null)
const manualRevision = ref('')

watch(recommendExperimentId, () => loadRecommendations())

onMounted(() => Promise.allSettled([loadRecords(), loadRecommendations()]))

async function handleAsk() {
  if (!askForm.question.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  asking.value = true
  try {
    const result = await askAi({
      scene: askForm.scene,
      question: askForm.question.trim(),
      experimentId: askForm.experimentId ? Number(askForm.experimentId) : undefined,
    })
    answer.value = result?.answer || result?.content || String(result || '')
    await loadRecords()
  } finally {
    asking.value = false
  }
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const result = await getAiRecords({
      pageNum: recordPage.value,
      pageSize: 10,
      scene: recordScene.value || undefined,
    })
    records.value = result?.records || []
    recordTotal.value = result?.total || 0
  } finally {
    recordLoading.value = false
  }
}

async function loadRecommendations() {
  recommendLoading.value = true
  try {
    const result = await getRecommendedResources({
      experimentId: recommendExperimentId.value || undefined,
    })
    recommendations.value = Array.isArray(result) ? result : result?.records || []
  } finally {
    recommendLoading.value = false
  }
}

function openFeedback(row) {
  currentRecord.value = row
  manualRevision.value = row.manualRevision || ''
  feedbackVisible.value = true
}

async function submitFeedback() {
  feedbackSaving.value = true
  try {
    await feedbackAiRecord(currentRecord.value.id, { manualRevision: manualRevision.value })
    ElMessage.success('修订已保存')
    feedbackVisible.value = false
    await loadRecords()
  } finally {
    feedbackSaving.value = false
  }
}
</script>

<style scoped>
.business-page { max-width: 1240px; margin: 0 auto; }
.page-head, .section-title, .pagination-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.page-head { align-items: flex-end; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.grid { display: grid; grid-template-columns: minmax(360px, 0.9fr) minmax(0, 1.1fr); gap: 16px; align-items: start; margin-bottom: 16px; }
.panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.section-title { margin-bottom: 12px; }
.section-title h2 { color: #13233a; font-size: 18px; }
.answer-box { margin-top: 14px; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 14px; color: #344054; line-height: 1.7; }
.answer-box strong { display: block; color: #13233a; margin-bottom: 8px; }
.recommend-input { margin-bottom: 12px; }
.recommend-list { min-height: 220px; }
.recommend-item { border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.recommend-item strong { display: block; color: #13233a; margin-bottom: 6px; }
.recommend-item span { color: #667085; font-size: 13px; }
.pagination-row { color: #667085; padding-top: 14px; }
@media (max-width: 980px) { .grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .page-head, .section-title, .pagination-row { align-items: stretch; flex-direction: column; } }
</style>
