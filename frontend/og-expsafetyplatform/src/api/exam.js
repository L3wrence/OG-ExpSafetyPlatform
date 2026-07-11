import request from '@/utils/request'

export function getAvailableExams(params, config = {}) {
  return request.get('/exams/available', { ...config, params })
}

export function startExam(paperId) {
  return request.post(`/exams/${paperId}/start`)
}

export function getInProgressExam(params = {}, config = {}) {
  return request.get('/exams/in-progress', { ...config, params })
}

export function submitExam(recordId, data) {
  return request.post(`/exams/${recordId}/submit`, data)
}

export function saveExamAnswers(recordId, data) {
  return request.put(`/exams/${recordId}/answers`, data)
}

export function getAdmissionStatus(experimentId, config = {}) {
  return request.get(`/exams/admissions/${experimentId}`, config)
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

export function getPendingGradingRecords(params, config = {}) {
  return request.get('/exams/statistics/pending-grading', { ...config, params })
}

export function getPaperSubmissionRecords(params, config = {}) {
  return request.get('/exams/statistics/submissions', { ...config, params })
}

export function getGradingRecordDetail(recordId, config = {}) {
  return request.get(`/exams/statistics/grading-records/${recordId}`, config)
}

export function gradeShortAnswers(recordId, data) {
  return request.put('/exams/statistics/grade-short-answer', data, { params: { recordId } })
}
