import request from '@/utils/request'

export function getAdminNotices(params) {
  return request.get('/admin/notices', { params })
}

export function createAdminNotice(data) {
  return request.post('/admin/notices', data)
}

export function updateAdminNotice(id, data) {
  return request.put(`/admin/notices/${id}`, data)
}

export function publishAdminNotice(id) {
  return request.put(`/admin/notices/${id}/publish`)
}

export function offlineAdminNotice(id) {
  return request.put(`/admin/notices/${id}/offline`)
}

export function deleteAdminNotice(id) {
  return request.delete(`/admin/notices/${id}`)
}

export function getOperationLogs(params) {
  return request.get('/admin/operation-logs', { params })
}

export function runReminderScan() {
  return request.post('/admin/reminders/run')
}
