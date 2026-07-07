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

export function getReportTemplate(experimentId, config = {}) {
  return request.get(`/reports/experiments/${experimentId}/template`, config)
}

export function saveReportTemplate(experimentId, data) {
  return request.put(`/reports/experiments/${experimentId}/template`, data)
}

export function getReportRubric(experimentId, config = {}) {
  return request.get(`/reports/experiments/${experimentId}/rubric`, config)
}

export function saveReportRubric(experimentId, data) {
  return request.put(`/reports/experiments/${experimentId}/rubric`, data)
}

export function gradeReportWithRubric(id, data) {
  return request.put(`/reports/${id}/rubric-grade`, data)
}
