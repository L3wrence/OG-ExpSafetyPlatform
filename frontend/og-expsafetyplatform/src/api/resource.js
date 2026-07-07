import request from '@/utils/request'

export function getResources(params, config = {}) {
  return request.get('/resources', { ...config, params })
}

export function getResourceDetail(id) {
  return request.get(`/resources/${id}`)
}

export function getResourcePreview(id) {
  return request.get(`/resources/${id}/preview`)
}

export function getResourceFileBlob(id) {
  return request.get(`/files/resources/${id}`, { responseType: 'blob', silent: true })
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

export function uploadResource(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/resources/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function markResourceDownload(id) {
  return request.post(`/resources/${id}/download`)
}

export function interactResource(id, data) {
  return request.post(`/resources/${id}/interaction`, data)
}

export function markResourceInvalid(id, invalidFlag) {
  return request.put(`/resources/${id}/invalid`, null, { params: { invalidFlag } })
}

export function getResourceStats(id) {
  return request.get(`/resources/${id}/stats`)
}
