import request from '@/utils/request'

export function getTimeSlots(params, config = {}) {
  return request.get('/reservations/time-slots', { ...config, params })
}

export function createTimeSlots(data) {
  return request.post('/reservations/time-slots', data)
}

export function updateTimeSlot(id, data) {
  return request.put(`/reservations/time-slots/${id}`, data)
}

export function deleteTimeSlot(id) {
  return request.delete(`/reservations/time-slots/${id}`)
}

export function getAvailableSlots(params, config = {}) {
  return request.get('/reservations/available-slots', { ...config, params })
}

export function createReservation(data) {
  return request.post('/reservations', data)
}

export function getMyReservations(params, config = {}) {
  return request.get('/reservations/my', { ...config, params })
}

export function cancelReservation(id) {
  return request.put(`/reservations/${id}/cancel`)
}

export function getPendingReservations(params, config = {}) {
  return request.get('/reservations/pending', { ...config, params })
}

export function reviewReservation(id, data) {
  return request.put(`/reservations/${id}/review`, data)
}
