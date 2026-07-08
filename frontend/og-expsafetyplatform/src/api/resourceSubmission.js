import request from '@/utils/request'

export function getPublicResources(params = {}) {
  return request.get('/public/resources', { params })
}

export function getPublicResourcePreview(id) {
  return request.get(`/public/resources/${id}/preview`)
}

export function submitResource(data) {
  return request.post('/resource-submissions', data)
}

export function getMyResourceSubmissions() {
  return request.get('/resource-submissions/my')
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
