package com.hotel.module.payment.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentSucceeded(PaymentSucceededMessage message) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(message);
                }
            });
            return;
        }
        doPublish(message);
    }

    private void doPublish(PaymentSucceededMessage message) {
        // #region debug-point payment-result-stuck-publish
        log.info("payment event publish: orderId={}, orderNo={}, userId={}, routingKey={}",
                message.getOrderId(), message.getOrderNo(), message.getUserId(), PaymentMqConstants.PAYMENT_SUCCEEDED_ROUTING_KEY);
        // #endregion
        rabbitTemplate.convertAndSend(
                PaymentMqConstants.PAYMENT_EVENT_EXCHANGE,
                PaymentMqConstants.PAYMENT_SUCCEEDED_ROUTING_KEY,
                message
        );
    }
}
