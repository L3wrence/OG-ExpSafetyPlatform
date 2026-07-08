import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import { logout as logoutApi } from '@/api/auth'
import { inferRoleFromLoginResult, normalizeRole } from '@/utils/role'

/**
 * @typedef {Object} LoginUserInfo
 * @property {string} [role]
 */

/**
 * @typedef {Object} LoginResult
 * @property {string} [token]
 * @property {LoginUserInfo} [userInfo]
 * @property {string} [role]
 * @property {Array} [menus]
 * @property {string[]} [permissions]
 */

function readJson(key, fallback) {
  try {
    return JSON.parse(localStorage.getItem(key) || JSON.stringify(fallback))
  } catch {
    return fallback
  }
}

export const useAuthStore = defineStore('auth', () => {
  const storedUserInfo = readJson('userInfo', null)
  const storedMenus = readJson('menus', [])
  const storedPermissions = readJson('permissions', [])
  const storedRole = normalizeRole(localStorage.getItem('role') || '')
    || inferRoleFromLoginResult({
      userInfo: storedUserInfo,
      menus: storedMenus,
      permissions: storedPermissions,
    })
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(storedUserInfo)
  const role = ref(storedRole)
  const menus = ref(storedMenus)
  const permissions = ref(storedPermissions)

  async function login(loginForm) {
    /** @type {LoginResult} */
    const res = await request.post('/auth/login', {
      username: loginForm.username,
      password: loginForm.password,
    }, { silent: true })

    const loginUserInfo = res.userInfo || {}
    const loginRole = inferRoleFromLoginResult(res) || 'user'

    if (!res.token) {
      throw new Error('登录成功但后端未返回令牌')
    }

    token.value = res.token
    userInfo.value = loginUserInfo
    role.value = loginRole
    menus.value = res.menus || []
    permissions.value = res.permissions || []

    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    localStorage.setItem('role', role.value)
    localStorage.setItem('menus', JSON.stringify(menus.value))
    localStorage.setItem('permissions', JSON.stringify(permissions.value))

    return { ...res, role: role.value }
  }

  function setRole(r) {
    role.value = normalizeRole(r)
    localStorage.setItem('role', role.value)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    role.value = ''
    menus.value = []
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('role')
    localStorage.removeItem('menus')
    localStorage.removeItem('permissions')
  }

  async function logoutRemote() {
    try {
      if (token.value) {
        await logoutApi()
      }
    } finally {
      logout()
    }
  }

  function setUserInfo(nextUserInfo) {
    userInfo.value = nextUserInfo
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  function hasPermission(code) {
    return permissions.value.includes(code)
  }

  return { token, userInfo, role, menus, permissions, login, setRole, logout, logoutRemote, setUserInfo, hasPermission }
})
