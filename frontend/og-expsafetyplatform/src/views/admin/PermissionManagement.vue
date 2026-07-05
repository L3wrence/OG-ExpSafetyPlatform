<template>
  <div class="admin-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">Permission Administration</p>
        <h1>权限管理</h1>
        <p class="page-desc">查看菜单权限和操作权限的层级结构，并维护角色与权限点之间的授权关系。</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">同步权限</el-button>
        <el-button type="primary" :icon="Plus" @click="notReady">新增权限</el-button>
      </div>
    </section>

    <section class="permission-overview">
      <div v-for="item in overview" :key="item.label" class="overview-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </section>

    <section class="permission-layout">
      <aside class="tree-card">
        <div class="card-head">
          <h2><el-icon><Menu /></el-icon> 权限树</h2>
          <el-input v-model="treeKeyword" :prefix-icon="Search" clearable placeholder="搜索权限" />
        </div>
        <el-tree
          ref="permissionTreeRef"
          v-loading="loading"
          :data="permissionTree"
          :props="permissionProps"
          :filter-node-method="filterPermissionNode"
          node-key="id"
          default-expand-all
          highlight-current
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
      </aside>

      <main class="matrix-card" v-loading="loading">
        <div class="card-head matrix-head">
          <div>
            <h2><el-icon><Lock /></el-icon> 权限矩阵</h2>
            <p>按模块维护各角色拥有的菜单和操作权限。</p>
          </div>
          <el-segmented v-model="scope" :options="['全部', '菜单', '操作']" />
        </div>

        <div class="module-grid">
          <article v-for="module in filteredModules" :key="module.id" class="module-card">
            <div class="module-head">
              <div class="module-icon" :style="{ color: module.color, background: module.bg }">
                <el-icon :size="22"><component :is="module.icon" /></el-icon>
              </div>
              <div>
                <strong>{{ module.name }}</strong>
                <span>{{ module.code || '-' }}</span>
              </div>
            </div>

            <div class="role-checks">
              <label v-for="role in roleColumns" :key="role.id">
                <el-checkbox
                  :model-value="hasRolePermission(role.id, module.id)"
                  @change="(checked) => toggleRolePermission(role, module, checked)"
                />
                <span>{{ role.label }}</span>
              </label>
            </div>

            <div class="action-list">
              <div v-for="action in module.actions" :key="action.id" class="action-item">
                <div class="action-main">
                  <span>{{ action.name }}</span>
                  <small>{{ action.code || '-' }}</small>
                </div>
                <el-tag size="small" :type="action.type === 1 ? 'primary' : 'info'" effect="plain">
                  {{ permissionTypeText(action.type) }}
                </el-tag>
                <div class="action-role-checks">
                  <el-tooltip v-for="role in roleColumns" :key="role.id" :content="role.label" placement="top">
                    <el-checkbox
                      :model-value="hasRolePermission(role.id, action.id)"
                      @change="(checked) => toggleRolePermission(role, action, checked)"
                    />
                  </el-tooltip>
                </div>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-if="!loading && filteredModules.length === 0" description="暂无权限数据" />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Avatar, Bell, Calendar, Document, Lock, Menu, Plus, Refresh, Search, Setting, User } from '@element-plus/icons-vue'
import { getPermissionTree } from '@/api/permission'
import { getRolePermissions, getRoles, saveRolePermissions } from '@/api/role'

const treeKeyword = ref('')
const scope = ref('全部')
const loading = ref(false)
const permissionTree = ref([])
const roles = ref([])
const rolePermissionMap = ref({})
const permissionTreeRef = ref(null)

const permissionProps = {
  label: 'name',
  children: 'children',
}

const moduleStyleMap = {
  user: { icon: User, color: '#409eff', bg: '#ecf5ff' },
  role: { icon: Avatar, color: '#67c23a', bg: '#f0f9eb' },
  permission: { icon: Lock, color: '#e6a23c', bg: '#fdf6ec' },
  reservation: { icon: Calendar, color: '#f56c6c', bg: '#fef0f0' },
  report: { icon: Document, color: '#77567a', bg: '#f5f0f7' },
  notice: { icon: Bell, color: '#33658a', bg: '#eef6ff' },
}

const roleColumns = computed(() => roles.value.map((role) => ({
  id: role.id,
  code: role.code,
  label: role.name,
})))

const allPermissions = computed(() => flattenPermissions(permissionTree.value))

const permissionModules = computed(() => permissionTree.value.map((permission) => {
  const style = getPermissionStyle(permission)
  const actions = flattenPermissions(permission.children || [])
  return {
    ...permission,
    ...style,
    actions: actions.length > 0 ? actions : [permission],
  }
}))

const filteredModules = computed(() => {
  if (scope.value === '全部') return permissionModules.value

  const targetType = scope.value === '菜单' ? 1 : 2
  return permissionModules.value
    .map((module) => ({
      ...module,
      actions: module.actions.filter((action) => Number(action.type) === targetType),
    }))
    .filter((module) => Number(module.type) === targetType || module.actions.length > 0)
})

const overview = computed(() => {
  const assignedCount = Object.values(rolePermissionMap.value)
    .reduce((total, ids) => total + ids.length, 0)
  return [
    { label: '模块数量', value: permissionTree.value.length, hint: '顶层权限模块' },
    { label: '权限点', value: allPermissions.value.length, hint: '菜单与操作合计' },
    { label: '角色数量', value: roles.value.length, hint: '来自后端角色表' },
    { label: '授权关系', value: assignedCount, hint: '角色权限绑定数' },
  ]
})

watch(treeKeyword, (keyword) => {
  permissionTreeRef.value?.filter(keyword)
})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [treeResult, roleResult] = await Promise.all([
      getPermissionTree(),
      getRoles(),
    ])

    permissionTree.value = treeResult || []
    roles.value = (roleResult || []).map(normalizeRole)

    const entries = await Promise.all(roles.value.map(async (role) => {
      const permissionIds = await getRolePermissions(role.id)
      return [role.id, normalizeIds(permissionIds || [])]
    }))
    rolePermissionMap.value = Object.fromEntries(entries)
  } finally {
    loading.value = false
  }
}

function hasRolePermission(roleId, permissionId) {
  return (rolePermissionMap.value[roleId] || []).includes(Number(permissionId))
}

async function toggleRolePermission(role, permission, checked) {
  const previousIds = rolePermissionMap.value[role.id] || []
  const nextIds = new Set(previousIds)

  if (checked) {
    nextIds.add(Number(permission.id))
    getAncestorIds(permission).forEach((id) => nextIds.add(id))
  } else {
    nextIds.delete(Number(permission.id))
    getDescendantIds(permission).forEach((id) => nextIds.delete(id))
  }

  const normalizedNextIds = normalizeIds([...nextIds])
  rolePermissionMap.value = {
    ...rolePermissionMap.value,
    [role.id]: normalizedNextIds,
  }

  try {
    await saveRolePermissions(role.id, normalizedNextIds)
    ElMessage.success('权限配置已保存')
  } catch (error) {
    rolePermissionMap.value = {
      ...rolePermissionMap.value,
      [role.id]: previousIds,
    }
    throw error
  }
}

function normalizeRole(role) {
  const code = normalizeRoleCode(role.roleCode || role.code)
  return {
    ...role,
    code,
    name: role.roleName || role.name || code || '未命名角色',
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

function flattenPermissions(nodes) {
  return nodes.flatMap((node) => [node, ...flattenPermissions(node.children || [])])
}

function getAncestorIds(permission) {
  const ids = []
  let currentParentId = Number(permission.parentId)
  while (currentParentId) {
    const parent = allPermissions.value.find((item) => Number(item.id) === currentParentId)
    if (!parent) break
    ids.push(Number(parent.id))
    currentParentId = Number(parent.parentId)
  }
  return ids
}

function getDescendantIds(permission) {
  return flattenPermissions(permission.children || []).map((item) => Number(item.id))
}

function getPermissionStyle(permission) {
  const moduleCode = String(permission.code || '').split(':')[0]
  return moduleStyleMap[moduleCode] || { icon: Setting, color: '#7b8794', bg: '#f4f6f8' }
}

function permissionTypeText(type) {
  return Number(type) === 1 ? '菜单' : '操作'
}

function filterPermissionNode(keyword, data) {
  if (!keyword) return true
  const normalizedKeyword = keyword.trim().toLowerCase()
  return [data.name, data.code].some((value) => String(value || '').toLowerCase().includes(normalizedKeyword))
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
.matrix-head p,
.module-head span,
.action-item small {
  color: #667085;
  line-height: 1.6;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.permission-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.overview-card,
.tree-card,
.matrix-card {
  background: #fff;
  border: 1px solid #e7ebf0;
  border-radius: 8px;
}

.overview-card {
  padding: 16px;
}

.overview-card span,
.overview-card small {
  display: block;
  color: #7b8794;
  font-size: 13px;
}

.overview-card strong {
  display: block;
  color: #13233a;
  font-size: 26px;
  margin: 8px 0 5px;
}

.permission-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.tree-card,
.matrix-card {
  padding: 16px;
}

.card-head {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 14px;
}

.card-head h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #13233a;
  font-size: 17px;
}

.matrix-head {
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-right: 8px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.module-card {
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 14px;
}

.module-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.module-head strong,
.action-item span {
  display: block;
  color: #13233a;
}

.module-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.role-checks {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(82px, 1fr));
  gap: 8px;
  background: #f8fafc;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 12px;
}

.role-checks label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #536579;
  font-size: 13px;
}

.action-list {
  display: grid;
  gap: 8px;
}

.action-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #f0f2f5;
  padding-bottom: 8px;
}

.action-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.action-main {
  min-width: 0;
}

.action-role-checks {
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 1040px) {
  .permission-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .page-head,
  .matrix-head {
    align-items: stretch;
    flex-direction: column;
  }

  .permission-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .permission-overview,
  .role-checks,
  .action-item {
    grid-template-columns: 1fr;
  }
}
</style>
