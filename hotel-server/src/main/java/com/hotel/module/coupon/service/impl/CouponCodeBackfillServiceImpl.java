package com.hotel.module.coupon.service.impl;

import com.hotel.module.coupon.entity.Coupon;
import com.hotel.module.coupon.mapper.CouponMapper;
import com.hotel.module.coupon.service.CouponCodeBackfillService;
import com.hotel.module.coupon.util.CouponCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponCodeBackfillServiceImpl implements CouponCodeBackfillService {

    private final CouponMapper couponMapper;

    @Override
    public void backfillMissingReceiveCodes() {
        List<Coupon> coupons = couponMapper.selectPendingReceiveCodeCoupons();
        for (Coupon coupon : coupons) {
            String receiveCode = CouponCodeUtils.generate(coupon.getId());
            int updatedRows = couponMapper.updateReceiveCodeIfBlank(coupon.getId(), receiveCode);
            if (updatedRows > 0) {
                log.info("Coupon receive code generated, couponId={}, receiveCode={}", coupon.getId(), receiveCode);
            }
        }
    }
}
