package com.hotel.module.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long hotelId;
    private String hotelName;
    private String hotelAddress;
    private BigDecimal hotelLongitude;
    private BigDecimal hotelLatitude;
    private Integer hotelStarLevel;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer roomCount;
    private String guestName;
    private String guestPhone;
    private Long userCouponId;
    private String couponName;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private List<OrderItemVO> items;

    @Data
    public static class OrderItemVO {
        private Long id;
        private String roomName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}
