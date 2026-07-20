package com.hotel.module.order.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.payment.mq.PaymentSucceededMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSucceededOrderConsumerTest {

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private PaymentSucceededOrderConsumer consumer;

    @Test
    void shouldUpdatePendingOrderToPaid() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("HT202607170001");
        order.setStatus(0);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        PaymentSucceededMessage message = new PaymentSucceededMessage(
                1L,
                "HT202607170001",
                8L,
                "202607170001",
                new BigDecimal("888"),
                "ALIPAY",
                LocalDateTime.now()
        );

        consumer.onPaymentSucceeded(message);

        assertEquals(1, order.getStatus());
        assertNotNull(order.getPayTime());
        verify(orderMapper).updateById(order);
    }

    @Test
    void shouldIgnoreAlreadyPaidOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("HT202607170001");
        order.setStatus(1);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        consumer.onPaymentSucceeded(new PaymentSucceededMessage(
                1L,
                "HT202607170001",
                8L,
                "202607170001",
                new BigDecimal("888"),
                "ALIPAY",
                LocalDateTime.now()
        ));

        verify(orderMapper, never()).updateById(any(Order.class));
    }
}
