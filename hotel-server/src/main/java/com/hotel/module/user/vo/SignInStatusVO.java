package com.hotel.module.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class SignInStatusVO {

    private Boolean checkedInToday;
    private Boolean justSignedIn;
    private Integer currentMonthSignInDays;
    private Integer continuousSignInDays;
    private String currentMonth;
    private List<Integer> signInDays;
}
