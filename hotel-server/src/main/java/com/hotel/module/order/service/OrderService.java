package com.hotel.module.order.service;

import com.hotel.common.result.PageResult;
import com.hotel.module.order.dto.OrderCreateRequest;
import com.hotel.module.order.vo.OrderVO;

import java.util.Map;

public interface OrderService {

    OrderVO create(Long userId, OrderCreateRequest request);

    PageResult<OrderVO> listByUser(Long userId, Integer page, Integer size, Integer status);

    OrderVO detail(Long orderId);

    void cancel(Long userId, Long orderId);

    Map<String, Object> preCancel(Long userId, Long orderId);

    void confirmCancel(Long userId, Long orderId, String cancelConfirmId);

    String getPayForm(Long userId, Long orderId);

    void handlePayNotify(Map<String, String> params);
}
