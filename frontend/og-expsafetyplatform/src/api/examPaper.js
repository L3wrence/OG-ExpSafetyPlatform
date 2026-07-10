import request from '@/utils/request'

export function getExamPapers(params, config = {}) {
  return request.get('/exams/papers', { ...config, params })
}

export function getExamPaperDetail(id, config = {}) {
  return request.get(`/exams/papers/${id}`, config)
}

export function createExamPaper(data) {
  return request.post('/exams/papers', data)
}

export function updateExamPaper(id, data) {
  return request.put(`/exams/papers/${id}`, data)
}

export function deleteExamPaper(id) {
  return request.delete(`/exams/papers/${id}`)
}

export function updateExamPaperStatus(id, status) {
  return request.put(`/exams/papers/${id}/status`, { status })
}

export function addPaperQuestions(id, data) {
  return request.post(`/exams/papers/${id}/questions`, data)
}

export function removePaperQuestion(id, questionId) {
  return request.delete(`/exams/papers/${id}/questions/${questionId}`)
}

export function reorderPaperQuestions(id, data) {
  return request.put(`/exams/papers/${id}/questions/order`, data)
}

export function smartAssemblePaper(id, data) {
  return request.post(`/exams/papers/${id}/smart-assemble`, data)
}
