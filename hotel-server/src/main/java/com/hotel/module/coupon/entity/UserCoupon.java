package com.hotel.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long couponId;
    private String couponName;
    private String receiveCode;
    private String description;
    private BigDecimal discountAmount;
    private BigDecimal thresholdAmount;
    private Integer status;
    private LocalDateTime receiveTime;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private LocalDateTime useTime;
    private LocalDateTime createTime;
}
