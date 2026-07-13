package com.hotel.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_hotel_facility")
public class HotelFacility {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private String name;
    private String icon;
    private LocalDateTime createTime;
}
