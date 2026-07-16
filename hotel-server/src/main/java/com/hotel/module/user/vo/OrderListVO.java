package com.hotel.module.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderListVO {
    private Long id;
    private String orderNo;
    private Long hotelId;
    private String hotelName;
    private String hotelImage;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;
    private LocalDateTime createTime;
}
