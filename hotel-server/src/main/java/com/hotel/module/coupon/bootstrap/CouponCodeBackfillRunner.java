package com.hotel.module.coupon.bootstrap;

import com.hotel.module.coupon.service.CouponCodeBackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class CouponCodeBackfillRunner implements ApplicationRunner {

    private final CouponCodeBackfillService couponCodeBackfillService;

    @Override
    public void run(ApplicationArguments args) {
        couponCodeBackfillService.backfillMissingReceiveCodes();
    }
}
