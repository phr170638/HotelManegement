import request from './request'

export function searchHotels(params) {
  return request.get('/search/hotels', { params })
}

export function searchNearby(params) {
  return request.get('/search/nearby', { params })
}

export function getSuggestions(keyword, cityId) {
  return request.get('/search/suggest', { params: { keyword, cityId } })
}
