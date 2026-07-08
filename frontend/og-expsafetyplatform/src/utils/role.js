export const ROLE_HOME_PATHS = {
  user: '/user/home',
  admin: '/admin/home',
}

function collectMenuCodes(menus = []) {
  return menus.flatMap((menu) => [
    menu?.code,
    menu?.path,
    menu?.name,
    ...collectMenuCodes(menu?.children || []),
  ])
}

export function normalizeRole(value) {
  const role = String(value?.roleCode || value?.code || value?.role || value || '').trim().toLowerCase()
  if (!role) return ''
  if (role.includes('admin') || role.includes('管理员')) return 'admin'
  return 'user'
}

export function getRoleHomePath(role) {
  return ROLE_HOME_PATHS[normalizeRole(role)] || '/login'
}

export function inferRoleFromLoginResult(result = {}) {
  const userInfo = result.userInfo || {}
  const directRoles = [
    result.role,
    result.roleCode,
    result.userType,
    ...(Array.isArray(result.roles) ? result.roles : []),
    userInfo.role,
    userInfo.roleCode,
    userInfo.roleName,
    userInfo.userType,
    ...(Array.isArray(userInfo.roles) ? userInfo.roles : []),
  ]

  for (const item of directRoles) {
    const role = normalizeRole(item)
    if (role) return role
  }

  const roleText = [
    result.roleName,
    ...(result.permissions || []),
    ...collectMenuCodes(result.menus || []),
  ].join(' ').toLowerCase()

  if (
    roleText.includes('admin')
    || roleText.includes('管理员')
    || roleText.includes('/admin')
    || roleText.includes('user:view')
    || roleText.includes('role:view')
    || roleText.includes('permission:view')
  ) {
    return 'admin'
  }
  if (roleText.includes('portal:view') || roleText.includes('resource:view') || roleText.includes('course:view')) {
    return 'user'
  }

  return ''
}
