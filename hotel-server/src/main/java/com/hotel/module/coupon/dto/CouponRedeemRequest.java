package com.hotel.module.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CouponRedeemRequest {

    @NotBlank(message = "兑换码不能为空")
    private String code;
}
