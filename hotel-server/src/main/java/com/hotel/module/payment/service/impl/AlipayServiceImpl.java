package com.hotel.module.payment.service.impl;

import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.common.exception.BusinessException;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.payment.config.AlipayConfig;
import com.hotel.module.payment.entity.Payment;
import com.hotel.module.payment.mapper.PaymentMapper;
import com.hotel.module.payment.mq.PaymentEventPublisher;
import com.hotel.module.payment.mq.PaymentSucceededMessage;
import com.hotel.module.payment.service.AlipayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayConfig alipayConfig;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final PaymentEventPublisher paymentEventPublisher;
    private final Environment environment;

    @Override
    public Map<String, Object> createPagePay(Long orderId) {
        validatePagePayConfig();

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

            AlipayTradePagePayResponse response = buildClient().pageExecute(request);
            if (!response.isSuccess() || !StringUtils.hasText(response.getBody())) {
                throw new BusinessException("支付宝支付请求失败");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", order.getOrderNo());
            result.put("payForm", response.getBody());
            return result;
        } catch (AlipayApiException e) {
            log.error("alipay page pay failed: message={}, errMsg={}, cause={}",
                    e.getMessage(), e.getErrMsg(), e.getCause(), e);
            throw new BusinessException("支付宝支付请求失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String handleNotify(Map<String, String> params) {
        validateClientConfig();
        // #region debug-point payment-result-stuck-notify
        log.info("payment notify received: outTradeNo={}, tradeNo={}, tradeStatus={}, totalAmount={}",
                params.get("out_trade_no"), params.get("trade_no"), params.get("trade_status"), params.get("total_amount"));
        // #endregion

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
            log.error("alipay notify verify failed: message={}, errMsg={}, cause={}",
                    e.getMessage(), e.getErrMsg(), e.getCause(), e);
            throw new BusinessException("支付宝回调验签失败: " + e.getMessage());
        }

        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmountText = params.get("total_amount");
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

        applyTradeResult(order, payment, tradeNo, tradeStatus, totalAmountText, LocalDateTime.now());

        return "success";
    }

    @Override
    @Transactional
    public boolean syncPaymentStatus(Long orderId) {
        validateClientConfig();

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (Objects.equals(order.getStatus(), 1)) {
            return true;
        }

        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, order.getId()).last("limit 1")
        );

        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent(JSONUtil.toJsonStr(Map.of("out_trade_no", order.getOrderNo())));
            AlipayTradeQueryResponse response = buildClient().execute(request);
            if (response == null || !response.isSuccess()) {
                log.info("payment sync query not successful: orderId={}, orderNo={}, code={}, subCode={}, msg={}",
                        order.getId(), order.getOrderNo(),
                        response == null ? null : response.getCode(),
                        response == null ? null : response.getSubCode(),
                        response == null ? null : response.getMsg());
                return false;
            }
            if (!isTradeSuccess(response.getTradeStatus())) {
                return false;
            }
            if (payment == null) {
                payment = new Payment();
                payment.setOrderId(order.getId());
                payment.setAmount(order.getTotalAmount());
                payment.setPayMethod("ALIPAY");
                payment.setCreateTime(LocalDateTime.now());
            }
            applyTradeResult(
                    order,
                    payment,
                    response.getTradeNo(),
                    response.getTradeStatus(),
                    response.getTotalAmount(),
                    parseAlipayTime(response.getSendPayDate())
            );
            return true;
        } catch (AlipayApiException e) {
            log.error("alipay query failed: message={}, errMsg={}, cause={}",
                    e.getMessage(), e.getErrMsg(), e.getCause(), e);
            throw new BusinessException("支付宝支付结果查询失败: " + e.getMessage());
        }
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

    private void applyTradeResult(Order order,
                                  Payment payment,
                                  String tradeNo,
                                  String tradeStatus,
                                  String totalAmountText,
                                  LocalDateTime payTime) {
        LocalDateTime finalPayTime = payTime == null ? LocalDateTime.now() : payTime;
        if (isTradeSuccess(tradeStatus)) {
            validateAmount(totalAmountText, order);
            payment.setTradeNo(tradeNo);
            payment.setStatus(1);
            payment.setPayTime(finalPayTime);
        } else {
            payment.setTradeNo(tradeNo);
            payment.setStatus(2);
        }

        if (payment.getId() == null) {
            paymentMapper.insert(payment);
        } else {
            paymentMapper.updateById(payment);
        }

        if (isTradeSuccess(tradeStatus) && !Objects.equals(order.getStatus(), 1)) {
            paymentEventPublisher.publishPaymentSucceeded(new PaymentSucceededMessage(
                    order.getId(),
                    order.getOrderNo(),
                    order.getUserId(),
                    tradeNo,
                    order.getTotalAmount(),
                    "ALIPAY",
                    finalPayTime
            ));
            // #region debug-point payment-result-stuck-notify-success
            log.info("payment notify success prepared publish: orderId={}, orderNo={}, paymentStatus={}, orderStatus={}",
                    order.getId(), order.getOrderNo(), payment.getStatus(), order.getStatus());
            // #endregion
        }
    }

    private void validateClientConfig() {
        log.info("alipay config check: appIdPresent={}, privateKeyPresent={}, alipayPublicKeyPresent={}, gatewayUrl={}",
                hasRealConfig(alipayConfig.getAppId()),
                hasRealConfig(alipayConfig.getPrivateKey()),
                hasRealConfig(alipayConfig.getAlipayPublicKey()),
                alipayConfig.getGatewayUrl());
        log.info("alipay env check: appIdPresent={}, privateKeyPresent={}, alipayPublicKeyPresent={}, gatewayUrl={}",
                hasRealConfig(environment.getProperty("alipay.app-id")),
                hasRealConfig(environment.getProperty("alipay.private-key")),
                hasRealConfig(environment.getProperty("alipay.alipay-public-key")),
                environment.getProperty("alipay.gateway-url"));
        if (!hasRealConfig(alipayConfig.getAppId())
                || !hasRealConfig(alipayConfig.getPrivateKey())
                || !hasRealConfig(alipayConfig.getAlipayPublicKey())
                || !hasRealConfig(alipayConfig.getGatewayUrl())) {
            throw new BusinessException("支付宝配置未完成，请先在 application-local.yml 中填写真实沙箱参数");
        }
    }

    private void validatePagePayConfig() {
        validateClientConfig();
        if (!hasRealConfig(alipayConfig.getNotifyUrl())
                || !hasRealConfig(alipayConfig.getReturnUrl())) {
            throw new BusinessException("支付宝回调地址未完成配置，请先补全支付回跳与异步通知地址");
        }
    }

    private boolean hasRealConfig(String value) {
        return StringUtils.hasText(value) && !value.startsWith("your_");
    }

    private boolean isTradeSuccess(String tradeStatus) {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    private void validateAmount(String totalAmountText, Order order) {
        if (!StringUtils.hasText(totalAmountText)) {
            throw new BusinessException("支付回调缺少支付金额");
        }
        BigDecimal notifyAmount = new BigDecimal(totalAmountText.trim());
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(notifyAmount) != 0) {
            throw new BusinessException(String.format(Locale.ROOT, "支付金额不匹配，订单金额=%s，回调金额=%s", order.getTotalAmount(), notifyAmount));
        }
    }

    private LocalDateTime parseAlipayTime(Date payTime) {
        if (payTime == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(payTime.toInstant(), ZoneId.systemDefault());
    }
}
