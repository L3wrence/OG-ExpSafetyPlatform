<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">User Administration</p>
        <h1>用户管理</h1>
        <p class="page-desc">维护学生、教师与管理员账号状态，集中查看角色、联系方式和最近登录情况。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
    </section>

    <section class="metric-grid">
      <div v-for="item in userMetrics" :key="item.label" class="metric-card">
        <div class="metric-icon" :style="{ color: item.color, background: item.bg }">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="toolbar">
      <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索账号、姓名或手机号" />
      <el-select v-model="filters.role" clearable placeholder="角色">
        <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.roleCode" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
    </section>

    <section class="table-card">
      <el-table :data="displayUsers" v-loading="loading" stripe>
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleTypeMap[resolveRoleCode(row)] || 'info'" effect="plain">
              {{ resolveRoleName(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="所属单位" min-width="150">
          <template #default="{ row }">{{ row.department || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="isUserEnabled(row) ? 'success' : 'info'">
              {{ isUserEnabled(row) ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="160">
          <template #default="{ row }">{{ row.lastLogin || row.lastLoginTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button text :type="isUserEnabled(row) ? 'warning' : 'success'" @click="toggleUserStatus(row)">
              {{ isUserEnabled(row) ? '停用' : '启用' }}
            </el-button>
            <el-button text type="danger" :icon="Delete" @click="removeUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="pageNum"
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="total"
          @current-change="loadUsers"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingUser ? '编辑用户' : '新增用户'" width="520px">
      <el-form :model="form" label-width="84px">
        <el-form-item label="账号">
          <el-input v-model="form.username" :disabled="Boolean(editingUser)" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item v-if="!editingUser" label="密码">
          <el-input v-model="form.password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" clearable placeholder="请选择角色">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Avatar, Delete, Edit, Plus, Search, User, UserFilled, WarningFilled } from '@element-plus/icons-vue'
import { createUser, deleteUser, getUsers, updateUser } from '@/api/user'
import { getRoles } from '@/api/role'

const dialogVisible = ref(false)
const editingUser = ref(null)
const loading = ref(false)
const saving = ref(false)
const users = ref([])
const roleOptions = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(8)
let keywordTimer = null

const filters = reactive({
  keyword: '',
  role: '',
  status: '',
})

const form = reactive({
  username: '',
  password: '',
  realName: '',
  roleId: null,
  phone: '',
  enabled: true,
})

const roleLabelMap = {
  student: '学生',
  teacher: '教师',
  admin: '管理员',
  lab_admin: '实验室管理员',
}

const roleTypeMap = {
  student: 'primary',
  teacher: 'success',
  admin: 'warning',
  lab_admin: 'danger',
}

const displayUsers = computed(() => users.value.filter((user) => {
  const roleCode = resolveRoleCode(user)
  const matchRole = !filters.role || roleCode === filters.role
  const matchStatus = filters.status === '' || filters.status == null || Number(user.status) === Number(filters.status)
  return matchRole && matchStatus
}))

const userMetrics = computed(() => [
  { label: '用户总数', value: total.value, icon: User, color: '#409eff', bg: '#ecf5ff' },
  { label: '教师账号', value: users.value.filter((item) => resolveRoleCode(item) === 'teacher').length, icon: Avatar, color: '#67c23a', bg: '#f0f9eb' },
  { label: '学生账号', value: users.value.filter((item) => resolveRoleCode(item) === 'student').length, icon: UserFilled, color: '#e6a23c', bg: '#fdf6ec' },
  { label: '停用账号', value: users.value.filter((item) => !isUserEnabled(item)).length, icon: WarningFilled, color: '#f56c6c', bg: '#fef0f0' },
])

watch(() => filters.keyword, () => {
  pageNum.value = 1
  window.clearTimeout(keywordTimer)
  keywordTimer = window.setTimeout(loadUsers, 300)
})

watch([() => filters.role, () => filters.status], () => {
  pageNum.value = 1
})

onMounted(async () => {
  await loadRoles()
  await loadUsers()
})

async function loadRoles() {
  const roles = await getRoles()
  roleOptions.value = (roles || []).map((role) => ({
    ...role,
    roleCode: normalizeRoleCode(role.roleCode),
    roleName: role.roleName || role.name || role.roleCode,
  }))
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await getUsers({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
    })
    users.value = result?.records || []
    total.value = result?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingUser.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(user) {
  editingUser.value = user
  Object.assign(form, {
    username: user.username || '',
    password: '',
    realName: user.realName || '',
    roleId: resolveRoleId(user),
    phone: user.phone || '',
    enabled: isUserEnabled(user),
  })
  dialogVisible.value = true
}

async function saveUser() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!editingUser.value && !form.password.trim()) {
    ElMessage.warning('请输入初始密码')
    return
  }
  if (form.roleId == null) {
    ElMessage.warning('请选择角色')
    return
  }

  saving.value = true
  try {
    if (editingUser.value) {
      await updateUser({
        id: editingUser.value.id,
        realName: form.realName,
        phone: form.phone,
        status: form.enabled ? 1 : 0,
        roleId: form.roleId,
      })
      ElMessage.success('用户已更新')
    } else {
      await createUser({
        username: form.username,
        password: form.password,
        realName: form.realName,
        phone: form.phone,
        roleId: form.roleId,
      })
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    await loadUsers()
  } finally {
    saving.value = false
  }
}

async function toggleUserStatus(user) {
  await updateUser({
    id: user.id,
    realName: user.realName,
    phone: user.phone,
    status: isUserEnabled(user) ? 0 : 1,
    roleId: resolveRoleId(user),
  })
  ElMessage.success(isUserEnabled(user) ? '用户已停用' : '用户已启用')
  await loadUsers()
}

async function removeUser(user) {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${user.username} 吗？`, '删除用户', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteUser(user.id)
    ElMessage.success('用户已删除')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error
    }
  }
}

function resetForm() {
  Object.assign(form, {
    username: '',
    password: '',
    realName: '',
    roleId: roleOptions.value[0]?.id || null,
    phone: '',
    enabled: true,
  })
}

function isUserEnabled(user) {
  return Number(user.status) === 1 || user.status === 'enabled'
}

function resolveRoleCode(user) {
  const directCode = user.roleCode || user.role
  if (directCode) return normalizeRoleCode(directCode)

  const roleCodes = Array.isArray(user.roles) ? user.roles.map(normalizeRoleCode).filter(Boolean) : []
  if (roleCodes.length > 0) return roleCodes[0]

  const role = roleOptions.value.find((item) => item.id === user.roleId)
  return role?.roleCode || ''
}

function resolveRoleName(user) {
  const roleCode = resolveRoleCode(user)
  if (roleCode && roleLabelMap[roleCode]) return roleLabelMap[roleCode]

  const role = roleOptions.value.find((item) => item.id === user.roleId || item.roleCode === roleCode)
  return role?.roleName || '未绑定'
}

function resolveRoleId(user) {
  if (user.roleId != null) return user.roleId

  const roleCode = resolveRoleCode(user)
  return roleOptions.value.find((item) => item.roleCode === roleCode)?.id || null
}

function normalizeRoleCode(code) {
  if (!code) return ''
  const normalized = String(code).trim().toLowerCase()
  return normalized.startsWith('role_') ? normalized.slice(5) : normalized
}
</script>

<style scoped>
.admin-page {
  max-width: 1240px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.eyebrow {
  color: #6b7c8f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 6px;
}

.page-head h1 {
  color: #13233a;
  font-size: 26px;
  line-height: 1.2;
  margin-bottom: 8px;
}

.page-desc {
  color: #667085;
  line-height: 1.6;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.metric-card,
.toolbar,
.table-card {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
}

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.metric-card strong {
  display: block;
  color: #13233a;
  font-size: 24px;
  line-height: 1;
  margin-bottom: 6px;
}

.metric-card span {
  color: #7b8794;
  font-size: 13px;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 150px 150px;
  gap: 12px;
  padding: 14px;
  margin-bottom: 16px;
}

.table-card {
  padding: 14px;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #667085;
  padding-top: 14px;
}

@media (max-width: 900px) {
  .page-head {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
