import request from '@/utils/request'

const AI_REQUEST_CONFIG = { timeout: 30000 }

export function askAi(data) {
  return request.post('/ai/ask', data, AI_REQUEST_CONFIG)
}

export function explainExamAnswer(answerId) {
  return request.post(`/ai/exam-answers/${answerId}/explain`, null, AI_REQUEST_CONFIG)
}

export function precheckReport(data) {
  return request.post('/ai/reports/precheck', data, AI_REQUEST_CONFIG)
}

export function getAiRecords(params, config = {}) {
  return request.get('/ai/records', { ...config, params })
}

export function feedbackAiRecord(id, data) {
  return request.put(`/ai/records/${id}/feedback`, data)
}

export function getRecommendedResources(params, config = {}) {
  return request.get('/recommendations/resources', { ...config, params })
}

export function refreshRecommendedResources(data) {
  return request.post('/recommendations/resources', data)
}
