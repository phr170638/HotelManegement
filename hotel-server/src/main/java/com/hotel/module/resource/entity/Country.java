package com.hotel.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_country")
public class Country {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String nameCn;
    private String nameEn;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
