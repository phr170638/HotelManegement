import request from './request'

// 国家
export function getCountries(params, config = {}) {
  return request.get('/resource/countries', { params, ...config })
}

// 城市
export function getCities(params, config = {}) {
  return request.get('/resource/cities', { params, ...config })
}

export function getHotCities(config = {}) {
  return request.get('/resource/cities/hot', config)
}

// 酒店
export function getHotels(params, config = {}) {
  return request.get('/resource/hotels', { params, ...config })
}

export function getHotelDetail(id, config = {}) {
  return request.get(`/resource/hotels/${id}`, config)
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
export function getRooms(hotelId, config = {}) {
  return request.get('/resource/rooms', { params: { hotelId }, ...config })
}

export function createRoom(data) {
  return request.post('/resource/rooms', data)
}

// 字典
export function getBedTypes(config = {}) {
  return request.get('/resource/bed-types', config)
}

export function getBreakfasts(config = {}) {
  return request.get('/resource/breakfasts', config)
}
