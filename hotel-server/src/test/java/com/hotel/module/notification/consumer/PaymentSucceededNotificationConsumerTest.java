package com.hotel.module.notification.consumer;

import com.hotel.module.notification.service.MailService;
import com.hotel.module.payment.mq.PaymentSucceededMessage;
import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSucceededNotificationConsumerTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private MailService mailService;

    @InjectMocks
    private PaymentSucceededNotificationConsumer consumer;

    @Test
    void shouldSendMailWhenUserHasEmail() {
        User user = new User();
        user.setId(8L);
        user.setEmail("user@example.com");
        when(userMapper.selectById(8L)).thenReturn(user);

        consumer.onPaymentSucceeded(buildMessage());

        verify(mailService).sendPaymentSuccessMail("user@example.com", "HT202607170001");
    }

    @Test
    void shouldSkipMailWhenEmailMissing() {
        User user = new User();
        user.setId(8L);
        when(userMapper.selectById(8L)).thenReturn(user);

        consumer.onPaymentSucceeded(buildMessage());

        verify(mailService, never()).sendPaymentSuccessMail(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private PaymentSucceededMessage buildMessage() {
        return new PaymentSucceededMessage(
                1L,
                "HT202607170001",
                8L,
                "202607170001",
                new BigDecimal("888"),
                "ALIPAY",
                LocalDateTime.now()
        );
    }
}
