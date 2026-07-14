import request from './request'

export function getHotelReviews(hotelId, params, config = {}) {
  return request.get(`/review/hotel/${hotelId}`, { params, ...config })
}

export function createReview(data) {
  return request.post('/review/create', data)
}

export function replyReview(id, reply) {
  return request.put(`/review/${id}/reply`, { reply })
}
