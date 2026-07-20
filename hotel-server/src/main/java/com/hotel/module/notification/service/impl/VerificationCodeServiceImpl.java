package com.hotel.module.notification.service.impl;

import com.hotel.common.exception.BusinessException;
import com.hotel.module.notification.service.MailService;
import com.hotel.module.notification.service.VerificationCodeService;
import com.hotel.module.user.vo.SendCodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Duration CODE_EXPIRE_DURATION = Duration.ofMinutes(5);
    private static final Duration CODE_RESEND_DURATION = Duration.ofMinutes(1);
    private static final String CODE_KEY_PREFIX = "verify:code:";
    private static final String RESEND_KEY_PREFIX = "verify:resend:";

    private final StringRedisTemplate stringRedisTemplate;
    private final MailService mailService;

    @Value("${app.verify-code.expose-code:false}")
    private boolean exposeVerifyCode;

    @Override
    public SendCodeVO sendEmailCode(String email, String type) {
        String normalizedEmail = normalizeEmail(email);
        String bizType = normalizeBizType(type);
        String resendKey = buildResendKey(normalizedEmail, bizType);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(resendKey))) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String verifyCode = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        mailService.sendVerificationCodeMail(normalizedEmail, bizType, verifyCode, CODE_EXPIRE_DURATION.toMinutes());

        stringRedisTemplate.opsForValue().set(
                buildCodeKey(normalizedEmail, bizType),
                verifyCode,
                CODE_EXPIRE_DURATION
        );
        stringRedisTemplate.opsForValue().set(resendKey, "1", CODE_RESEND_DURATION);

        return new SendCodeVO(
                Math.toIntExact(CODE_EXPIRE_DURATION.toSeconds()),
                Math.toIntExact(CODE_RESEND_DURATION.toSeconds()),
                exposeVerifyCode ? verifyCode : null
        );
    }

    @Override
    public void validateEmailCode(String email, String type, String code) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = normalizeRequired(code, "验证码不能为空");
        String cacheKey = buildCodeKey(normalizedEmail, normalizeBizType(type));
        String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null) {
            throw new BusinessException("验证码已失效，请重新获取");
        }
        if (!cachedCode.equals(normalizedCode)) {
            throw new BusinessException("验证码错误");
        }
        stringRedisTemplate.delete(cacheKey);
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = normalizeRequired(email, "邮箱不能为空").toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        return normalizedEmail;
    }

    private String normalizeBizType(String type) {
        String normalizedType = normalizeText(type);
        return normalizedType == null ? "register" : normalizedType.toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String message) {
        String normalizedValue = normalizeText(value);
        if (normalizedValue == null) {
            throw new BusinessException(message);
        }
        return normalizedValue;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String buildCodeKey(String email, String type) {
        return CODE_KEY_PREFIX + type + ":" + email;
    }

    private String buildResendKey(String email, String type) {
        return RESEND_KEY_PREFIX + type + ":" + email;
    }
}
