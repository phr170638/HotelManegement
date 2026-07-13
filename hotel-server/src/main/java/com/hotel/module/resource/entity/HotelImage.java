package com.hotel.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_hotel_image")
public class HotelImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private String url;
    private Integer type;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
