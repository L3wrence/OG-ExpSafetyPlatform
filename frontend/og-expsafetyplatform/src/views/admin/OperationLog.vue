<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Operation Logs</p>
        <h1>操作日志</h1>
        <p class="page-desc">按模块、动作、结果和时间范围查询系统操作记录。</p>
      </div>
      <div class="head-actions">
        <el-button :icon="AlarmClock" @click="runReminders">执行提醒扫描</el-button>
        <el-button :icon="Refresh" @click="loadLogs">刷新</el-button>
      </div>
    </section>

    <section class="toolbar">
      <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索账号或内容" @keyup.enter="loadLogs" />
      <el-input v-model="filters.module" clearable placeholder="模块" />
      <el-input v-model="filters.action" clearable placeholder="动作" />
      <el-select v-model="filters.result" clearable placeholder="结果">
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAIL" />
      </el-select>
      <el-date-picker
        v-model="filters.range"
        type="datetimerange"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DDTHH:mm:ss"
      />
      <el-button type="primary" :icon="Search" @click="loadLogs">查询</el-button>
    </section>

    <section class="table-card">
      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column prop="createTime" label="时间" min-width="170" />
        <el-table-column prop="username" label="账号" min-width="120">
          <template #default="{ row }">{{ row.username || row.userId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="130" />
        <el-table-column prop="action" label="动作" width="140" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" effect="plain">{{ row.result || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && logs.length === 0" description="暂无日志" />
      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="pageNum"
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="total"
          @current-change="loadLogs"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { AlarmClock, Refresh, Search } from '@element-plus/icons-vue'
import { getOperationLogs, runReminderScan } from '@/api/adminPortal'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: '',
  module: '',
  action: '',
  result: '',
  range: [],
})

onMounted(loadLogs)

async function loadLogs() {
  loading.value = true
  try {
    const result = await getOperationLogs({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      module: filters.module || undefined,
      action: filters.action || undefined,
      result: filters.result || undefined,
      startTime: filters.range?.[0] || undefined,
      endTime: filters.range?.[1] || undefined,
    })
    logs.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

async function runReminders() {
  await runReminderScan()
  ElMessage.success('提醒扫描已执行')
  await loadLogs()
}
</script>

<style scoped>
.admin-page { max-width: 1240px; margin: 0 auto; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.head-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; line-height: 1.6; }
.toolbar, .table-card { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; }
.toolbar { display: grid; grid-template-columns: minmax(180px, 1fr) 120px 120px 120px minmax(320px, 1.4fr) auto; gap: 12px; padding: 14px; margin-bottom: 16px; }
.table-card { padding: 14px; }
.pagination-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #667085; padding-top: 14px; }
@media (max-width: 1040px) {
  .toolbar { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .page-head { align-items: stretch; flex-direction: column; }
  .toolbar { grid-template-columns: 1fr; }
}
</style>
