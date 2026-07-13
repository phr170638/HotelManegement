import request from './request'

export function getHotelReviews(hotelId, params) {
  return request.get(`/review/hotel/${hotelId}`, { params })
}

export function createReview(data) {
  return request.post('/review/create', data)
}

export function replyReview(id, reply) {
  return request.put(`/review/${id}/reply`, { reply })
}
