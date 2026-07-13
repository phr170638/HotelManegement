package com.hotel.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_city")
public class City {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long countryId;
    private String nameCn;
    private String nameEn;
    private String code;
    private Integer hot;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
