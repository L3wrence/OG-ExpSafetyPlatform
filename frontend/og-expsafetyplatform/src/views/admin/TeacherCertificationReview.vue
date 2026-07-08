<template>
  <div class="review-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Teacher Verification</p>
        <h1>教师认证审核</h1>
        <p>审核所属学校、工号与教育邮箱，通过后用户获得创建课堂权限。</p>
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
      <el-table :data="records" v-loading="loading" stripe empty-text="暂无申请">
        <el-table-column prop="userName" label="申请人" width="120" />
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column prop="school" label="所属学校" min-width="180" />
        <el-table-column prop="employeeNo" label="工号" width="140" />
        <el-table-column prop="educationEmail" label="教育邮箱" min-width="200" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="reviewComment" label="审核意见" min-width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
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
  approveTeacherCertification,
  getTeacherCertificationReviews,
  rejectTeacherCertification,
} from '@/api/teacherCertification'

const loading = ref(false)
const status = ref('PENDING')
const records = ref([])

onMounted(loadList)

async function loadList() {
  loading.value = true
  try {
    const result = await getTeacherCertificationReviews({ status: status.value })
    records.value = result?.records || []
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  await approveTeacherCertification(row.id, { reviewComment: '认证通过' })
  ElMessage.success('已通过认证')
  await loadList()
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回教师认证', {
    inputPlaceholder: '如：教育邮箱与学校不匹配',
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
  })
  await rejectTeacherCertification(row.id, { reviewComment: value })
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
