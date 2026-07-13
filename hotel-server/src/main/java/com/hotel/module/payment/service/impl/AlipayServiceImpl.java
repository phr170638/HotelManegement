package com.hotel.module.payment.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.hotel.module.payment.config.AlipayConfig;
import com.hotel.module.payment.service.AlipayService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayConfig alipayConfig;

    private AlipayClient alipayClient;

    @PostConstruct
    public void init() {
        this.alipayClient = new DefaultAlipayClient(
                alipayConfig.getGatewayUrl(),
                alipayConfig.getAppId(),
                alipayConfig.getPrivateKey(),
                "json",
                "UTF-8",
                alipayConfig.getAlipayPublicKey(),
                "RSA2"
        );
    }

    @Override
    public String pagePay(String orderNo, String amount, String subject) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl());

        // 组装业务参数
        String bizContent = "{"
                + "\"out_trade_no\":\"" + orderNo + "\","
                + "\"total_amount\":\"" + amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\""
                + "}";
        request.setBizContent(bizContent);

        try {
            // pageExecute 返回完整的 HTML 表单
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("生成支付页面失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成支付页面失败", e);
        }
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );
        } catch (AlipayApiException e) {
            log.error("支付宝签名验证失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String extractOrderNo(Map<String, String> params) {
        return params.get("out_trade_no");
    }

    @Override
    public String extractTradeNo(Map<String, String> params) {
        return params.get("trade_no");
    }

    @Override
    public String extractAmount(Map<String, String> params) {
        return params.get("total_amount");
    }

    @Override
    public boolean isTradeSuccess(Map<String, String> params) {
        return "TRADE_SUCCESS".equals(params.get("trade_status"));
    }
}
