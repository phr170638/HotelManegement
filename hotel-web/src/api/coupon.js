import request from './request'

export function redeemCoupon(data, config = {}) {
  return request.post('/coupon/redeem', data, config)
}

export function getMyCoupons(config = {}) {
  return request.get('/coupon/my', config)
}
