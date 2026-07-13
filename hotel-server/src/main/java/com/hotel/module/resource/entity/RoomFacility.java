package com.hotel.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_room_facility")
public class RoomFacility {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String name;
    private LocalDateTime createTime;
}
