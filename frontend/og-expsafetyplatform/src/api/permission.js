import request from '@/utils/request'

export function getPermissionTree() {
  return request.get('/permissions/tree')
}
