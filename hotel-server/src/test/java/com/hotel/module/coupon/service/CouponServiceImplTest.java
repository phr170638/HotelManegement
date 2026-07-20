package com.hotel.module.coupon.service;

import com.hotel.common.exception.BusinessException;
import com.hotel.common.lock.DistributedLockFactory;
import com.hotel.module.coupon.entity.Coupon;
import com.hotel.module.coupon.entity.UserCoupon;
import com.hotel.module.coupon.mapper.CouponMapper;
import com.hotel.module.coupon.mapper.UserCouponMapper;
import com.hotel.module.coupon.service.impl.CouponServiceImpl;
import com.hotel.module.coupon.vo.UserCouponVO;
import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DistributedLockFactory distributedLockFactory;

    @InjectMocks
    private CouponServiceImpl couponService;

    @Test
    void checkAndCreateUserCouponShouldCreateCouponForUser() {
        Coupon coupon = buildCoupon();
        when(couponMapper.selectById(1L)).thenReturn(coupon);
        when(userCouponMapper.countByUserIdAndCouponId(8L, 1L)).thenReturn(0);
        when(couponMapper.incrementIssueNumIfAvailable(1L)).thenReturn(1);

        UserCouponVO couponVO = couponService.checkAndCreateUserCoupon(1L, 8L, "Q0000001");

        assertEquals("新客立减券", couponVO.getCouponName());
        assertEquals(0, couponVO.getStatus());
        assertEquals("未使用", couponVO.getStatusText());

        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponMapper).insert(captor.capture());
        assertEquals(8L, captor.getValue().getUserId());
        assertEquals(1L, captor.getValue().getCouponId());
        assertNotNull(captor.getValue().getReceiveTime());
    }

    @Test
    void checkAndCreateUserCouponShouldRejectWhenLimitExceeded() {
        Coupon coupon = buildCoupon();
        when(couponMapper.selectById(1L)).thenReturn(coupon);
        when(userCouponMapper.countByUserIdAndCouponId(8L, 1L)).thenReturn(1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> couponService.checkAndCreateUserCoupon(1L, 8L, "Q0000001")
        );

        assertEquals("已超过该优惠券限领次数", exception.getMessage());
        verify(couponMapper, never()).incrementIssueNumIfAvailable(any());
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    @Test
    void getMyCouponsShouldReturnCouponList() {
        User user = new User();
        user.setId(8L);
        when(userMapper.selectById(8L)).thenReturn(user);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(1L);
        userCoupon.setCouponId(1L);
        userCoupon.setCouponName("新客立减券");
        userCoupon.setReceiveCode("Q0000001");
        userCoupon.setDescription("满 300 减 50");
        userCoupon.setDiscountAmount(new BigDecimal("50"));
        userCoupon.setThresholdAmount(new BigDecimal("300"));
        userCoupon.setStatus(0);
        userCoupon.setReceiveTime(LocalDateTime.now());
        userCoupon.setValidEndTime(LocalDateTime.now().plusDays(30));
        when(userCouponMapper.selectByUserId(8L)).thenReturn(java.util.List.of(userCoupon));

        java.util.List<UserCouponVO> couponList = couponService.getMyCoupons(8L);

        assertEquals(1, couponList.size());
        assertEquals("未使用", couponList.get(0).getStatusText());
    }

    private Coupon buildCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setName("新客立减券");
        coupon.setReceiveCode("Q0000001");
        coupon.setDescription("满 300 元可用，立减 50 元");
        coupon.setDiscountAmount(new BigDecimal("50"));
        coupon.setThresholdAmount(new BigDecimal("300"));
        coupon.setTotalNum(100);
        coupon.setIssueNum(0);
        coupon.setPerUserLimit(1);
        coupon.setStatus(1);
        coupon.setReceiveStartTime(LocalDateTime.now().minusDays(1));
        coupon.setReceiveEndTime(LocalDateTime.now().plusDays(10));
        coupon.setValidStartTime(LocalDateTime.now().minusDays(1));
        coupon.setValidEndTime(LocalDateTime.now().plusDays(30));
        return coupon;
    }
}
