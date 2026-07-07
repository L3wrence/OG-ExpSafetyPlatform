import request from '@/utils/request'

export function getPracticeQuestions(params, config = {}) {
  return request.get('/hse-training/practice', { ...config, params })
}

export function submitPractice(data) {
  return request.post('/hse-training/practice/submit', data)
}

export function getWrongBook(params, config = {}) {
  return request.get('/hse-training/wrong-book', { ...config, params })
}

export function getMyWeakPoints(config = {}) {
  return request.get('/hse-training/weak-points/my', config)
}

export function getClassWeakPoints(courseId, config = {}) {
  return request.get('/hse-training/weak-points/class', { ...config, params: { courseId } })
}

export function favoriteQuestion(questionId) {
  return request.post(`/hse-training/questions/${questionId}/favorite`)
}

export function unfavoriteQuestion(questionId) {
  return request.delete(`/hse-training/questions/${questionId}/favorite`)
}
