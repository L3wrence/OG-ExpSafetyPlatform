<template>
  <div class="review-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Resource Curation</p>
        <h1>资源投稿审核</h1>
        <p>筛选适合公开学习的油气工程资源，审核通过后进入公共资源学习区。</p>
      </div>
      <el-button :icon="Refresh" @click="loadList">刷新</el-button>
    </section>

    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="status" @change="loadList">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </div>
      <el-table :data="records" v-loading="loading" stripe empty-text="暂无投稿">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="submitterName" label="投稿人" width="120" />
        <el-table-column prop="resourceType" label="类型" width="120" />
        <el-table-column prop="knowledgePoint" label="知识点" min-width="140" />
        <el-table-column prop="riskType" label="风险类型" min-width="120" />
        <el-table-column prop="url" label="外链" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="open(row)">打开</el-button>
            <el-button text type="success" :disabled="row.status !== 'PENDING'" @click="approve(row)">通过</el-button>
            <el-button text type="danger" :disabled="row.status !== 'PENDING'" @click="reject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  approveResourceSubmission,
  getResourceSubmissionReviews,
  rejectResourceSubmission,
} from '@/api/resourceSubmission'

const loading = ref(false)
const status = ref('PENDING')
const records = ref([])

onMounted(loadList)

async function loadList() {
  loading.value = true
  try {
    const result = await getResourceSubmissionReviews({ status: status.value })
    records.value = result?.records || []
  } finally {
    loading.value = false
  }
}

function open(row) {
  if (!row.url && !row.filePath) {
    ElMessage.warning('投稿未提供可打开地址')
    return
  }
  window.open(row.url || row.filePath, '_blank', 'noopener,noreferrer')
}

async function approve(row) {
  await approveResourceSubmission(row.id, { reviewComment: '资源适合公开学习，审核通过' })
  ElMessage.success('已通过投稿')
  await loadList()
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回资源投稿', {
    inputPlaceholder: '如：链接不可访问或与油气工程学习无关',
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
  })
  await rejectResourceSubmission(row.id, { reviewComment: value })
  ElMessage.success('已驳回')
  await loadList()
}
</script>

<style scoped>
.review-page { display: grid; gap: 16px; }
.page-head { display: flex; justify-content: space-between; gap: 16px; align-items: flex-end; }
.page-head h1 { color: #13233a; font-size: 24px; margin: 6px 0; }
.page-head p { color: #667085; }
.eyebrow { color: #177e89; font-size: 12px; font-weight: 800; }
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
</style>
