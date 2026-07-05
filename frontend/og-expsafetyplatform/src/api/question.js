import request from '@/utils/request'

export function getQuestions(params, config = {}) {
  return request.get('/questions', { ...config, params })
}

export function getQuestionDetail(id, config = {}) {
  return request.get(`/questions/${id}`, config)
}

export function createQuestion(data) {
  return request.post('/questions', data)
}

export function updateQuestion(id, data) {
  return request.put(`/questions/${id}`, data)
}

export function deleteQuestion(id) {
  return request.delete(`/questions/${id}`)
}
