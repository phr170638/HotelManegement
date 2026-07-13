package com.hotel.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_hotel")
public class Hotel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long cityId;
    private String nameCn;
    private String nameEn;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer starLevel;
    private String brand;
    private String description;
    private BigDecimal score;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
