import request from '@/utils/request'

export function getExperiments(params, config = {}) {
  return request.get('/experiments', { ...config, params })
}

export function getExperimentDetail(id, config = {}) {
  return request.get(`/experiments/${id}`, config)
}

export function getExperimentOverview(id, config = {}) {
  return request.get(`/experiments/${id}/overview`, config)
}

export function createExperiment(data) {
  return request.post('/experiments', data)
}

export function updateExperiment(id, data) {
  return request.put(`/experiments/${id}`, data)
}

export function deleteExperiment(id) {
  return request.delete(`/experiments/${id}`)
}

export function updateExperimentStatus(id, status) {
  return request.put(`/experiments/${id}/status`, null, { params: { status } })
}

export function saveExperimentSteps(id, steps) {
  return request.post(`/experiments/${id}/steps`, steps)
}
