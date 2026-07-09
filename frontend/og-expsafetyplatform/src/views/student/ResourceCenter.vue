<template>
  <div class="resource-page">
    <section class="resource-hero" :style="{ backgroundImage: `url(${resourceCore})` }">
      <div>
        <p>油气工程实验教学与考核平台</p>
        <h1>油气资源库</h1>
        <span>像逛知识视频库一样发现实验指导书、微课、事故案例、设备说明和虚拟仿真实验。</span>
      </div>
    </section>

    <section class="page-head">
      <div>
        <p class="eyebrow">Oil & Gas Library</p>
        <h1>教学资源与虚拟实验</h1>
        <p class="page-desc">检索平台资源和用户投稿资源，学习、收藏、记录进度，也可以分享你发现的优质油气工程资源。</p>
      </div>
      <el-button type="primary" @click="openSubmission">投稿资源</el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="filters.keyword" clearable placeholder="关键词、标签、知识点" @keyup.enter="loadResources" />
        <el-input v-model="filters.experimentId" clearable placeholder="实验ID" @keyup.enter="loadResources" />
        <el-select v-model="filters.resourceType" clearable placeholder="资源类型">
          <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-checkbox v-model="filters.favoriteOnly" :true-value="1" :false-value="0">只看收藏</el-checkbox>
        <el-button type="primary" :icon="Search" @click="loadResources">查询</el-button>
      </div>

      <div v-loading="loading" class="resource-list">
        <el-empty v-if="!loading && resources.length === 0" description="暂无可学习资源" />
        <article v-for="resource in resources" :key="resource.id" class="resource-card">
          <div class="resource-main">
            <div class="resource-title">
              <h2>{{ resource.title }}</h2>
              <el-tag :type="resource.sourceType === 'SUBMISSION' ? 'success' : (resource.requiredFlag ? 'warning' : 'info')">
                {{ resource.sourceType === 'SUBMISSION' ? '用户投稿' : (resource.requiredFlag ? '必学' : '拓展') }}
              </el-tag>
            </div>
            <p>{{ resource.description || '暂无资源说明。' }}</p>
            <div class="meta-row">
              <span>{{ typeLabel(resource.resourceType) }}</span>
              <span>{{ resource.knowledgePoint || '未设置知识点' }}</span>
              <span>{{ resource.riskType || '通用风险' }}</span>
              <span v-if="resource.sourceType !== 'SUBMISSION'">查看 {{ resource.viewCount || 0 }}</span>
              <span v-if="resource.sourceType !== 'SUBMISSION'">收藏 {{ resource.favoriteCount || 0 }}</span>
              <span v-if="resource.sourceType !== 'SUBMISSION'">点赞 {{ resource.likeCount || 0 }}</span>
            </div>
            <div class="tag-row">
              <el-tag v-for="tag in splitTags(resource.tags)" :key="tag" size="small">{{ tag }}</el-tag>
            </div>
          </div>

          <div class="study-box">
            <div class="progress-head">
              <span>学习进度</span>
              <strong>{{ progressValue(resource.id) }}%</strong>
            </div>
            <el-slider v-model="progressDrafts[resource.id]" :min="0" :max="100" :step="5" />
            <el-input-number v-model="durationDrafts[resource.id]" :min="0" :step="5" controls-position="right" />
            <span class="hint">本次学习分钟，已累计 {{ durationTotal(resource.id) }} 分钟</span>
            <div class="action-row">
              <el-button :icon="View" @click="preview(resource)">预览</el-button>
              <template v-if="resource.sourceType !== 'SUBMISSION'">
                <el-button :icon="Download" @click="download(resource)">下载</el-button>
                <el-button :icon="Star" @click="favorite(resource)">收藏</el-button>
                <el-button :icon="Pointer" @click="like(resource)">点赞</el-button>
                <el-button type="primary" :icon="Check" @click="saveProgress(resource)">记录进度</el-button>
              </template>
            </div>
          </div>
        </article>
      </div>

      <div class="pagination-row">
        <span>共 {{ total }} 条资源</span>
        <el-pagination v-model:current-page="pageNum" layout="prev, pager, next" :page-size="8" :total="total" @current-change="loadResources" />
      </div>
    </section>
    <ResourceViewer v-model="viewerVisible" :resource-id="activeResourceId" @completed="loadResources" />

    <el-dialog v-model="submissionVisible" title="投稿油气工程资源" width="720px">
      <el-form label-width="92px">
        <el-form-item label="标题">
          <el-input v-model="submissionForm.title" maxlength="120" placeholder="请输入资源标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="submissionForm.resourceType" placeholder="请选择类型">
            <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="submissionForm.knowledgePoint" maxlength="100" placeholder="如 钻井液密度、管输压降" />
        </el-form-item>
        <el-form-item label="风险类型">
          <el-input v-model="submissionForm.riskType" maxlength="100" placeholder="如 高压、易燃、腐蚀" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="submissionForm.tags" maxlength="200" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="外链">
          <el-input v-model="submissionForm.url" maxlength="500" placeholder="推荐填写可访问的视频、文档或网页链接" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="submissionForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <el-divider>我的投稿</el-divider>
      <el-table :data="mySubmissions" size="small" empty-text="暂无投稿">
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="reviewComment" label="审核意见" min-width="180" />
      </el-table>
      <template #footer>
        <el-button @click="submissionVisible = false">关闭</el-button>
        <el-button type="primary" :loading="submissionSaving" @click="submitPublicResource">提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publicPreviewVisible" :title="publicPreview?.title || '资源预览'" width="860px">
      <div class="public-preview">
        <p>{{ publicPreview?.aiSummary || '该资源来自用户投稿，审核通过后进入公共学习区。' }}</p>
        <iframe v-if="publicPreview?.previewUrl" :src="publicPreview.previewUrl" title="公共资源预览" />
        <el-empty v-else description="该资源暂未配置可预览地址" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Download, Pointer, Search, Star, View } from '@element-plus/icons-vue'
import ResourceViewer from '@/components/learning/ResourceViewer.vue'
import { getMyLearningRecords, updateLearningProgress } from '@/api/learningRecord'
import { getResources, interactResource, markResourceDownload } from '@/api/resource'
import { getMyResourceSubmissions, getPublicResourcePreview, getPublicResources, submitResource } from '@/api/resourceSubmission'
import resourceCore from '@/assets/amazing/resource-core.png'

const resourceTypes = [
  { label: '实验指导书', value: 'GUIDE' },
  { label: '课程讲义', value: 'LECTURE' },
  { label: 'PPT课件', value: 'PPT' },
  { label: '教学视频', value: 'TEACHING_VIDEO' },
  { label: '微课', value: 'MICRO_COURSE' },
  { label: '仪器操作视频', value: 'INSTRUMENT_VIDEO' },
  { label: 'HSE安全培训视频', value: 'HSE_VIDEO' },
  { label: '设备说明书', value: 'DEVICE_MANUAL' },
  { label: '实验案例', value: 'EXPERIMENT_CASE' },
  { label: '事故案例', value: 'ACCIDENT_CASE' },
  { label: '应急处置流程', value: 'EMERGENCY_PROCESS' },
  { label: '实验报告模板', value: 'REPORT_TEMPLATE' },
  { label: '参考文献', value: 'REFERENCE' },
  { label: '外部课程链接', value: 'EXTERNAL_COURSE' },
  { label: '虚拟仿真实验', value: 'VIRTUAL_SIMULATION' },
  { label: '文档', value: 'DOCUMENT' },
  { label: '图片', value: 'IMAGE' },
  { label: '视频', value: 'VIDEO' },
  { label: '音频', value: 'AUDIO' },
  { label: '网页链接', value: 'LINK' },
]

const filters = reactive({ keyword: '', experimentId: '', resourceType: '', favoriteOnly: 0 })
const resources = ref([])
const records = ref([])
const progressDrafts = reactive({})
const durationDrafts = reactive({})
const pageNum = ref(1)
const total = ref(0)
const loading = ref(false)
const viewerVisible = ref(false)
const activeResourceId = ref(null)
const submissionVisible = ref(false)
const submissionSaving = ref(false)
const mySubmissions = ref([])
const publicPreviewVisible = ref(false)
const publicPreview = ref(null)
const submissionForm = reactive({
  title: '',
  resourceType: 'LINK',
  knowledgePoint: '',
  riskType: '',
  tags: '',
  url: '',
  description: '',
})

onMounted(loadResources)

async function loadResources() {
  loading.value = true
  try {
    const [resourceResult, publicResult, recordResult] = await Promise.all([
      getResources({
        pageNum: pageNum.value,
        pageSize: 8,
        keyword: filters.keyword || undefined,
        experimentId: filters.experimentId || undefined,
        resourceType: filters.resourceType || undefined,
        favoriteOnly: filters.favoriteOnly || undefined,
        openScope: 'PUBLIC',
      }),
      getPublicResources({
        pageNum: pageNum.value,
        pageSize: 8,
        keyword: filters.keyword || undefined,
        resourceType: filters.resourceType || undefined,
      }).catch(() => ({ records: [], total: 0 })),
      getMyLearningRecords(),
    ])
    const platformResources = (resourceResult?.records || []).map((item) => ({ ...item, sourceType: 'PLATFORM' }))
    const publicResources = filters.favoriteOnly
      ? []
      : (publicResult?.records || []).map((item) => ({ ...item, sourceType: 'SUBMISSION' }))
    resources.value = [...platformResources, ...publicResources]
    total.value = Number(resourceResult?.total || 0) + Number(publicResult?.total || 0)
    records.value = Array.isArray(recordResult) ? recordResult : (recordResult?.records || [])
    initDrafts()
  } finally {
    loading.value = false
  }
}

function initDrafts() {
  resources.value.forEach((resource) => {
    const record = records.value.find((item) => item.resourceId === resource.id)
    progressDrafts[resource.id] = Number(record?.progress || 0)
    durationDrafts[resource.id] = 0
  })
}

async function preview(resource) {
  if (resource.sourceType === 'SUBMISSION') {
    publicPreview.value = await getPublicResourcePreview(resource.id)
    publicPreviewVisible.value = true
    return
  }
  activeResourceId.value = resource.id
  viewerVisible.value = true
}

async function openSubmission() {
  submissionVisible.value = true
  mySubmissions.value = submissionRecords(await getMyResourceSubmissions().catch(() => []))
}

async function submitPublicResource() {
  if (!submissionForm.title.trim()) {
    ElMessage.warning('请输入资源标题')
    return
  }
  if (!submissionForm.url.trim()) {
    ElMessage.warning('请填写资源外链')
    return
  }
  submissionSaving.value = true
  try {
    await submitResource({
      title: submissionForm.title.trim(),
      resourceType: submissionForm.resourceType,
      knowledgePoint: submissionForm.knowledgePoint || undefined,
      riskType: submissionForm.riskType || undefined,
      tags: submissionForm.tags || undefined,
      url: submissionForm.url.trim(),
      description: submissionForm.description || undefined,
    })
    ElMessage.success('投稿已提交，审核通过后将进入公共资源库')
    Object.assign(submissionForm, { title: '', resourceType: 'LINK', knowledgePoint: '', riskType: '', tags: '', url: '', description: '' })
    mySubmissions.value = submissionRecords(await getMyResourceSubmissions().catch(() => []))
  } finally {
    submissionSaving.value = false
  }
}

function submissionRecords(result) {
  return Array.isArray(result) ? result : (result?.records || [])
}

async function download(resource) {
  if (!resource.filePath && !resource.url) {
    ElMessage.warning('资源暂未配置下载地址')
    return
  }
  await markResourceDownload(resource.id)
  window.open(resource.filePath || resource.url, '_blank', 'noopener,noreferrer')
  ElMessage.success('已记录下载')
  await loadResources()
}

async function favorite(resource) {
  await interactResource(resource.id, { favoriteFlag: 1 })
  ElMessage.success('已收藏')
  await loadResources()
}

async function like(resource) {
  await interactResource(resource.id, { likeFlag: 1 })
  ElMessage.success('已点赞')
  await loadResources()
}

async function saveProgress(resource) {
  const progress = Number(progressDrafts[resource.id] || 0)
  const durationSeconds = Number(durationDrafts[resource.id] || 0) * 60
  const finished = isFinished(resource, progress, durationSeconds) ? 1 : 0
  await updateLearningProgress({
    resourceId: resource.id,
    progress,
    durationSeconds,
    finishFlag: finished,
  })
  ElMessage.success(finished ? '已达到完成条件' : '学习进度已保存')
  await loadResources()
}

function isFinished(resource, progress, durationSeconds) {
  const rule = resource.completionRule || 'CONFIRM'
  const minProgress = Number(resource.minProgress ?? 100)
  const minSeconds = Number(resource.minStudySeconds || 0)
  if (rule === 'TIME') return durationSeconds >= minSeconds
  if (rule === 'PROGRESS') return progress >= minProgress
  if (rule === 'PROGRESS_TIME') return progress >= minProgress && durationSeconds >= minSeconds
  return progress >= 100 || durationSeconds >= minSeconds
}

function progressValue(id) {
  return Number(progressDrafts[id] || 0)
}

function durationTotal(id) {
  const record = records.value.find((item) => item.resourceId === id)
  return Math.ceil(Number(record?.durationSeconds || 0) / 60)
}

function splitTags(tags) {
  return String(tags || '').split(/[,，]/).map((item) => item.trim()).filter(Boolean)
}

function typeLabel(type) {
  return resourceTypes.find((item) => item.value === type)?.label || type || '-'
}
</script>

<style scoped>
.resource-page { max-width: 1240px; margin: 0 auto; }
.resource-hero { min-height: 220px; border-radius: 8px; overflow: hidden; background-size: cover; background-position: center; display: flex; align-items: center; margin-bottom: 18px; }
.resource-hero > div { max-width: 620px; padding: 28px; background: linear-gradient(90deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0)); }
.resource-hero p { color: #177e89; font-size: 12px; font-weight: 800; margin-bottom: 8px; }
.resource-hero h1 { color: #13233a; font-size: 32px; line-height: 1.15; margin-bottom: 10px; }
.resource-hero span { color: #344054; line-height: 1.7; }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 14px; }
.toolbar .el-input, .toolbar .el-select { width: 220px; }
.resource-list { display: grid; gap: 12px; min-height: 180px; }
.resource-card { border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; background: #fff; }
.resource-main { margin-bottom: 12px; }
.resource-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.resource-title h2 { color: #13233a; font-size: 18px; line-height: 1.4; }
.resource-main p { color: #667085; line-height: 1.6; margin: 8px 0; }
.meta-row, .tag-row, .action-row, .progress-head, .pagination-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.meta-row span { color: #667085; font-size: 12px; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 4px 8px; }
.study-box { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.progress-head { justify-content: space-between; color: #13233a; margin-bottom: 4px; }
.study-box .el-input-number { width: 132px; }
.hint { color: #7b8794; font-size: 12px; }
.action-row { margin-top: 10px; }
.pagination-row { justify-content: space-between; color: #667085; padding-top: 14px; }
.public-preview p { color: #667085; line-height: 1.6; margin-bottom: 12px; }
.public-preview iframe { width: 100%; min-height: 520px; border: 1px solid #e7ebf0; border-radius: 8px; }
@media (max-width: 760px) {
  .page-head, .pagination-row { align-items: stretch; flex-direction: column; }
  .resource-hero > div { padding: 22px; background: rgba(255, 255, 255, 0.88); }
  .toolbar .el-input, .toolbar .el-select { width: 100%; min-width: 0; }
}
</style>
