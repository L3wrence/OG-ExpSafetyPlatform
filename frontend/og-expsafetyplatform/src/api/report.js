import request from '@/utils/request'

export function createReport(data) {
  return request.post('/reports', data)
}

export function updateReport(id, data) {
  return request.put(`/reports/${id}`, data)
}

export function submitReport(id) {
  return request.put(`/reports/${id}/submit`)
}

export function getMyReports(params, config = {}) {
  return request.get('/reports/my', { ...config, params })
}

export function getReportDetail(id, config = {}) {
  return request.get(`/reports/${id}`, config)
}

export function getPendingReports(params, config = {}) {
  return request.get('/reports/pending', { ...config, params })
}

export function gradeReport(id, data) {
  return request.put(`/reports/${id}/grade`, data)
}

export function returnReport(id, data) {
  return request.put(`/reports/${id}/return`, data)
}
