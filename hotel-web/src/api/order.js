import request from './request'

export function createOrder(data) {
  return request.post('/order/create', data)
}

export function payOrder(id, config = {}) {
  return request.post(`/order/${id}/pay`, null, config)
}

export function mockPaySuccess(id) {
  return request.post(`/order/${id}/mock-pay-success`)
}

export function cancelOrder(id) {
  return request.put(`/order/${id}/cancel`)
}

export function preCancelOrder(id) {
  return request.post(`/order/${id}/pre-cancel`)
}

export function confirmCancelOrder(id, cancelConfirmId) {
  return request.post(`/order/${id}/confirm-cancel`, null, { params: { cancelConfirmId } })
}

// 管理端
export function getAdminOrders(params, config = {}) {
  return request.get('/admin/orders', { params, ...config })
}

export function refundOrder(id, reason) {
  return request.put(`/admin/orders/${id}/refund`, null, { params: { reason } })
}
