package com.hotel.module.review.service;

import com.hotel.common.exception.BusinessException;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.review.dto.ReviewCreateRequest;
import com.hotel.module.review.entity.Review;
import com.hotel.module.review.mapper.ReviewMapper;
import com.hotel.module.review.service.impl.ReviewServiceImpl;
import com.hotel.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void createShouldInsertReviewWithCreateTime() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setHotelId(1L);
        request.setOrderId(3L);
        request.setScore(5);
        request.setContent("great stay overall");

        Order order = new Order();
        order.setId(3L);
        order.setUserId(2L);
        order.setHotelId(1L);
        order.setStatus(5);
        when(orderMapper.selectById(3L)).thenReturn(order);
        when(reviewMapper.selectOne(any())).thenReturn(null);
        when(reviewMapper.insert(any(Review.class))).thenReturn(1);

        reviewService.create(2L, request);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        Review inserted = reviewCaptor.getValue();
        assertEquals(2L, inserted.getUserId());
        assertEquals(1L, inserted.getHotelId());
        assertEquals(3L, inserted.getOrderId());
        assertEquals(5, inserted.getScore());
        assertEquals("great stay overall", inserted.getContent());
        assertNotNull(inserted.getCreateTime());
    }

    @Test
    void createShouldRejectWhenInsertDoesNotAffectRow() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setHotelId(1L);
        request.setOrderId(3L);
        request.setScore(5);
        request.setContent("great stay overall");

        Order order = new Order();
        order.setId(3L);
        order.setUserId(2L);
        order.setHotelId(1L);
        order.setStatus(5);
        when(orderMapper.selectById(3L)).thenReturn(order);
        when(reviewMapper.selectOne(any())).thenReturn(null);
        when(reviewMapper.insert(any(Review.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> reviewService.create(2L, request));

        assertEquals("评价提交失败，请稍后重试", exception.getMessage());
    }

    @Test
    void createShouldRejectDuplicateReview() {
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setHotelId(1L);
        request.setOrderId(3L);
        request.setScore(5);
        request.setContent("great stay overall");

        Order order = new Order();
        order.setId(3L);
        order.setUserId(2L);
        order.setHotelId(1L);
        order.setStatus(5);
        when(orderMapper.selectById(3L)).thenReturn(order);
        when(reviewMapper.selectOne(any())).thenReturn(new Review());

        BusinessException exception = assertThrows(BusinessException.class, () -> reviewService.create(2L, request));

        assertEquals("该订单已提交过评价", exception.getMessage());
        verify(reviewMapper, never()).insert(any(Review.class));
    }
}
