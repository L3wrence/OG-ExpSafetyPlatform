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

function resourceFormData(data, file) {
  const formData = new FormData()
  formData.append('metadata', new Blob([JSON.stringify(data)], { type: 'application/json' }))
  if (file) formData.append('file', file)
  return formData
}

export function createResource(courseId, data, file, onUploadProgress) {
  return request.post(`/courses/${courseId}/resources`, resourceFormData(data, file), { timeout: 600000, onUploadProgress })
}

export function updateResource(courseId, id, data, file, onUploadProgress) {
  return request.put(`/courses/${courseId}/resources/${id}`, resourceFormData(data, file), { timeout: 600000, onUploadProgress })
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

export function getResourceTimelineNotes(id, params = {}) {
  return request.get(`/resources/${id}/timeline-notes`, { params })
}

export function createResourceTimelineNote(id, data) {
  return request.post(`/resources/${id}/timeline-notes`, data)
}

export function deleteResourceTimelineNote(noteId) {
  return request.delete(`/resources/timeline-notes/${noteId}`)
}

export function getResourceTimelineHotspots(experimentId) {
  return request.get(`/resources/experiments/${experimentId}/timeline-hotspots`)
}
