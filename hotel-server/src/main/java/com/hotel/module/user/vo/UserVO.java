package com.hotel.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String phone;
    private String email;
    private String nickname;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
    private LocalDateTime createTime;
}
