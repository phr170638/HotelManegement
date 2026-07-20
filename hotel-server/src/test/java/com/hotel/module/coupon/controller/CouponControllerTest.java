package com.hotel.module.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.coupon.dto.CouponRedeemRequest;
import com.hotel.module.coupon.service.CouponService;
import com.hotel.module.coupon.vo.UserCouponVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private CouponService couponService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CouponController(couponService)).build();
    }

    @Test
    void redeemCouponShouldReturnOk() throws Exception {
        CouponRedeemRequest request = new CouponRedeemRequest();
        request.setCode("Q0000001");

        UserCouponVO couponVO = new UserCouponVO();
        couponVO.setCouponName("新客立减券");
        couponVO.setDiscountAmount(new BigDecimal("50"));
        couponVO.setStatusText("未使用");
        when(couponService.redeemCoupon(any(), any())).thenReturn(couponVO);

        mockMvc.perform(post("/api/coupon/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("领取成功"))
                .andExpect(jsonPath("$.data.couponName").value("新客立减券"));

        verify(couponService).redeemCoupon(null, "Q0000001");
    }

    @Test
    void myCouponsShouldReturnOk() throws Exception {
        UserCouponVO couponVO = new UserCouponVO();
        couponVO.setCouponName("新客立减券");
        couponVO.setStatusText("未使用");
        when(couponService.getMyCoupons(any())).thenReturn(List.of(couponVO));

        mockMvc.perform(get("/api/coupon/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].couponName").value("新客立减券"));

        verify(couponService).getMyCoupons(null);
    }
}
