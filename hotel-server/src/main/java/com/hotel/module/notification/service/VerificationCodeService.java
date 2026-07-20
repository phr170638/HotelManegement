package com.hotel.module.notification.service;

import com.hotel.module.user.vo.SendCodeVO;

public interface VerificationCodeService {

    SendCodeVO sendEmailCode(String email, String type);

    void validateEmailCode(String email, String type, String code);
}
