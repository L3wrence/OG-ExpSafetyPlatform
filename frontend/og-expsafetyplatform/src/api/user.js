import request from '@/utils/request'

export function getUsers(params) {
  return request.get('/users', { params })
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(data) {
  return request.put('/users', data)
}

export function deleteUser(id) {
  return request.delete('/users', { data: { id } })
}

export function getMyProfile(config = {}) {
  return request.get('/users/me', config)
}

export function updateMyProfile(data) {
  return request.put('/users/me', data)
}
