package com.hotel.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAdminVO {
    private Long id;
    private String phone;
    private String email;
    private String nickname;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
}
