package com.hotel.module.coupon.service.impl;

import com.hotel.common.exception.BusinessException;
import com.hotel.common.lock.CouponReceiveLockTimeoutStrategy;
import com.hotel.common.lock.DistributedLockFactory;
import com.hotel.common.lock.LockType;
import com.hotel.module.coupon.entity.Coupon;
import com.hotel.module.coupon.entity.UserCoupon;
import com.hotel.module.coupon.mapper.CouponMapper;
import com.hotel.module.coupon.mapper.UserCouponMapper;
import com.hotel.module.coupon.service.CouponService;
import com.hotel.module.coupon.util.CouponCodeUtils;
import com.hotel.module.coupon.vo.UserCouponVO;
import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final String COUPON_RECEIVE_LOCK_KEY = "lock:coupon:receive:";

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserMapper userMapper;
    private final DistributedLockFactory distributedLockFactory;

    @Override
    public UserCouponVO redeemCoupon(Long userId, String code) {
        ensureUserExists(userId);
        String normalizedCode = normalizeCode(code);
        if (!CouponCodeUtils.isValid(normalizedCode)) {
            throw new BusinessException("兑换码格式不正确");
        }
        Coupon coupon = couponMapper.selectByReceiveCode(normalizedCode);
        if (coupon == null) {
            throw new BusinessException("兑换码不存在");
        }
        String lockKey = COUPON_RECEIVE_LOCK_KEY + coupon.getId() + ":" + userId;
        CouponService proxy = (CouponService) AopContext.currentProxy();
        return distributedLockFactory.executeWithLock(
                LockType.REENTRANT,
                lockKey,
                CouponReceiveLockTimeoutStrategy.STRATEGY_NAME,
                () -> proxy.checkAndCreateUserCoupon(coupon.getId(), userId, normalizedCode)
        );
    }

    @Override
    public List<UserCouponVO> getMyCoupons(Long userId) {
        ensureUserExists(userId);
        return userCouponMapper.selectByUserId(userId).stream()
                .map(this::toUserCouponVO)
                .toList();
    }

    @Override
    @Transactional
    public UserCouponVO checkAndCreateUserCoupon(Long couponId, Long userId, String code) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (!Objects.equals(coupon.getReceiveCode(), code)) {
            throw new BusinessException("兑换码与优惠券不匹配");
        }

        LocalDateTime now = LocalDateTime.now();
        validateCouponReceivable(coupon, now);
        checkUserReceiveLimit(coupon, userId);
        reserveCouponStock(couponId);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setCouponName(coupon.getName());
        userCoupon.setReceiveCode(coupon.getReceiveCode());
        userCoupon.setDescription(coupon.getDescription());
        userCoupon.setDiscountAmount(coupon.getDiscountAmount());
        userCoupon.setThresholdAmount(coupon.getThresholdAmount());
        userCoupon.setStatus(0);
        userCoupon.setReceiveTime(now);
        userCoupon.setValidStartTime(coupon.getValidStartTime());
        userCoupon.setValidEndTime(coupon.getValidEndTime());
        userCouponMapper.insert(userCoupon);
        return toUserCouponVO(userCoupon);
    }

    private void checkUserReceiveLimit(Coupon coupon, Long userId) {
        int receivedCount = userCouponMapper.countByUserIdAndCouponId(userId, coupon.getId());
        if (receivedCount >= coupon.getPerUserLimit()) {
            throw new BusinessException("已超过该优惠券限领次数");
        }
    }

    private void reserveCouponStock(Long couponId) {
        int updatedRows = couponMapper.incrementIssueNumIfAvailable(couponId);
        if (updatedRows == 0) {
            throw new BusinessException("优惠券已领完");
        }
    }

    private void validateCouponReceivable(Coupon coupon, LocalDateTime now) {
        if (!Objects.equals(coupon.getStatus(), 1)) {
            throw new BusinessException("该优惠券当前不可领取");
        }
        if (coupon.getReceiveStartTime() != null && now.isBefore(coupon.getReceiveStartTime())) {
            throw new BusinessException("优惠券尚未开始领取");
        }
        if (coupon.getReceiveEndTime() != null && now.isAfter(coupon.getReceiveEndTime())) {
            throw new BusinessException("优惠券领取已结束");
        }
        if (coupon.getTotalNum() == null || coupon.getTotalNum() <= 0) {
            throw new BusinessException("优惠券库存异常");
        }
        if (coupon.getPerUserLimit() == null || coupon.getPerUserLimit() <= 0) {
            throw new BusinessException("优惠券限领配置异常");
        }
    }

    private void ensureUserExists(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("兑换码不能为空");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private UserCouponVO toUserCouponVO(UserCoupon userCoupon) {
        UserCouponVO couponVO = new UserCouponVO();
        couponVO.setId(userCoupon.getId());
        couponVO.setCouponId(userCoupon.getCouponId());
        couponVO.setCouponName(userCoupon.getCouponName());
        couponVO.setReceiveCode(userCoupon.getReceiveCode());
        couponVO.setDescription(userCoupon.getDescription());
        couponVO.setDiscountAmount(userCoupon.getDiscountAmount());
        couponVO.setThresholdAmount(userCoupon.getThresholdAmount());
        couponVO.setStatus(resolveCouponStatus(userCoupon));
        couponVO.setStatusText(resolveCouponStatusText(userCoupon));
        couponVO.setReceiveTime(userCoupon.getReceiveTime());
        couponVO.setValidStartTime(userCoupon.getValidStartTime());
        couponVO.setValidEndTime(userCoupon.getValidEndTime());
        return couponVO;
    }

    private int resolveCouponStatus(UserCoupon userCoupon) {
        if (Objects.equals(userCoupon.getStatus(), 1)) {
            return 1;
        }
        if (Objects.equals(userCoupon.getStatus(), 3)) {
            return 3;
        }
        if (userCoupon.getValidEndTime() != null && LocalDateTime.now().isAfter(userCoupon.getValidEndTime())) {
            return 2;
        }
        return 0;
    }

    private String resolveCouponStatusText(UserCoupon userCoupon) {
        return switch (resolveCouponStatus(userCoupon)) {
            case 1 -> "已使用";
            case 2 -> "已过期";
            case 3 -> "已锁定";
            default -> "未使用";
        };
    }
}
