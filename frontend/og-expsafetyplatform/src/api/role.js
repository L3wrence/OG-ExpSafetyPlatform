import request from '@/utils/request'

export function getRoles() {
  return request.get('/roles')
}

export function getRolePermissions(roleId) {
  return request.get(`/roles/${roleId}/permissions`)
}

export function saveRolePermissions(roleId, permissionIds) {
  return request.put(`/roles/${roleId}/permissions`, permissionIds)
}
