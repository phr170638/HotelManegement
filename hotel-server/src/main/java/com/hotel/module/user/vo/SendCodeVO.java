package com.hotel.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendCodeVO {
    private Integer expireInSeconds;
    private Integer resendIntervalInSeconds;
    private String debugCode;
}
