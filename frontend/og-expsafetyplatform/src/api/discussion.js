import request from '@/utils/request'

export function getDiscussions(params, config = {}) {
  return request.get('/discussions', { ...config, params })
}

export function getDiscussionDetail(id, config = {}) {
  return request.get(`/discussions/${id}`, config)
}

export function createDiscussion(data) {
  return request.post('/discussions', data)
}

export function replyDiscussion(id, data) {
  return request.post(`/discussions/${id}/replies`, data)
}

export function updateDiscussionStatus(id, status) {
  return request.put(`/discussions/${id}/status`, null, { params: { status } })
}

export function updateDiscussionFeatured(id, featured) {
  return request.put(`/discussions/${id}/featured`, null, { params: { featured } })
}
