package com.hotel.module.coupon.controller;

import com.hotel.common.result.R;
import com.hotel.module.coupon.dto.CouponRedeemRequest;
import com.hotel.module.coupon.service.CouponService;
import com.hotel.module.coupon.vo.UserCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "优惠券模块", description = "用户输入兑换码领取优惠券")
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "兑换优惠券")
    @PostMapping("/redeem")
    public R<UserCouponVO> redeemCoupon(@AuthenticationPrincipal Long userId, @Valid @RequestBody CouponRedeemRequest request) {
        return R.ok("领取成功", couponService.redeemCoupon(userId, request.getCode()));
    }

    @Operation(summary = "我的优惠券")
    @GetMapping("/my")
    public R<List<UserCouponVO>> myCoupons(@AuthenticationPrincipal Long userId) {
        return R.ok(couponService.getMyCoupons(userId));
    }
}
