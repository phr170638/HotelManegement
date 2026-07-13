package com.hotel.module.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HotelSaveRequest {
    @NotNull(message = "城市ID不能为空")
    private Long cityId;

    @NotBlank(message = "酒店名称不能为空")
    private String nameCn;

    private String nameEn;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer starLevel;
    private String brand;
    private String description;
    private List<String> imageUrls;
    private List<String> facilities;
}
