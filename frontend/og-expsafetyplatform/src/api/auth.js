import request from '@/utils/request'

export function register(data) {
  return request.post('/auth/register', data)
}

export function logout() {
  return request.post('/auth/logout', null, { silent: true })
}

export function changePassword(data) {
  return request.put('/auth/password', data)
}
