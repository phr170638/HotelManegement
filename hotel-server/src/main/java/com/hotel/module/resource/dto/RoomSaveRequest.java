package com.hotel.module.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomSaveRequest {
    @NotNull(message = "酒店ID不能为空")
    private Long hotelId;

    @NotBlank(message = "房型名称不能为空")
    private String name;

    private Long bedTypeId;
    private Long breakfastId;
    private Integer maxGuests;
    private String area;
    private String floor;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private Integer cancelable;
    private BigDecimal cancelPenalty;
}
