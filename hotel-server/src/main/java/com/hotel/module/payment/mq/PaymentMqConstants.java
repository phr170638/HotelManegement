package com.hotel.module.payment.mq;

public final class PaymentMqConstants {

    public static final String PAYMENT_EVENT_EXCHANGE = "hotel.payment.event.exchange";
    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.succeeded";
    public static final String ORDER_PAYMENT_SUCCEEDED_QUEUE = "hotel.order.payment-succeeded.queue";
    public static final String NOTIFICATION_PAYMENT_SUCCEEDED_QUEUE = "hotel.notification.payment-succeeded.queue";

    private PaymentMqConstants() {
    }
}
