import request from './request'

export function searchHotels(params, config = {}) {
  return request.get('/search/hotels', { params, ...config })
}

export function searchNearby(params, config = {}) {
  return request.get('/search/nearby', { params, ...config })
}

export function getSuggestions(keyword, cityId, config = {}) {
  return request.get('/search/suggest', { params: { keyword, cityId }, ...config })
}
