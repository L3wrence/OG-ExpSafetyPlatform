import request from '@/utils/request'

export function getResources(params, config = {}) {
  return request.get('/resources', { ...config, params })
}

export function createResource(data) {
  return request.post('/resources', data)
}

export function updateResource(id, data) {
  return request.put(`/resources/${id}`, data)
}

export function deleteResource(id) {
  return request.delete(`/resources/${id}`)
}

export function updateResourceStatus(id, status) {
  return request.put(`/resources/${id}/status`, null, { params: { status } })
}

export function viewResource(id) {
  return request.post(`/resources/${id}/view`)
}
