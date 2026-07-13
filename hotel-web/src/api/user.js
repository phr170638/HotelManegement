import request from './request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function sendCode(phone, type) {
  return request.post('/user/send-code', null, { params: { phone, type } })
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function updateUser(data) {
  return request.put('/user/update', data)
}

export function getMyOrders(page = 1, size = 10, status = null) {
  return request.get('/user/orders', { params: { page, size, status } })
}
