package com.hotel.module.user.service.impl;

import com.hotel.common.exception.BusinessException;
import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import com.hotel.module.user.service.UserSignInService;
import com.hotel.module.user.vo.SignInStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSignInServiceImpl implements UserSignInService {

    private static final String SIGN_IN_KEY_PREFIX = "user:sign-in:";
    private static final Duration SIGN_IN_BITMAP_TTL = Duration.ofDays(400);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter MONTH_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;

    @Override
    public SignInStatusVO signIn(Long userId) {
        ensureUserExists(userId);
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        String key = buildKey(userId, currentMonth);
        long todayOffset = today.getDayOfMonth() - 1L;
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        boolean checkedInToday = Boolean.TRUE.equals(valueOperations.getBit(key, todayOffset));
        if (!checkedInToday) {
            valueOperations.setBit(key, todayOffset, true);
            stringRedisTemplate.expire(key, SIGN_IN_BITMAP_TTL);
        }
        return buildStatus(userId, today, !checkedInToday);
    }

    @Override
    public SignInStatusVO getSignInStatus(Long userId) {
        ensureUserExists(userId);
        return buildStatus(userId, LocalDate.now(), false);
    }

    private SignInStatusVO buildStatus(Long userId, LocalDate today, boolean justSignedIn) {
        YearMonth currentMonth = YearMonth.from(today);
        String key = buildKey(userId, currentMonth);
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        List<Integer> signInDays = new ArrayList<>();
        int currentMonthSignInDays = 0;

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            if (Boolean.TRUE.equals(valueOperations.getBit(key, day - 1L))) {
                signInDays.add(day);
                currentMonthSignInDays++;
            }
        }

        int continuousSignInDays = 0;
        for (int day = today.getDayOfMonth(); day >= 1; day--) {
            if (!Boolean.TRUE.equals(valueOperations.getBit(key, day - 1L))) {
                break;
            }
            continuousSignInDays++;
        }

        SignInStatusVO statusVO = new SignInStatusVO();
        statusVO.setCheckedInToday(Boolean.TRUE.equals(valueOperations.getBit(key, today.getDayOfMonth() - 1L)));
        statusVO.setJustSignedIn(justSignedIn);
        statusVO.setCurrentMonthSignInDays(currentMonthSignInDays);
        statusVO.setContinuousSignInDays(continuousSignInDays);
        statusVO.setCurrentMonth(currentMonth.format(MONTH_DISPLAY_FORMATTER));
        statusVO.setSignInDays(signInDays);
        return statusVO;
    }

    private void ensureUserExists(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
    }

    private String buildKey(Long userId, YearMonth yearMonth) {
        return SIGN_IN_KEY_PREFIX + userId + ":" + yearMonth.format(MONTH_FORMATTER);
    }
}
