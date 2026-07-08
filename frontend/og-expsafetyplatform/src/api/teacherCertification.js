import request from '@/utils/request'

export function getMyTeacherCertification() {
  return request.get('/teacher-certifications/my')
}

export function applyTeacherCertification(data) {
  return request.post('/teacher-certifications', data)
}

export function getTeacherCertificationReviews(params = {}) {
  return request.get('/admin/teacher-certifications', { params })
}

export function approveTeacherCertification(id, data = {}) {
  return request.put(`/admin/teacher-certifications/${id}/approve`, data)
}

export function rejectTeacherCertification(id, data = {}) {
  return request.put(`/admin/teacher-certifications/${id}/reject`, data)
}
