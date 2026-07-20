package com.hotel.module.coupon.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CouponCodeUtilsTest {

    @Test
    void shouldGenerateAndValidateCouponCode() {
        String couponCode = CouponCodeUtils.generate(1_000_000_001L);

        assertEquals(9, couponCode.length());
        assertTrue(couponCode.matches("^[0-9A-Z]+$"));
        assertTrue(CouponCodeUtils.isValid(couponCode));
    }

    @Test
    void shouldRejectTamperedCouponCode() {
        String couponCode = CouponCodeUtils.generate(123456789L);
        String tamperedCode = couponCode.substring(0, 8) + "Z";

        assertTrue(!CouponCodeUtils.isValid(tamperedCode));
    }
}
