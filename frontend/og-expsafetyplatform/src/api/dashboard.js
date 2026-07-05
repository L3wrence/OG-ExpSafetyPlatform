import request from '@/utils/request'

const silentConfig = { silent: true }

export function getDashboardOverview(params, config = silentConfig) {
  return request.get('/dashboard/overview', { ...config, params })
}

export function getCourseCompletion(params, config = silentConfig) {
  return request.get('/dashboard/course-completion', { ...config, params })
}

export function getResourceTypeDistribution(params, config = silentConfig) {
  return request.get('/dashboard/resources/type-distribution', { ...config, params })
}

export function getHotResources(params, config = silentConfig) {
  return request.get('/dashboard/resources/hot-ranking', { ...config, params })
}

export function getResourceCompletion(params, config = silentConfig) {
  return request.get('/dashboard/resources/completion', { ...config, params })
}

export function getExamPassRate(params, config = silentConfig) {
  return request.get('/dashboard/exams/pass-rate', { ...config, params })
}

export function getWrongKnowledgeRanking(params, config = silentConfig) {
  return request.get('/dashboard/exams/wrong-knowledge-ranking', { ...config, params })
}

export function getReservationTrend(params, config = silentConfig) {
  return request.get('/dashboard/reservations/trend', { ...config, params })
}

export function getReservationStatusDistribution(params, config = silentConfig) {
  return request.get('/dashboard/reservations/status-distribution', { ...config, params })
}

export function getCapacityUsage(params, config = silentConfig) {
  return request.get('/dashboard/reservations/capacity-usage', { ...config, params })
}

export function getReportScoreDistribution(params, config = silentConfig) {
  return request.get('/dashboard/reports/score-distribution', { ...config, params })
}

export function getLearningActivityTrend(params, config = silentConfig) {
  return request.get('/dashboard/learning/activity-trend', { ...config, params })
}
