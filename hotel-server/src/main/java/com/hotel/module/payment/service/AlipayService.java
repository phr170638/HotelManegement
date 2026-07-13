package com.hotel.module.payment.service;

import java.util.Map;

public interface AlipayService {

    /**
     * 生成支付宝网页支付表单（自动提交跳转到支付宝收银台）
     *
     * @param orderNo  订单号
     * @param amount   支付金额
     * @param subject  商品标题
     * @return HTML 表单字符串
     */
    String pagePay(String orderNo, String amount, String subject);

    /**
     * 验证支付宝异步通知的签名
     */
    boolean verifySignature(Map<String, String> params);

    /**
     * 从异步通知参数中提取订单号
     */
    String extractOrderNo(Map<String, String> params);

    /**
     * 从异步通知参数中提取支付宝交易号
     */
    String extractTradeNo(Map<String, String> params);

    /**
     * 从异步通知参数中提取实付金额
     */
    String extractAmount(Map<String, String> params);

    /**
     * 判断通知是否为交易成功
     */
    boolean isTradeSuccess(Map<String, String> params);
}
