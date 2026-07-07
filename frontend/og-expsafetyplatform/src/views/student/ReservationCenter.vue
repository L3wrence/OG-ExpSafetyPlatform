<template>
  <div class="business-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Lab Reservations</p>
        <h1>实验预约</h1>
        <p class="page-desc">选择可预约实验时段，提交预约用途，并跟踪审核结果。</p>
      </div>
      <el-button :icon="Refresh" @click="reload">刷新</el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-date-picker v-model="filters.date" value-format="YYYY-MM-DD" placeholder="预约日期" />
        <el-input v-model="filters.labId" clearable placeholder="实验室ID" />
        <el-button type="primary" :icon="Search" @click="loadAvailableSlots">查询可用时段</el-button>
      </div>

      <el-table v-loading="slotLoading" :data="availableSlots" stripe>
        <el-table-column label="日期" min-width="120">
          <template #default="{ row }">{{ slotOf(row).date || '-' }}</template>
        </el-table-column>
        <el-table-column label="时段" min-width="140">
          <template #default="{ row }">{{ slotOf(row).startTime || '-' }} - {{ slotOf(row).endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="实验室ID" width="100">
          <template #default="{ row }">{{ slotOf(row).labId || '-' }}</template>
        </el-table-column>
        <el-table-column label="实验ID" width="100">
          <template #default="{ row }">{{ slotOf(row).experimentId || '-' }}</template>
        </el-table-column>
        <el-table-column label="容量" width="130">
          <template #default="{ row }">{{ slotOf(row).bookedCount || 0 }} / {{ slotOf(row).capacity || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="slotStatusMeta(slotOf(row).status).type">{{ slotStatusMeta(slotOf(row).status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" :icon="Calendar" @click="openReserve(row)">预约</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ slotTotal }} 条记录</span>
        <el-pagination v-model:current-page="slotPage" layout="prev, pager, next" :page-size="10" :total="slotTotal" @current-change="loadAvailableSlots" />
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <h2>我的预约</h2>
        <el-select v-model="myStatus" clearable placeholder="筛选状态" @change="loadMyReservations">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </div>
      <el-table v-loading="myLoading" :data="myReservations" stripe>
        <el-table-column prop="labName" label="实验室" min-width="130" />
        <el-table-column prop="date" label="日期" min-width="120" />
        <el-table-column prop="timeRange" label="时段" min-width="140" />
        <el-table-column prop="experimentId" label="实验ID" width="100" />
        <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="reservationStatusMeta(row.status).type">{{ reservationStatusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核意见" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" text type="danger" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ myTotal }} 条记录</span>
        <el-pagination v-model:current-page="myPage" layout="prev, pager, next" :page-size="10" :total="myTotal" @current-change="loadMyReservations" />
      </div>
    </section>

    <el-dialog v-model="reserveVisible" title="提交实验预约" width="520px">
      <el-form :model="reserveForm" label-width="92px">
        <el-form-item label="时间段">
          <span>{{ selectedSlot?.date }} {{ selectedSlot?.startTime }} - {{ selectedSlot?.endTime }}</span>
        </el-form-item>
        <el-form-item label="实验室ID">
          <el-input v-model="reserveForm.labId" />
        </el-form-item>
        <el-form-item label="实验ID">
          <el-input v-model="reserveForm.experimentId" />
        </el-form-item>
        <el-form-item label="预约用途">
          <el-input v-model="reserveForm.purpose" type="textarea" :rows="4" placeholder="说明本次实验目的或任务" />
        </el-form-item>
        <el-alert
          v-if="admissionStatus"
          :type="admissionStatus.qualified ? 'success' : 'warning'"
          :closable="false"
          :title="admissionStatus.qualified ? `已获得准入资格，有效期至 ${admissionStatus.validUntil || '-'}` : `暂未满足准入：${admissionStatus.reason || '请完成必学任务和正式考试'}`"
        />
      </el-form>
      <template #footer>
        <el-button @click="reserveVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReservation">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Refresh, Search } from '@element-plus/icons-vue'
import {
  cancelReservation,
  createReservation,
  getAvailableSlots,
  getMyReservations,
} from '@/api/reservation'
import { getAdmissionStatus } from '@/api/exam'

const filters = reactive({ date: '', labId: '' })
const reserveForm = reactive({ timeSlotId: null, labId: '', experimentId: '', purpose: '' })
const availableSlots = ref([])
const myReservations = ref([])
const selectedSlot = ref(null)
const admissionStatus = ref(null)
const myStatus = ref('')
const slotPage = ref(1)
const myPage = ref(1)
const slotTotal = ref(0)
const myTotal = ref(0)
const slotLoading = ref(false)
const myLoading = ref(false)
const saving = ref(false)
const reserveVisible = ref(false)

onMounted(reload)

async function reload() {
  await Promise.allSettled([loadAvailableSlots(), loadMyReservations()])
}

async function loadAvailableSlots() {
  slotLoading.value = true
  try {
    const result = await getAvailableSlots({
      pageNum: slotPage.value,
      pageSize: 10,
      date: filters.date || undefined,
      labId: filters.labId || undefined,
    })
    availableSlots.value = result?.records || []
    slotTotal.value = result?.total || 0
  } finally {
    slotLoading.value = false
  }
}

async function loadMyReservations() {
  myLoading.value = true
  try {
    const result = await getMyReservations({
      pageNum: myPage.value,
      pageSize: 10,
      status: myStatus.value || undefined,
    })
    myReservations.value = result?.records || []
    myTotal.value = result?.total || 0
  } finally {
    myLoading.value = false
  }
}

async function openReserve(row) {
  const slot = slotOf(row)
  selectedSlot.value = slot
  Object.assign(reserveForm, {
    timeSlotId: slot.id,
    labId: slot.labId || '',
    experimentId: slot.experimentId || '',
    purpose: '',
  })
  admissionStatus.value = null
  if (slot.experimentId) {
    admissionStatus.value = await getAdmissionStatus(slot.experimentId).catch(() => null)
  }
  reserveVisible.value = true
}

async function submitReservation() {
  if (!reserveForm.labId) {
    ElMessage.warning('请输入实验室ID')
    return
  }
  if (admissionStatus.value && !admissionStatus.value.qualified) {
    ElMessage.warning(admissionStatus.value.reason || '暂未满足实验准入条件')
    return
  }
  saving.value = true
  try {
    await createReservation({
      timeSlotId: reserveForm.timeSlotId,
      labId: Number(reserveForm.labId),
      experimentId: reserveForm.experimentId ? Number(reserveForm.experimentId) : undefined,
      purpose: reserveForm.purpose || undefined,
    })
    ElMessage.success('预约已提交')
    reserveVisible.value = false
    await reload()
  } finally {
    saving.value = false
  }
}

async function handleCancel(row) {
  await ElMessageBox.confirm('确认取消该预约吗？', '取消预约', { type: 'warning' })
  await cancelReservation(row.id)
  ElMessage.success('预约已取消')
  await reload()
}

function slotOf(row) {
  return row?.timeSlot || row || {}
}

function slotStatusMeta(status) {
  return {
    AVAILABLE: { label: '可预约', type: 'success' },
    FULL: { label: '已满', type: 'warning' },
    CLOSED: { label: '关闭', type: 'info' },
  }[status] || { label: status || '未知', type: 'info' }
}

function reservationStatusMeta(status) {
  return {
    PENDING: { label: '待审核', type: 'warning' },
    APPROVED: { label: '已通过', type: 'success' },
    REJECTED: { label: '已拒绝', type: 'danger' },
    CANCELLED: { label: '已取消', type: 'info' },
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
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; }
.toolbar .el-input { max-width: 180px; }
.section-title { margin-bottom: 12px; }
.section-title h2 { color: #13233a; font-size: 18px; }
.pagination-row { color: #667085; padding-top: 14px; }
@media (max-width: 720px) {
  .page-head, .section-title, .pagination-row, .toolbar { align-items: stretch; flex-direction: column; }
  .toolbar .el-input { max-width: none; }
}
</style>
