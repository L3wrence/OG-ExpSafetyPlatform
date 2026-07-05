import request from '@/utils/request'

export function getCourses(params, config = {}) {
  return request.get('/courses', { ...config, params })
}

export function getCourseDetail(id, config = {}) {
  return request.get(`/courses/${id}`, config)
}

export function createCourse(data) {
  return request.post('/courses', data)
}

export function updateCourse(id, data) {
  return request.put(`/courses/${id}`, data)
}

export function deleteCourse(id) {
  return request.delete(`/courses/${id}`)
}

export function updateCourseStatus(id, status) {
  return request.put(`/courses/${id}/status`, null, { params: { status } })
}
