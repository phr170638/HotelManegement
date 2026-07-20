package com.hotel.module.payment.service;

import java.util.Map;

public interface AlipayService {

    Map<String, Object> createPagePay(Long orderId);

    String handleNotify(Map<String, String> params);

    boolean syncPaymentStatus(Long orderId);
}
