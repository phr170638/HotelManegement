package com.hotel.module.search.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HotelSearchRequest {
    private Long cityId;
    private String checkInDate;
    private String checkOutDate;
    private Integer roomCount;
    private Integer adults;
    private Integer children;
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer starLevel;
    private BigDecimal score;
    private String sortBy;
    private String sortOrder;
    private Integer page = 1;
    private Integer size = 20;
}
