import request from '@/utils/request'

export function getSafetyKnowledge(params, config = {}) {
  return request.get('/safety-knowledge', { ...config, params })
}

export function createSafetyKnowledge(data) {
  return request.post('/safety-knowledge', data)
}

export function updateSafetyKnowledge(id, data) {
  return request.put(`/safety-knowledge/${id}`, data)
}

export function deleteSafetyKnowledge(id) {
  return request.delete(`/safety-knowledge/${id}`)
}
