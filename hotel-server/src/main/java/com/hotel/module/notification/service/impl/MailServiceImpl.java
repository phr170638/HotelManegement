package com.hotel.module.notification.service.impl;

import com.hotel.common.exception.BusinessException;
import com.hotel.module.notification.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final Map<String, String> BIZ_TYPE_LABELS = Map.of(
            "register", "注册"
    );

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.notification.mail.from-name:酒店管理系统}")
    private String fromName;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:0}")
    private Integer mailPort;

    @Override
    public void sendVerificationCodeMail(String email, String bizType, String code, long expireMinutes) {
        String sceneLabel = BIZ_TYPE_LABELS.getOrDefault(bizType, "业务");
        String subject = "【酒店管理系统】" + sceneLabel + "验证码";
        String content = buildMailContent(sceneLabel, code, expireMinutes);
        sendPlainTextMail(email, subject, content, "验证码邮件发送失败，请稍后再试");
    }

    @Override
    public void sendPaymentSuccessMail(String email, String orderNo) {
        String subject = "【酒店管理系统】订单支付成功通知";
        String content = "您好，\n\n"
                + "您的订单已支付成功，订单号：" + orderNo + "。\n"
                + "后续请前往个人中心或我的订单查看入住安排。\n\n"
                + "感谢您的使用。";
        sendPlainTextMail(email, subject, content, "支付成功通知发送失败，请稍后再试");
    }

    private void sendPlainTextMail(String email, String subject, String content, String errorMessage) {
        if (!StringUtils.hasText(fromAddress) || !StringUtils.hasText(mailHost) || mailPort == null || mailPort <= 0) {
            log.warn("Mail config missing, skip send mail: fromAddress={}, host={}, port={}, target={}, subject={}",
                    fromAddress, mailHost, mailPort, email, subject);
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        try {
            log.info("Mail send attempt: fromAddress={}, fromName={}, host={}, port={}, target={}",
                    fromAddress, fromName, mailHost, mailPort, email);
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()).toString());
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, false);
            mailSender.send(message);
            log.info("Mail send success: target={}, subject={}", email, subject);
        } catch (MessagingException | UnsupportedEncodingException | MailException exception) {
            log.error("Mail send failed: fromAddress={}, fromName={}, host={}, port={}, target={}, exceptionType={}, message={}",
                    fromAddress, fromName, mailHost, mailPort, email,
                    exception.getClass().getName(), exception.getMessage(), exception);
            throw new BusinessException(errorMessage);
        }
    }

    private String buildMailContent(String sceneLabel, String code, long expireMinutes) {
        return "您好，\n\n"
                + "您正在进行酒店管理系统" + sceneLabel + "操作，本次验证码为：" + code + "。\n"
                + "验证码 " + expireMinutes + " 分钟内有效，请勿泄露给他人。\n\n"
                + "如果这不是您的操作，请忽略此邮件。";
    }
}
