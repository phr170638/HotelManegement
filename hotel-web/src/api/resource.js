import request from './request'

// 国家
export function getCountries(params) {
  return request.get('/resource/countries', { params })
}

// 城市
export function getCities(params) {
  return request.get('/resource/cities', { params })
}

export function getHotCities() {
  return request.get('/resource/cities/hot')
}

// 酒店
export function getHotels(params) {
  return request.get('/resource/hotels', { params })
}

export function getHotelDetail(id) {
  return request.get(`/resource/hotels/${id}`)
}

export function createHotel(data) {
  return request.post('/resource/hotels', data)
}

export function updateHotel(id, data) {
  return request.put(`/resource/hotels/${id}`, data)
}

export function deleteHotel(id) {
  return request.delete(`/resource/hotels/${id}`)
}

// 房型
export function getRooms(hotelId) {
  return request.get('/resource/rooms', { params: { hotelId } })
}

export function createRoom(data) {
  return request.post('/resource/rooms', data)
}

// 字典
export function getBedTypes() {
  return request.get('/resource/bed-types')
}

export function getBreakfasts() {
  return request.get('/resource/breakfasts')
}
