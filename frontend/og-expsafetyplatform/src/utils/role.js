export const ROLE_HOME_PATHS = {
  student: '/student/home',
  teacher: '/teacher/home',
  admin: '/admin/home',
}

const ROLE_ALIASES = {
  1: 'student',
  2: 'teacher',
  3: 'admin',
  student: 'student',
  teacher: 'teacher',
  admin: 'admin',
  administrator: 'admin',
  role_student: 'student',
  role_teacher: 'teacher',
  role_admin: 'admin',
  'student role': 'student',
  'teacher role': 'teacher',
  'admin role': 'admin',
  '学生': 'student',
  '教师': 'teacher',
  '老师': 'teacher',
  '管理员': 'admin',
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
  const role = String(value || '').trim().toLowerCase()
  if (ROLE_ALIASES[role]) return ROLE_ALIASES[role]
  if (role.includes('admin') || role.includes('管理员')) return 'admin'
  if (role.includes('teacher') || role.includes('教师') || role.includes('老师')) return 'teacher'
  if (role.includes('student') || role.includes('学生')) return 'student'
  return ''
}

export function getRoleHomePath(role) {
  return ROLE_HOME_PATHS[normalizeRole(role)] || '/login'
}

export function inferRoleFromLoginResult(result = {}) {
  const userInfo = result.userInfo || {}
  const directRoles = [
    result.role,
    result.roleCode,
    result.roleId,
    result.userType,
    ...(Array.isArray(result.roles) ? result.roles : []),
    userInfo.role,
    userInfo.roleCode,
    userInfo.roleName,
    userInfo.roleId,
    userInfo.userType,
    userInfo.username,
    ...(Array.isArray(userInfo.roles) ? userInfo.roles : []),
  ]
  let directRole = ''

  for (const item of directRoles) {
    const role = normalizeRole(item)
    if (role) {
      directRole = role
      break
    }
  }

  const roleText = [
    result.roleName,
    userInfo.realName,
    userInfo.name,
    ...(Array.isArray(result.roles) ? result.roles : []),
    ...(Array.isArray(userInfo.roles) ? userInfo.roles : []),
    ...(result.permissions || []),
    ...collectMenuCodes(result.menus || []),
  ].join(' ').toLowerCase()

  if (
    roleText.includes('admin')
    || roleText.includes('管理员')
    || roleText.includes('/admin')
    || roleText.includes('user:')
    || roleText.includes('role:')
    || roleText.includes('permission:')
    || roleText.includes('用户管理')
    || roleText.includes('角色管理')
    || roleText.includes('权限管理')
  ) return 'admin'

  if (directRole) return directRole
  if (roleText.includes('teacher') || roleText.includes('course:') || roleText.includes('exam:')) return 'teacher'
  if (roleText.includes('student')) return 'student'

  return ''
}
