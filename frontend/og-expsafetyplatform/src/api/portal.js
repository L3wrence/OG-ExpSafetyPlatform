import request from '@/utils/request'

export function getPortalHome(config = {}) {
  return request.get('/portal/home', config)
}

export function getPortalNotices(params, config = {}) {
  return request.get('/portal/notices', { ...config, params })
}

export function getPortalMessages(params, config = {}) {
  return request.get('/portal/messages', { ...config, params })
}

export function getUnreadMessageCount(config = {}) {
  return request.get('/portal/messages/unread-count', config)
}

export function markMessageRead(id) {
  return request.put(`/portal/messages/${id}/read`)
}

export function getPortalCalendar(params, config = {}) {
  return request.get('/portal/calendar', { ...config, params })
}

export function searchPortal(params, config = {}) {
  return request.get('/portal/search', { ...config, params })
}

export function getRecentVisits(params, config = {}) {
  return request.get('/portal/recent-visits', { ...config, params })
}

export function recordRecentVisit(data) {
  return request.post('/portal/recent-visits', data, { silent: true })
}

export function getShortcuts(params, config = {}) {
  return request.get('/portal/shortcuts', { ...config, params })
}

export function saveShortcuts(data) {
  return request.put('/portal/shortcuts', data)
}
