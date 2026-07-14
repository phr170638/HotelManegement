package com.hotel.module.resource.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HotelVO {
    private Long id;
    private Long cityId;
    private String cityName;
    private String nameCn;
    private String nameEn;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer starLevel;
    private String brand;
    private String description;
    private BigDecimal score;
    private String mainImage;
    private BigDecimal minPrice;
    private Integer reviewCount;
    private List<ImageVO> images;
    private List<String> facilities;
    private List<RoomVO> rooms;

    @Data
    public static class ImageVO {
        private Long id;
        private String url;
        private Integer type;
        private Integer sortOrder;
    }
}
