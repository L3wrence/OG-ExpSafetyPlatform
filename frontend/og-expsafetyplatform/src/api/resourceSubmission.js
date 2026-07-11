import request from '@/utils/request'

export function getPublicResources(params = {}) {
  return request.get('/public/resources', { params })
}

export function getPublicResourcePreview(id) {
  return request.get(`/public/resources/${id}/preview`)
}

export function submitResource(data, file, onUploadProgress) {
  const formData = new FormData()
  formData.append('metadata', new Blob([JSON.stringify(data)], { type: 'application/json' }))
  formData.append('file', file)
  return request.post('/resource-submissions', formData, { timeout: 600000, onUploadProgress })
}

export function getMyResourceSubmissions() {
  return request.get('/resource-submissions/my')
}

export function getSubmissionFileBlob(id) {
  return request.get(`/files/resource-submissions/${id}`, { responseType: 'blob', silent: true })
}

export function getResourceSubmissionReviews(params = {}) {
  return request.get('/admin/resource-submissions', { params })
}

export function approveResourceSubmission(id, data = {}) {
  return request.put(`/admin/resource-submissions/${id}/approve`, data)
}

export function rejectResourceSubmission(id, data = {}) {
  return request.put(`/admin/resource-submissions/${id}/reject`, data)
}
