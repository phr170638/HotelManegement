package com.hotel.module.notification.consumer;

import com.hotel.module.notification.service.MailService;
import com.hotel.module.payment.mq.PaymentMqConstants;
import com.hotel.module.payment.mq.PaymentSucceededMessage;
import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSucceededNotificationConsumer {

    private final UserMapper userMapper;
    private final MailService mailService;

    @RabbitListener(queues = PaymentMqConstants.NOTIFICATION_PAYMENT_SUCCEEDED_QUEUE)
    public void onPaymentSucceeded(PaymentSucceededMessage message) {
        User user = userMapper.selectById(message.getUserId());
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            log.info("Skip payment success mail because user/email missing, orderNo={}", message.getOrderNo());
            return;
        }
        mailService.sendPaymentSuccessMail(user.getEmail(), message.getOrderNo());
    }
}
