import request from './request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function sendCode(email, type = 'register', config = {}) {
  return request.post('/user/send-code', null, { params: { email, type }, ...config })
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function updateUser(data) {
  return request.put('/user/update', data)
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getMyOrders(page = 1, size = 10, status = null, config = {}) {
  return request.get('/user/orders', { params: { page, size, status }, ...config })
}

export function signIn(config = {}) {
  return request.post('/user/sign-in', null, config)
}

export function getSignInStatus(config = {}) {
  return request.get('/user/sign-in/status', config)
}
