package com.hotel.module.payment.service.impl;

import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.common.exception.BusinessException;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.payment.config.AlipayConfig;
import com.hotel.module.payment.entity.Payment;
import com.hotel.module.payment.mapper.PaymentMapper;
import com.hotel.module.payment.service.AlipayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayConfig alipayConfig;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;

    @Override
    public Map<String, Object> createPagePay(Long orderId) {
        validateConfig();

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new BusinessException("当前订单状态不支持发起支付");
        }
        if (!StringUtils.hasText(order.getOrderNo()) || order.getTotalAmount() == null) {
            throw new BusinessException("订单信息不完整，无法发起支付");
        }

        upsertPendingPayment(order);

        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayConfig.getNotifyUrl());
            request.setReturnUrl(alipayConfig.getReturnUrl());
            request.setBizContent(JSONUtil.toJsonStr(Map.of(
                    "out_trade_no", order.getOrderNo(),
                    "total_amount", order.getTotalAmount(),
                    "subject", "酒店订单支付-" + order.getOrderNo(),
                    "product_code", "FAST_INSTANT_TRADE_PAY"
            )));

            AlipayTradePagePayResponse response = buildClient().pageExecute(request, "GET");
            if (!response.isSuccess() || !StringUtils.hasText(response.getBody())) {
                throw new BusinessException("支付宝支付请求失败");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", order.getOrderNo());
            result.put("payForm", response.getBody());
            return result;
        } catch (AlipayApiException e) {
            throw new BusinessException("支付宝支付请求失败: " + e.getErrMsg());
        }
    }

    @Override
    public String handleNotify(Map<String, String> params) {
        validateConfig();

        try {
            boolean passed = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
            if (!passed) {
                throw new BusinessException("支付宝回调验签失败");
            }
        } catch (AlipayApiException e) {
            throw new BusinessException("支付宝回调验签失败: " + e.getErrMsg());
        }

        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException("支付回调缺少订单号");
        }

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo).last("limit 1"));
        if (order == null) {
            throw new BusinessException("支付订单不存在");
        }

        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, order.getId()).last("limit 1")
        );
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setAmount(order.getTotalAmount());
            payment.setPayMethod("ALIPAY");
            payment.setCreateTime(LocalDateTime.now());
        }

        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            order.setStatus(1);
            order.setPayTime(LocalDateTime.now());
            orderMapper.updateById(order);

            payment.setTradeNo(tradeNo);
            payment.setStatus(1);
            payment.setPayTime(LocalDateTime.now());
        } else {
            payment.setTradeNo(tradeNo);
            payment.setStatus(2);
        }

        if (payment.getId() == null) {
            paymentMapper.insert(payment);
        } else {
            paymentMapper.updateById(payment);
        }

        return "success";
    }

    private void upsertPendingPayment(Order order) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, order.getId()).last("limit 1")
        );

        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setAmount(order.getTotalAmount());
            payment.setStatus(0);
            payment.setPayMethod("ALIPAY");
            payment.setCreateTime(LocalDateTime.now());
            paymentMapper.insert(payment);
            return;
        }

        payment.setAmount(order.getTotalAmount());
        payment.setStatus(0);
        paymentMapper.updateById(payment);
    }

    private AlipayClient buildClient() {
        return new DefaultAlipayClient(
                alipayConfig.getGatewayUrl(),
                alipayConfig.getAppId(),
                alipayConfig.getPrivateKey(),
                "json",
                "UTF-8",
                alipayConfig.getAlipayPublicKey(),
                "RSA2"
        );
    }

    private void validateConfig() {
        if (!hasRealConfig(alipayConfig.getAppId())
                || !hasRealConfig(alipayConfig.getPrivateKey())
                || !hasRealConfig(alipayConfig.getAlipayPublicKey())
                || !hasRealConfig(alipayConfig.getGatewayUrl())
                || !hasRealConfig(alipayConfig.getNotifyUrl())
                || !hasRealConfig(alipayConfig.getReturnUrl())) {
            throw new BusinessException("支付宝配置未完成，请先在 application-local.yml 中填写真实沙箱参数");
        }
    }

    private boolean hasRealConfig(String value) {
        return StringUtils.hasText(value) && !value.startsWith("your_");
    }
}
