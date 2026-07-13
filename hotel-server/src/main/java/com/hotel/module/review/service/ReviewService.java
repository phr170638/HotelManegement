package com.hotel.module.review.service;

import com.hotel.common.result.PageResult;
import com.hotel.module.review.dto.ReviewCreateRequest;
import com.hotel.module.review.vo.ReviewVO;

import java.util.Map;

public interface ReviewService {

    void create(Long userId, ReviewCreateRequest request);

    PageResult<ReviewVO> listByHotel(Long hotelId, Integer page, Integer size, Integer score);

    Map<String, Object> getHotelScoreStats(Long hotelId);

    void reply(Long reviewId, String reply);
}
