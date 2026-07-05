import request from '@/utils/request'

export function askAi(data) {
  return request.post('/ai/ask', data)
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
