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
  const formData = new FormData()
  const metadata = steps.map(({ _file, ...step }) => step)
  formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
  steps.forEach((step) => { if (step._file) formData.append(`file_${step.stepNo}`, step._file) })
  return request.post(`/experiments/${id}/steps`, formData, { timeout: 600000 })
}

export function getExperimentStepFileBlob(stepId) {
  return request.get(`/files/experiment-steps/${stepId}`, { responseType: 'blob', silent: true })
}
