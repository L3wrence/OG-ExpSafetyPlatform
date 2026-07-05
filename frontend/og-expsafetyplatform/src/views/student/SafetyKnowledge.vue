<template>
  <div class="safety-knowledge-page">
    <section class="safety-head">
      <div class="head-copy">
        <p class="eyebrow">Safety Learning</p>
        <h1>安全知识学习</h1>
        <p>面向实验前准备、风险识别、个人防护和应急处置的学习页面，帮助学生在进入实验前完成必要安全准备。</p>
      </div>
      <div class="readiness-panel">
        <div class="score-circle">
          <strong>{{ readinessScore }}</strong>
          <span>掌握度</span>
        </div>
        <div class="readiness-copy">
          <strong>{{ readinessTitle }}</strong>
          <span>{{ readinessText }}</span>
        </div>
      </div>
    </section>

    <section class="safety-toolbar">
      <el-input
        v-model="filters.keyword"
        :prefix-icon="Search"
        clearable
        placeholder="搜索风险类型、安全制度或应急措施"
      />
      <el-select v-model="filters.risk" placeholder="风险等级" clearable>
        <el-option label="低风险" value="低风险" />
        <el-option label="中风险" value="中风险" />
        <el-option label="高风险" value="高风险" />
      </el-select>
      <el-radio-group v-model="filters.category" class="category-tabs">
        <el-radio-button label="全部" />
        <el-radio-button label="实验室安全" />
        <el-radio-button label="设备操作" />
        <el-radio-button label="化学品管理" />
        <el-radio-button label="应急处置" />
      </el-radio-group>
    </section>

    <section class="overview-row">
      <div v-for="item in overview" :key="item.label" class="overview-item">
        <div class="overview-icon" :class="item.tone">
          <el-icon :size="22">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <div>
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="knowledge-layout">
      <div v-loading="loading" class="knowledge-list">
        <article v-for="item in displayedKnowledge" :key="item.id" class="knowledge-card">
          <div class="knowledge-main">
            <div class="knowledge-top">
              <div>
                <div class="label-row">
                  <el-tag size="small" :type="item.riskType">{{ item.risk }}</el-tag>
                  <el-tag v-if="item.required" size="small" type="danger" effect="plain">必学</el-tag>
                  <span>{{ item.category }}</span>
                </div>
                <h2>{{ item.title }}</h2>
              </div>
              <div class="progress-mini">
                <strong>{{ item.progress }}%</strong>
                <span>学习进度</span>
              </div>
            </div>

            <p class="knowledge-desc">{{ item.desc }}</p>

            <div class="knowledge-meta">
              <span><el-icon><Timer /></el-icon>{{ item.duration }}</span>
              <span><el-icon><Monitor /></el-icon>{{ item.experiment }}</span>
              <span><el-icon><DocumentChecked /></el-icon>{{ item.check }}</span>
            </div>

            <div class="point-list">
              <span v-for="point in item.points" :key="point">{{ point }}</span>
            </div>
          </div>

          <div class="knowledge-actions">
            <el-progress type="circle" :percentage="item.progress" :width="58" />
            <div class="action-buttons">
              <el-button :icon="Reading" @click="showPlaceholder(`${item.title} 学习`)">学习</el-button>
              <el-button type="primary" :icon="EditPen" @click="showPlaceholder(`${item.title} 测评`)">测评</el-button>
            </div>
          </div>
        </article>

        <el-empty v-if="!loading && displayedKnowledge.length === 0" description="暂无符合条件的安全知识" />

        <div class="pagination-row">
          <span>共 {{ total }} 条记录</span>
          <el-pagination
            v-model:current-page="pageNum"
            layout="prev, pager, next"
            :page-size="pageSize"
            :total="total"
            @current-change="loadKnowledge"
          />
        </div>
      </div>

      <aside class="risk-panel">
        <div class="panel-section">
          <div class="panel-title">
            <el-icon><WarningFilled /></el-icon>
            <span>高风险关注</span>
          </div>
          <div v-if="riskFocus.length > 0" class="risk-stack">
            <div v-for="risk in riskFocus" :key="risk.name" class="risk-row">
              <span>{{ risk.name }}</span>
              <el-progress :percentage="risk.value" :stroke-width="7" :color="risk.color" />
            </div>
          </div>
          <el-empty v-else description="暂无风险数据" :image-size="70" />
        </div>

        <div class="panel-section">
          <div class="panel-title">
            <el-icon><FirstAidKit /></el-icon>
            <span>应急处置流程</span>
          </div>
          <ol class="emergency-list">
            <li v-for="step in emergencySteps" :key="step">{{ step }}</li>
          </ol>
        </div>

        <div class="panel-section">
          <div class="panel-title">
            <el-icon><Checked /></el-icon>
            <span>实验前检查</span>
          </div>
          <el-checkbox-group v-model="checkedItems" class="check-list">
            <el-checkbox label="已阅读实验安全说明" />
            <el-checkbox label="已确认个人防护用品" />
            <el-checkbox label="已通过相关知识测评" />
            <el-checkbox label="已了解应急撤离路线" />
          </el-checkbox-group>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Checked,
  DocumentChecked,
  EditPen,
  FirstAidKit,
  Lock,
  Monitor,
  Reading,
  Search,
  Timer,
  Trophy,
  Warning,
  WarningFilled,
} from '@element-plus/icons-vue'
import { getSafetyKnowledge } from '@/api/safetyKnowledge'

const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(8)
const total = ref(0)
const knowledgeItems = ref([])
let keywordTimer = null

const filters = reactive({
  keyword: '',
  risk: '',
  category: '全部',
})

const checkedItems = ref(['已阅读实验安全说明'])

const displayedKnowledge = computed(() => {
  if (filters.category === '全部') return knowledgeItems.value
  return knowledgeItems.value.filter((item) => item.category === filters.category)
})

const highRiskCount = computed(() => knowledgeItems.value.filter((item) => item.riskType === 'danger').length)

const readinessScore = computed(() => {
  if (knowledgeItems.value.length === 0) return 0
  return Math.round(knowledgeItems.value.reduce((sum, item) => sum + item.progress, 0) / knowledgeItems.value.length)
})

const readinessTitle = computed(() => (total.value > 0 ? '安全知识已同步' : '暂无开放知识点'))
const readinessText = computed(() => {
  if (total.value === 0) return '后端当前没有开放的安全知识数据。'
  return `当前筛选到 ${displayedKnowledge.value.length} 个知识点，其中 ${highRiskCount.value} 个属于高风险关注。`
})

const overview = computed(() => [
  { label: '开放知识点', value: total.value, tone: 'primary', icon: Reading },
  { label: '已勾选检查', value: checkedItems.value.length, tone: 'success', icon: Trophy },
  { label: '关联实验', value: experimentCount.value, tone: 'warning', icon: Lock },
  { label: '高风险提醒', value: highRiskCount.value, tone: 'danger', icon: Warning },
])

const experimentCount = computed(() => new Set(knowledgeItems.value.map((item) => item.raw.experimentId).filter(Boolean)).size)

const riskFocus = computed(() => {
  const groups = [
    { name: '高风险', tone: 'danger', color: '#f56c6c' },
    { name: '中风险', tone: 'warning', color: '#e6a23c' },
    { name: '低风险', tone: 'success', color: '#67c23a' },
  ]
  return groups
    .map((group) => {
      const count = knowledgeItems.value.filter((item) => item.riskType === group.tone).length
      return {
        name: group.name,
        value: total.value === 0 ? 0 : Math.round((count / Math.max(knowledgeItems.value.length, 1)) * 100),
        color: group.color,
      }
    })
    .filter((item) => item.value > 0)
})

const emergencySteps = [
  '立即停止实验操作并提醒周围人员',
  '切断电源、气源或压力源',
  '按照现场标识撤离或隔离风险区域',
  '联系指导教师并记录事故信息',
]

watch(() => filters.keyword, () => {
  pageNum.value = 1
  window.clearTimeout(keywordTimer)
  keywordTimer = window.setTimeout(loadKnowledge, 300)
})

watch(() => filters.risk, () => {
  pageNum.value = 1
  loadKnowledge()
})

onMounted(loadKnowledge)

async function loadKnowledge() {
  loading.value = true
  try {
    const result = await getSafetyKnowledge({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      riskType: filters.risk || undefined,
    })
    knowledgeItems.value = (result?.records || []).map(mapKnowledge)
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

function mapKnowledge(item) {
  const risk = item.riskType || '未分类'
  const riskType = riskTone(risk)
  const category = inferCategory(item)
  const content = item.content || '暂无知识内容'
  return {
    id: item.id,
    title: item.knowledgePoint || `安全知识 ${item.id}`,
    category,
    risk,
    riskType,
    required: riskType === 'danger',
    progress: 0,
    duration: estimateDuration(content),
    experiment: item.experimentId ? `实验 ID ${item.experimentId}` : '通用安全知识',
    check: item.status === 1 ? '可学习' : '未开放',
    desc: content,
    points: buildPoints(item, category, risk),
    raw: item,
  }
}

function riskTone(risk) {
  const text = String(risk).toLowerCase()
  if (text.includes('高') || text.includes('high')) return 'danger'
  if (text.includes('中') || text.includes('medium')) return 'warning'
  if (text.includes('低') || text.includes('low')) return 'success'
  return 'info'
}

function inferCategory(item) {
  const text = `${item.knowledgePoint || ''} ${item.riskType || ''} ${item.content || ''}`
  if (/化学|药品|废液|泄漏/.test(text)) return '化学品管理'
  if (/设备|仪器|操作|阀门|压力/.test(text)) return '设备操作'
  if (/应急|火灾|报警|撤离|伤害/.test(text)) return '应急处置'
  return '实验室安全'
}

function estimateDuration(content) {
  const minutes = Math.max(5, Math.min(30, Math.ceil(content.length / 45)))
  return `${minutes} 分钟`
}

function buildPoints(item, category, risk) {
  return [
    category,
    risk,
    item.relatedStepId ? `关联步骤 ${item.relatedStepId}` : '',
    item.experimentId ? `实验 ${item.experimentId}` : '通用知识',
  ].filter(Boolean)
}

function showPlaceholder(target) {
  ElMessage.info(`${target} 功能后续接入接口`)
}
</script>

<style scoped>
.safety-knowledge-page {
  max-width: 1240px;
  margin: 0 auto;
}

.safety-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  align-items: stretch;
  margin-bottom: 18px;
}

.head-copy {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 22px;
}

.eyebrow {
  color: #7a5a00;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 6px;
}

.head-copy h1 {
  color: #13233a;
  font-size: 26px;
  line-height: 1.2;
  margin-bottom: 8px;
}

.head-copy p {
  color: #667085;
  line-height: 1.65;
}

.readiness-panel {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #1f3a5f;
  color: #fff;
  border-radius: 8px;
  padding: 22px;
}

.score-circle {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  border: 8px solid rgba(255, 255, 255, 0.22);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.score-circle strong {
  font-size: 28px;
  line-height: 1;
}

.score-circle span,
.readiness-copy span {
  font-size: 12px;
  opacity: 0.82;
}

.readiness-copy strong {
  display: block;
  font-size: 18px;
  margin-bottom: 8px;
}

.safety-toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 140px auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 14px;
}

.overview-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 16px;
}

.overview-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overview-icon.primary { background: #ecf5ff; color: #409eff; }
.overview-icon.success { background: #f0f9eb; color: #67c23a; }
.overview-icon.warning { background: #fdf6ec; color: #e6a23c; }
.overview-icon.danger { background: #fef0f0; color: #f56c6c; }

.overview-item strong {
  display: block;
  color: #13233a;
  font-size: 22px;
  line-height: 1.1;
}

.overview-item span {
  color: #7b8794;
  font-size: 13px;
}

.knowledge-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: 18px;
  align-items: start;
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 240px;
}

.knowledge-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
  gap: 16px;
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 18px;
  transition: box-shadow 0.2s;
}

.knowledge-card:hover {
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.07);
}

.knowledge-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}

.label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #7b8794;
  font-size: 12px;
  margin-bottom: 8px;
}

.knowledge-top h2 {
  color: #182230;
  font-size: 18px;
  line-height: 1.35;
}

.progress-mini {
  min-width: 74px;
  text-align: right;
}

.progress-mini strong {
  display: block;
  color: #13233a;
  font-size: 22px;
  line-height: 1.1;
}

.progress-mini span {
  color: #7b8794;
  font-size: 12px;
}

.knowledge-desc {
  color: #667085;
  line-height: 1.6;
  margin-bottom: 12px;
}

.knowledge-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  color: #667085;
  font-size: 13px;
  margin-bottom: 12px;
}

.knowledge-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.point-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.point-list span {
  color: #536579;
  background: #f2f5f8;
  border-radius: 999px;
  padding: 4px 9px;
  font-size: 12px;
}

.knowledge-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.action-buttons .el-button {
  margin-left: 0;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #667085;
  padding: 6px 0 14px;
}

.risk-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-section {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 16px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #13233a;
  font-weight: 700;
  margin-bottom: 14px;
}

.panel-title .el-icon {
  color: #e6a23c;
}

.risk-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.risk-row span {
  display: block;
  color: #536579;
  font-size: 13px;
  margin-bottom: 6px;
}

.emergency-list {
  padding-left: 18px;
  color: #536579;
  line-height: 1.75;
}

.emergency-list li + li {
  margin-top: 6px;
}

.check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

:deep(.knowledge-card .el-progress-bar__outer),
:deep(.risk-row .el-progress-bar__outer) {
  background: #e7edf3 !important;
}

@media (max-width: 1100px) {
  .safety-head,
  .knowledge-layout {
    grid-template-columns: 1fr;
  }

  .risk-panel {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 860px) {
  .safety-toolbar,
  .overview-row,
  .risk-panel {
    grid-template-columns: 1fr;
  }

  .knowledge-card {
    grid-template-columns: 1fr;
  }

  .knowledge-actions {
    align-items: stretch;
  }

  .category-tabs {
    overflow-x: auto;
  }

  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
