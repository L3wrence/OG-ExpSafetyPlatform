<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Role Administration</p>
        <h1>角色管理</h1>
        <p class="page-desc">配置普通用户与系统管理员的基础权限，教师能力由认证状态派生。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="notReady">新增角色</el-button>
    </section>

    <section class="role-layout">
      <div class="role-list" v-loading="roleLoading">
        <article
          v-for="role in roles"
          :key="role.id"
          class="role-card"
          :class="{ active: selectedRole?.id === role.id }"
          @click="selectRole(role)"
        >
          <div class="role-icon" :style="{ background: role.bg, color: role.color }">
            <el-icon :size="24"><component :is="role.icon" /></el-icon>
          </div>
          <div class="role-main">
            <div class="role-title-row">
              <strong>{{ role.name }}</strong>
              <el-tag size="small" :type="role.type">{{ role.code }}</el-tag>
            </div>
            <p>{{ role.description || '暂无角色说明' }}</p>
            <div class="role-meta">
              <span>权限 {{ role.id === selectedRole?.id ? checkedPermissionIds.length : '-' }} 项</span>
            </div>
          </div>
        </article>
        <el-empty v-if="!roleLoading && roles.length === 0" description="暂无角色" />
      </div>

      <div class="detail-card" v-if="selectedRole">
        <div class="detail-head">
          <div>
            <p class="detail-label">当前角色</p>
            <h2>{{ selectedRole.name }}</h2>
            <span>{{ selectedRole.description || '暂无角色说明' }}</span>
          </div>
          <div class="detail-actions">
            <el-button :icon="Edit" @click="notReady">编辑角色</el-button>
            <el-button type="primary" :icon="Check" :loading="saving" @click="saveSelectedPermissions">保存配置</el-button>
          </div>
        </div>

        <div class="summary-grid">
          <div>
            <strong>-</strong>
            <span>绑定用户</span>
          </div>
          <div>
            <strong>{{ checkedPermissionIds.length }}</strong>
            <span>权限点</span>
          </div>
          <div>
            <strong>{{ selectedRole.code }}</strong>
            <span>角色编码</span>
          </div>
        </div>

        <el-tabs v-model="activeTab">
          <el-tab-pane label="权限范围" name="permissions">
            <el-tree
              ref="permissionTreeRef"
              v-loading="permissionLoading"
              class="permission-tree"
              :data="permissionTree"
              :props="permissionProps"
              show-checkbox
              node-key="id"
              default-expand-all
              @check="syncCheckedPermissions"
            >
              <template #default="{ data }">
                <div class="tree-node">
                  <span>{{ data.name }}</span>
                  <el-tag size="small" :type="data.type === 1 ? 'primary' : 'info'" effect="plain">
                    {{ permissionTypeText(data.type) }}
                  </el-tag>
                </div>
              </template>
            </el-tree>
          </el-tab-pane>
          <el-tab-pane label="成员预览" name="members">
            <el-empty description="后端暂未提供角色成员接口" />
          </el-tab-pane>
          <el-tab-pane label="变更记录" name="logs">
            <el-empty description="后端暂未提供角色变更记录接口" />
          </el-tab-pane>
        </el-tabs>
      </div>

      <div v-else class="detail-card">
        <el-empty description="请选择角色" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Edit, Plus, Setting, UserFilled } from '@element-plus/icons-vue'
import { getPermissionTree } from '@/api/permission'
import { getRolePermissions, getRoles, saveRolePermissions } from '@/api/role'

const activeTab = ref('permissions')
const roleLoading = ref(false)
const permissionLoading = ref(false)
const saving = ref(false)
const roles = ref([])
const selectedRole = ref(null)
const permissionTree = ref([])
const permissionTreeRef = ref(null)
const checkedPermissionIds = ref([])

const permissionProps = {
  label: 'name',
  children: 'children',
}

const roleStyleMap = {
  admin: { type: 'warning', icon: Setting, color: '#e6a23c', bg: '#fdf6ec' },
  user: { type: 'primary', icon: UserFilled, color: '#409eff', bg: '#ecf5ff' },
}

onMounted(async () => {
  await loadPermissionTree()
  await loadRoles()
})

async function loadRoles() {
  roleLoading.value = true
  try {
    const result = await getRoles()
    roles.value = (result || []).map(normalizeRole)
    if (roles.value.length > 0) {
      await selectRole(roles.value[0])
    }
  } finally {
    roleLoading.value = false
  }
}

async function loadPermissionTree() {
  permissionLoading.value = true
  try {
    permissionTree.value = await getPermissionTree()
  } finally {
    permissionLoading.value = false
  }
}

async function selectRole(role) {
  selectedRole.value = role
  const result = await getRolePermissions(role.id)
  checkedPermissionIds.value = normalizeIds(result || [])
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys(checkedPermissionIds.value)
}

async function saveSelectedPermissions() {
  if (!selectedRole.value) return
  syncCheckedPermissions()
  saving.value = true
  try {
    await saveRolePermissions(selectedRole.value.id, checkedPermissionIds.value)
    ElMessage.success('角色权限已保存')
  } finally {
    saving.value = false
  }
}

function syncCheckedPermissions() {
  const tree = permissionTreeRef.value
  if (!tree) return

  checkedPermissionIds.value = normalizeIds([
    ...tree.getCheckedKeys(false),
    ...tree.getHalfCheckedKeys(),
  ])
}

function normalizeRole(role) {
  const code = normalizeRoleCode(role.roleCode || role.code)
  const style = roleStyleMap[code] || { type: 'info', icon: Setting, color: '#7b8794', bg: '#f4f6f8' }
  return {
    ...role,
    code,
    name: role.roleName || role.name || code || '未命名角色',
    description: role.description,
    ...style,
  }
}

function normalizeRoleCode(code) {
  if (!code) return ''
  const normalized = String(code).trim().toLowerCase()
  return normalized.startsWith('role_') ? normalized.slice(5) : normalized
}

function normalizeIds(ids) {
  return [...new Set(ids.map((id) => Number(id)).filter((id) => !Number.isNaN(id)))]
}

function permissionTypeText(type) {
  return Number(type) === 1 ? '菜单' : '操作'
}

function notReady() {
  ElMessage.warning('后端接口暂未开放')
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

.page-desc,
.role-card p,
.detail-head span,
.summary-grid span {
  color: #667085;
  line-height: 1.6;
}

.role-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.role-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 220px;
}

.role-card,
.detail-card {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
}

.role-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.role-card:hover,
.role-card.active {
  border-color: #409eff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
}

.role-icon {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.role-main {
  flex: 1;
  min-width: 0;
}

.role-title-row,
.role-meta,
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.role-title-row strong,
.detail-head h2 {
  color: #13233a;
}

.role-card p {
  margin: 8px 0 10px;
}

.role-meta {
  justify-content: flex-start;
  color: #7b8794;
  font-size: 12px;
}

.detail-card {
  padding: 18px;
}

.detail-label {
  color: #7b8794;
  font-size: 12px;
  margin-bottom: 4px;
}

.detail-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0;
}

.summary-grid div {
  background: #f8fafc;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 14px;
}

.summary-grid strong {
  display: block;
  color: #13233a;
  font-size: 22px;
  margin-bottom: 6px;
}

.permission-tree {
  min-height: 220px;
}

.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-right: 8px;
}

@media (max-width: 980px) {
  .role-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .page-head,
  .detail-head {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
