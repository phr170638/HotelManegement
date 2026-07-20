package com.hotel.module.order.consumer;

import com.hotel.module.order.entity.Order;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.coupon.mapper.UserCouponMapper;
import com.hotel.module.payment.mq.PaymentMqConstants;
import com.hotel.module.payment.mq.PaymentSucceededMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSucceededOrderConsumer {

    private final OrderMapper orderMapper;
    private final UserCouponMapper userCouponMapper;

    @RabbitListener(queues = PaymentMqConstants.ORDER_PAYMENT_SUCCEEDED_QUEUE)
    @Transactional
    public void onPaymentSucceeded(PaymentSucceededMessage message) {
        // #region debug-point payment-result-stuck-consume
        log.info("payment event consumed: orderId={}, orderNo={}, tradeNo={}, payTime={}",
                message.getOrderId(), message.getOrderNo(), message.getTradeNo(), message.getPayTime());
        // #endregion
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, message.getOrderNo()).last("limit 1")
        );
        if (order == null) {
            log.warn("Payment success event ignored because order not found, orderNo={}", message.getOrderNo());
            return;
        }
        LocalDateTime payTime = message.getPayTime() == null ? LocalDateTime.now() : message.getPayTime();
        if (order.getUserCouponId() != null) {
            userCouponMapper.markUsed(order.getUserCouponId(), order.getUserId(), payTime);
        }
        if (Objects.equals(order.getStatus(), 1)) {
            // #region debug-point payment-result-stuck-consume-idempotent
            log.info("payment event ignored because order already paid: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
            // #endregion
            return;
        }
        order.setStatus(1);
        order.setPayTime(payTime);
        orderMapper.updateById(order);
        // #region debug-point payment-result-stuck-consume-updated
        log.info("payment event updated order paid: orderId={}, orderNo={}, newStatus={}", order.getId(), order.getOrderNo(), order.getStatus());
        // #endregion
    }
}
