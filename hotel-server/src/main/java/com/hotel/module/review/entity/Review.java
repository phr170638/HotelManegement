package com.hotel.module.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long hotelId;
    private Long orderId;
    private Integer score;
    private String content;
    private String images;
    private String reply;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
}
