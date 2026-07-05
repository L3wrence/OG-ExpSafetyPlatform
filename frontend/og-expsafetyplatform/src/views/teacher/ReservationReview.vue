<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Reservation Review</p>
        <h1>预约审核</h1>
        <p class="page-desc">审核学生实验预约，并维护实验室可预约时间段。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openSlotCreate">新增时段</el-button>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>待审核预约</h2>
        <div class="toolbar">
          <el-input v-model="pendingLabId" clearable placeholder="实验室ID" />
          <el-button type="primary" :icon="Search" @click="loadPending">查询</el-button>
        </div>
      </div>
      <el-table v-loading="pendingLoading" :data="pendingReservations" stripe>
        <el-table-column prop="studentName" label="学生" min-width="110" />
        <el-table-column prop="labName" label="实验室" min-width="130" />
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="timeRange" label="时段" min-width="140" />
        <el-table-column prop="experimentId" label="实验ID" width="90" />
        <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text type="success" @click="openReview(row, 'APPROVED')">通过</el-button>
            <el-button text type="danger" @click="openReview(row, 'REJECTED')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ pendingTotal }} 条记录</span>
        <el-pagination v-model:current-page="pendingPage" layout="prev, pager, next" :page-size="10" :total="pendingTotal" @current-change="loadPending" />
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>实验时段</h2>
        <div class="toolbar">
          <el-date-picker v-model="slotFilters.date" value-format="YYYY-MM-DD" placeholder="日期" />
          <el-input v-model="slotFilters.labId" clearable placeholder="实验室ID" />
          <el-select v-model="slotFilters.status" clearable placeholder="状态">
            <el-option label="可预约" value="AVAILABLE" />
            <el-option label="已满" value="FULL" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadSlots">查询</el-button>
        </div>
      </div>
      <el-table v-loading="slotLoading" :data="slots" stripe>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column label="时段" min-width="140">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column prop="labId" label="实验室ID" width="100" />
        <el-table-column prop="experimentId" label="实验ID" width="100" />
        <el-table-column label="容量" width="130">
          <template #default="{ row }">{{ row.bookedCount || 0 }} / {{ row.capacity || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="slotStatusMeta(row.status).type">{{ slotStatusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openSlotEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="removeSlot(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ slotTotal }} 条记录</span>
        <el-pagination v-model:current-page="slotPage" layout="prev, pager, next" :page-size="10" :total="slotTotal" @current-change="loadSlots" />
      </div>
    </section>

    <el-dialog v-model="reviewVisible" :title="reviewForm.status === 'APPROVED' ? '通过预约' : '驳回预约'" width="520px">
      <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="5" placeholder="填写审核意见" />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSaving" @click="submitReview">提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="slotVisible" :title="editingSlot ? '编辑时段' : '新增时段'" width="600px">
      <el-form :model="slotForm" label-width="92px">
        <el-form-item label="日期" required><el-date-picker v-model="slotForm.date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="开始时间" required><el-time-picker v-model="slotForm.startTime" value-format="HH:mm" format="HH:mm" /></el-form-item>
        <el-form-item label="结束时间" required><el-time-picker v-model="slotForm.endTime" value-format="HH:mm" format="HH:mm" /></el-form-item>
        <el-form-item label="实验室ID" required><el-input v-model="slotForm.labId" /></el-form-item>
        <el-form-item label="实验ID"><el-input v-model="slotForm.experimentId" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="slotForm.capacity" :min="1" :max="200" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="slotForm.status">
            <el-option label="可预约" value="AVAILABLE" />
            <el-option label="已满" value="FULL" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="slotVisible = false">取消</el-button>
        <el-button type="primary" :loading="slotSaving" @click="saveSlot">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  createTimeSlots,
  deleteTimeSlot,
  getPendingReservations,
  getTimeSlots,
  reviewReservation,
  updateTimeSlot,
} from '@/api/reservation'

const pendingReservations = ref([])
const slots = ref([])
const pendingLabId = ref('')
const pendingPage = ref(1)
const slotPage = ref(1)
const pendingTotal = ref(0)
const slotTotal = ref(0)
const pendingLoading = ref(false)
const slotLoading = ref(false)
const reviewSaving = ref(false)
const slotSaving = ref(false)
const reviewVisible = ref(false)
const slotVisible = ref(false)
const currentReservation = ref(null)
const editingSlot = ref(null)
const slotFilters = reactive({ date: '', labId: '', status: '' })
const reviewForm = reactive({ status: 'APPROVED', reviewComment: '' })
const slotForm = reactive({ date: '', startTime: '', endTime: '', labId: '', experimentId: '', capacity: 20, status: 'AVAILABLE' })

onMounted(() => Promise.allSettled([loadPending(), loadSlots()]))

async function loadPending() {
  pendingLoading.value = true
  try {
    const result = await getPendingReservations({
      pageNum: pendingPage.value,
      pageSize: 10,
      labId: pendingLabId.value || undefined,
    })
    pendingReservations.value = result?.records || []
    pendingTotal.value = result?.total || 0
  } finally {
    pendingLoading.value = false
  }
}

async function loadSlots() {
  slotLoading.value = true
  try {
    const result = await getTimeSlots({
      pageNum: slotPage.value,
      pageSize: 10,
      labId: slotFilters.labId || undefined,
      date: slotFilters.date || undefined,
      status: slotFilters.status || undefined,
    })
    slots.value = result?.records || []
    slotTotal.value = result?.total || 0
  } finally {
    slotLoading.value = false
  }
}

function openReview(row, status) {
  currentReservation.value = row
  Object.assign(reviewForm, { status, reviewComment: status === 'APPROVED' ? '同意预约' : '' })
  reviewVisible.value = true
}

async function submitReview() {
  reviewSaving.value = true
  try {
    await reviewReservation(currentReservation.value.id, reviewForm)
    ElMessage.success('预约已审核')
    reviewVisible.value = false
    await Promise.allSettled([loadPending(), loadSlots()])
  } finally {
    reviewSaving.value = false
  }
}

function openSlotCreate() {
  editingSlot.value = null
  Object.assign(slotForm, { date: '', startTime: '', endTime: '', labId: '', experimentId: '', capacity: 20, status: 'AVAILABLE' })
  slotVisible.value = true
}

function openSlotEdit(row) {
  editingSlot.value = row
  Object.assign(slotForm, {
    date: row.date || '',
    startTime: row.startTime || '',
    endTime: row.endTime || '',
    labId: row.labId || '',
    experimentId: row.experimentId || '',
    capacity: row.capacity || 20,
    status: row.status || 'AVAILABLE',
  })
  slotVisible.value = true
}

async function saveSlot() {
  if (!slotForm.date || !slotForm.startTime || !slotForm.endTime || !slotForm.labId) {
    ElMessage.warning('请填写日期、时间和实验室ID')
    return
  }
  const payload = {
    date: slotForm.date,
    startTime: slotForm.startTime,
    endTime: slotForm.endTime,
    labId: Number(slotForm.labId),
    experimentId: slotForm.experimentId ? Number(slotForm.experimentId) : undefined,
    capacity: Number(slotForm.capacity),
    status: slotForm.status,
  }
  slotSaving.value = true
  try {
    if (editingSlot.value) {
      await updateTimeSlot(editingSlot.value.id, payload)
      ElMessage.success('时段已更新')
    } else {
      await createTimeSlots([payload])
      ElMessage.success('时段已创建')
    }
    slotVisible.value = false
    await loadSlots()
  } finally {
    slotSaving.value = false
  }
}

async function removeSlot(row) {
  await ElMessageBox.confirm('确认删除该实验时段吗？', '删除时段', { type: 'warning' })
  await deleteTimeSlot(row.id)
  ElMessage.success('时段已删除')
  await loadSlots()
}

function slotStatusMeta(status) {
  return {
    AVAILABLE: { label: '可预约', type: 'success' },
    FULL: { label: '已满', type: 'warning' },
    CLOSED: { label: '关闭', type: 'info' },
  }[status] || { label: status || '未知', type: 'info' }
}
</script>

<style scoped>
.business-page { max-width: 1240px; margin: 0 auto; }
.page-head, .section-title, .pagination-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.page-head { align-items: flex-end; margin-bottom: 18px; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; margin-bottom: 16px; }
.section-title { margin-bottom: 12px; }
.section-title h2 { color: #13233a; font-size: 18px; }
.toolbar { display: flex; gap: 10px; align-items: center; }
.toolbar .el-input, .toolbar .el-select { max-width: 170px; }
.pagination-row { color: #667085; padding-top: 14px; }
@media (max-width: 860px) {
  .page-head, .section-title, .toolbar, .pagination-row { align-items: stretch; flex-direction: column; }
  .toolbar .el-input, .toolbar .el-select { max-width: none; }
}
</style>
