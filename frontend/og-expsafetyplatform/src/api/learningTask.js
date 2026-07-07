import request from '@/utils/request'

export function getLearningTasks(params) {
  return request.get('/learning-tasks', { params })
}

export function createLearningTask(data) {
  return request.post('/learning-tasks', data)
}

export function updateLearningTask(id, data) {
  return request.put(`/learning-tasks/${id}`, data)
}

export function disableLearningTask(id) {
  return request.delete(`/learning-tasks/${id}`)
}

export function getLearningPath(experimentId) {
  return request.get(`/learning-tasks/experiments/${experimentId}/path`)
}

export function confirmChecklistTask(id) {
  return request.post(`/learning-tasks/${id}/confirm-checklist`)
}

export function completeSafetyKnowledgeTask(knowledgeId) {
  return request.post(`/learning-tasks/safety-knowledge/${knowledgeId}/complete`)
}

export function getTaskDistribution(experimentId) {
  return request.get(`/learning-tasks/experiments/${experimentId}/distribution`)
}
