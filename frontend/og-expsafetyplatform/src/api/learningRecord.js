import request from '@/utils/request'

export function updateLearningProgress(data) {
  return request.put('/learning-records/progress', data)
}

export function getMyLearningRecords(config = {}) {
  return request.get('/learning-records/my', config)
}

export function getExperimentLearningProgress(experimentId, config = {}) {
  return request.get(`/learning-records/experiments/${experimentId}/progress`, config)
}

export function getMyStepLearningRecords(config = {}) {
  return request.get('/learning-records/steps/my', config)
}

export function completeStepLearning(stepId) {
  return request.put(`/learning-records/steps/${stepId}/complete`)
}
