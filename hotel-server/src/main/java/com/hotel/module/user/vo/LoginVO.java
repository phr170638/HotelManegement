package com.hotel.module.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String nickname;
    private List<String> roles;
}
