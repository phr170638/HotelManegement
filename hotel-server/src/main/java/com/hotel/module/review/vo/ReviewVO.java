package com.hotel.module.review.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewVO {
    private Long id;
    private UserInfo user;
    private Integer score;
    private String content;
    private Boolean anonymous;
    private List<String> images;
    private String reply;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;

    @Data
    public static class UserInfo {
        private Long id;
        private String nickname;
        private String avatar;
    }
}
