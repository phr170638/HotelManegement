package com.hotel.module.payment.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSucceededMessage implements Serializable {

    private Long orderId;
    private String orderNo;
    private Long userId;
    private String tradeNo;
    private BigDecimal amount;
    private String payMethod;
    private LocalDateTime payTime;
}
