package com.hotel.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_room")
public class Room {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hotelId;
    private String name;
    private Long bedTypeId;
    private Long breakfastId;
    private Integer maxGuests;
    private String area;
    private String floor;
    private BigDecimal price;
    private Integer cancelable;
    private BigDecimal cancelPenalty;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
