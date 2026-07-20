package com.hotel.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_coupon")
public class Coupon {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String receiveCode;
    private String description;
    private BigDecimal discountAmount;
    private BigDecimal thresholdAmount;
    private Integer totalNum;
    private Integer issueNum;
    private Integer perUserLimit;
    private Integer status;
    private LocalDateTime receiveStartTime;
    private LocalDateTime receiveEndTime;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
