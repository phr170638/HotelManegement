package com.hotel.module.resource.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomVO {
    private Long id;
    private String name;
    private String bedType;
    private String breakfast;
    private Integer maxGuests;
    private String area;
    private String floor;
    private BigDecimal price;
    private Integer cancelable;
    private BigDecimal cancelPenalty;
    private List<String> images;
    private List<String> facilities;
}
