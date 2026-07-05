import request from '@/utils/request'

export function getAvailableExams(params, config = {}) {
  return request.get('/exams/available', { ...config, params })
}

export function startExam(paperId) {
  return request.post(`/exams/${paperId}/start`)
}

export function submitExam(recordId, data) {
  return request.post(`/exams/${recordId}/submit`, data)
}

export function getExamRecords(params, config = {}) {
  return request.get('/exams/records', { ...config, params })
}

export function getExamRecordDetail(id, config = {}) {
  return request.get(`/exams/records/${id}`, config)
}

export function getWrongQuestions(params, config = {}) {
  return request.get('/exams/wrong-questions', { ...config, params })
}

export function getWrongQuestionStats(config = {}) {
  return request.get('/exams/wrong-questions/stats', config)
}
