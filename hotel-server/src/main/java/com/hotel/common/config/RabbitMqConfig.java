package com.hotel.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.payment.mq.PaymentMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange paymentEventExchange() {
        return new DirectExchange(PaymentMqConstants.PAYMENT_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderPaymentSucceededQueue() {
        return new Queue(PaymentMqConstants.ORDER_PAYMENT_SUCCEEDED_QUEUE, true);
    }

    @Bean
    public Queue notificationPaymentSucceededQueue() {
        return new Queue(PaymentMqConstants.NOTIFICATION_PAYMENT_SUCCEEDED_QUEUE, true);
    }

    @Bean
    public Binding orderPaymentSucceededBinding() {
        return BindingBuilder.bind(orderPaymentSucceededQueue())
                .to(paymentEventExchange())
                .with(PaymentMqConstants.PAYMENT_SUCCEEDED_ROUTING_KEY);
    }

    @Bean
    public Binding notificationPaymentSucceededBinding() {
        return BindingBuilder.bind(notificationPaymentSucceededQueue())
                .to(paymentEventExchange())
                .with(PaymentMqConstants.PAYMENT_SUCCEEDED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
