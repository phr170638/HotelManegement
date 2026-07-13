package com.hotel.module.review.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.result.PageResult;
import com.hotel.module.review.dto.ReviewCreateRequest;
import com.hotel.module.review.entity.Review;
import com.hotel.module.review.mapper.ReviewMapper;
import com.hotel.module.review.service.ReviewService;
import com.hotel.module.review.vo.ReviewVO;
import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;

    @Override
    public void create(Long userId, ReviewCreateRequest req) {
        Review review = new Review();
        review.setUserId(userId);
        review.setHotelId(req.getHotelId());
        review.setOrderId(req.getOrderId());
        review.setScore(req.getScore());
        review.setContent(req.getContent());
        review.setImages(req.getImages() != null ? String.join(",", req.getImages()) : null);
        reviewMapper.insert(review);
    }

    @Override
    public PageResult<ReviewVO> listByHotel(Long hotelId, Integer page, Integer size, Integer score) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getHotelId, hotelId);
        if (score != null) wrapper.eq(Review::getScore, score);
        wrapper.orderByDesc(Review::getCreateTime);

        Page<Review> result = reviewMapper.selectPage(new Page<>(page, size), wrapper);
        List<ReviewVO> records = result.getRecords().stream().map(r -> {
            ReviewVO vo = new ReviewVO();
            BeanUtil.copyProperties(r, vo);
            if (r.getImages() != null) vo.setImages(Arrays.asList(r.getImages().split(",")));

            User user = userMapper.selectById(r.getUserId());
            if (user != null) {
                ReviewVO.UserInfo ui = new ReviewVO.UserInfo();
                ui.setId(user.getId());
                ui.setNickname(user.getNickname());
                ui.setAvatar(user.getAvatar());
                vo.setUser(ui);
            }
            return vo;
        }).toList();

        return new PageResult<>(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public Map<String, Object> getHotelScoreStats(Long hotelId) {
        // TODO: 计算评分分布统计
        Map<String, Object> stats = new HashMap<>();
        stats.put("avgScore", 0);
        stats.put("scoreDistribution", Map.of());
        return stats;
    }

    @Override
    public void reply(Long reviewId, String reply) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) throw new BusinessException("评价不存在");
        review.setReply(reply);
        review.setReplyTime(LocalDateTime.now());
        reviewMapper.updateById(review);
    }
}
