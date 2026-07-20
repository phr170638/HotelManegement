package com.hotel.module.coupon.service;

import com.hotel.module.coupon.vo.UserCouponVO;

import java.util.List;

public interface CouponService {

    UserCouponVO redeemCoupon(Long userId, String code);

    List<UserCouponVO> getMyCoupons(Long userId);

    UserCouponVO checkAndCreateUserCoupon(Long couponId, Long userId, String code);
}
