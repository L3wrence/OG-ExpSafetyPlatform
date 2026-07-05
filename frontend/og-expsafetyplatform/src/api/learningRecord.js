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
