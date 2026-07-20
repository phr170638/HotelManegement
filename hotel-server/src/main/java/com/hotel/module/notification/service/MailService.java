package com.hotel.module.notification.service;

public interface MailService {

    void sendVerificationCodeMail(String email, String bizType, String code, long expireMinutes);

    void sendPaymentSuccessMail(String email, String orderNo);
}
