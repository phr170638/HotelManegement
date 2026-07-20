package com.hotel.module.user.service;

import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import com.hotel.module.user.service.impl.UserSignInServiceImpl;
import com.hotel.module.user.vo.SignInStatusVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSignInServiceImplTest {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserSignInServiceImpl userSignInService;

    private final Map<String, BitSet> bitmapStore = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.selectById(8L)).thenReturn(new User());
        lenient().when(stringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(valueOperations.getBit(anyString(), anyLong())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            long offset = invocation.getArgument(1);
            return bitmapStore.getOrDefault(key, new BitSet()).get((int) offset);
        });
        lenient().when(valueOperations.setBit(anyString(), anyLong(), any(Boolean.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            long offset = invocation.getArgument(1);
            boolean value = invocation.getArgument(2);
            bitmapStore.computeIfAbsent(key, ignored -> new BitSet()).set((int) offset, value);
            return true;
        });
    }

    @Test
    void signInShouldMarkTodayAsChecked() {
        SignInStatusVO statusVO = userSignInService.signIn(8L);

        assertTrue(Boolean.TRUE.equals(statusVO.getCheckedInToday()));
        assertTrue(Boolean.TRUE.equals(statusVO.getJustSignedIn()));
        assertEquals(1, statusVO.getCurrentMonthSignInDays());
        assertEquals(1, statusVO.getContinuousSignInDays());
        verify(stringRedisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void getSignInStatusShouldCountCurrentMonthAndContinuousDays() {
        LocalDate today = LocalDate.now();
        String key = "user:sign-in:8:" + YearMonth.from(today).format(MONTH_FORMATTER);
        BitSet bitSet = new BitSet();
        bitSet.set(today.getDayOfMonth() - 1);
        int expectedCurrentMonthSignInDays = 1;
        if (today.getDayOfMonth() > 1) {
            bitSet.set(today.getDayOfMonth() - 2);
            expectedCurrentMonthSignInDays++;
        }
        if (today.getDayOfMonth() > 2) {
            bitSet.set(0);
            expectedCurrentMonthSignInDays++;
        }
        bitmapStore.put(key, bitSet);

        SignInStatusVO statusVO = userSignInService.getSignInStatus(8L);

        assertTrue(Boolean.TRUE.equals(statusVO.getCheckedInToday()));
        assertEquals(expectedCurrentMonthSignInDays, statusVO.getCurrentMonthSignInDays());
        assertEquals(today.getDayOfMonth() > 1 ? 2 : 1, statusVO.getContinuousSignInDays());
    }
}
