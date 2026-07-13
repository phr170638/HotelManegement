package com.hotel.module.search.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HotelSearchVO {
    private Long id;
    private String nameCn;
    private String nameEn;
    private String mainImage;
    private Integer starLevel;
    private BigDecimal score;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String brand;
    private BigDecimal minPrice;
    private Integer reviewCount;
    private List<String> facilities;
}
