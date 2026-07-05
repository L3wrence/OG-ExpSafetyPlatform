import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
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
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(readJson('userInfo', null))
  const role = ref(normalizeRole(localStorage.getItem('role') || ''))
  const menus = ref(readJson('menus', []))
  const permissions = ref(readJson('permissions', []))

  async function login(loginForm) {
    /** @type {LoginResult} */
    const res = await request.post('/auth/login', {
      username: loginForm.username,
      password: loginForm.password,
    })

    const loginUserInfo = res.userInfo || {}
    const loginRole = inferRoleFromLoginResult(res)

    if (!res.token) {
      throw new Error('登录成功但后端未返回令牌')
    }

    if (!loginRole) {
      throw new Error('登录成功但账号未绑定学生、教师或管理员角色')
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
 
  return { token, userInfo, role, menus, permissions, login, setRole, logout }
})
