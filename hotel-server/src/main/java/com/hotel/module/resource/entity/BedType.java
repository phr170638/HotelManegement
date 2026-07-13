package com.hotel.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_bed_type")
public class BedType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private LocalDateTime createTime;
}
