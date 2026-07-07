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

export function publishCourse(id, allowEmpty = false) {
  return request.put(`/courses/${id}/publish`, null, { params: { allowEmpty } })
}

export function archiveCourse(id) {
  return request.put(`/courses/${id}/archive`)
}

export function copyCourse(id) {
  return request.post(`/courses/${id}/copy`)
}

export function getCourseClasses(courseId) {
  return request.get(`/courses/${courseId}/classes`)
}

export function createCourseClass(courseId, data) {
  return request.post(`/courses/${courseId}/classes`, data)
}

export function updateCourseClass(courseId, classId, data) {
  return request.put(`/courses/${courseId}/classes/${classId}`, data)
}

export function deleteCourseClass(courseId, classId) {
  return request.delete(`/courses/${courseId}/classes/${classId}`)
}

export function getCourseStudents(courseId, params = {}) {
  return request.get(`/courses/${courseId}/students`, { params })
}

export function importCourseStudents(courseId, data) {
  return request.post(`/courses/${courseId}/students/import`, data)
}

export function removeCourseStudents(courseId, studentIds) {
  return request.delete(`/courses/${courseId}/students`, { data: { studentIds } })
}
