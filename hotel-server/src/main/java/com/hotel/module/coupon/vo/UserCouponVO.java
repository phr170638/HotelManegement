package com.hotel.module.coupon.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO {

    private Long id;
    private Long couponId;
    private String couponName;
    private String receiveCode;
    private String description;
    private BigDecimal discountAmount;
    private BigDecimal thresholdAmount;
    private Integer status;
    private String statusText;
    private LocalDateTime receiveTime;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
}
