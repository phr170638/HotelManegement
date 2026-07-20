package com.hotel.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OrderCreateRequest {
    @NotNull(message = "酒店ID不能为空")
    private Long hotelId;

    @NotNull(message = "入住日期不能为空")
    private LocalDate checkInDate;

    @NotNull(message = "退房日期不能为空")
    private LocalDate checkOutDate;

    private Integer roomCount = 1;

    @NotBlank(message = "入住人姓名不能为空")
    private String guestName;

    @NotBlank(message = "入住人手机号不能为空")
    private String guestPhone;

    private Long userCouponId;

    @NotEmpty(message = "订单明细不能为空")
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotNull(message = "房型ID不能为空")
        private Long roomId;
        private Integer quantity = 1;
    }
}
