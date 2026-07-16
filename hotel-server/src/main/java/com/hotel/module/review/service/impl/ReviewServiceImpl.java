package com.hotel.module.review.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.result.PageResult;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.review.dto.ReviewCreateRequest;
import com.hotel.module.review.entity.Review;
import com.hotel.module.review.mapper.ReviewMapper;
import com.hotel.module.review.service.ReviewService;
import com.hotel.module.review.vo.ReviewVO;
import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public void create(Long userId, ReviewCreateRequest req) {
        if (userId == null) {
            throw new BusinessException("登录后才可以发表评价");
        }
        if (req.getScore() == null || req.getScore() < 1 || req.getScore() > 5) {
            throw new BusinessException("评分必须在 1 到 5 分之间");
        }
        String content = req.getContent() == null ? null : req.getContent().trim();
        if (content != null && content.length() > 300) {
            throw new BusinessException("评价内容不能超过 300 字");
        }
        if (req.getOrderId() == null) {
            throw new BusinessException("评价必须关联已完成订单");
        }

        Order order = orderMapper.selectById(req.getOrderId());
        if (order == null) {
            throw new BusinessException("关联订单不存在");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException("无权评价该订单");
        }
        if (!Objects.equals(order.getHotelId(), req.getHotelId())) {
            throw new BusinessException("订单与酒店信息不匹配");
        }
        if (!Objects.equals(order.getStatus(), 5) && !Objects.equals(order.getStatus(), 6)) {
            throw new BusinessException("仅已退房或已完成订单可以评价");
        }

        Review existingReview = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, req.getOrderId())
                .last("limit 1"));
        if (existingReview != null) {
            throw new BusinessException("该订单已提交过评价");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setHotelId(req.getHotelId());
        review.setOrderId(req.getOrderId());
        review.setScore(req.getScore());
        review.setContent(content == null || content.isEmpty() ? null : content);
        review.setAnonymous(Boolean.TRUE.equals(req.getAnonymous()));
        review.setImages(req.getImages() != null ? String.join(",", req.getImages()) : null);
        review.setCreateTime(LocalDateTime.now());

        int inserted = reviewMapper.insert(review);
        if (inserted != 1) {
            throw new BusinessException("评价提交失败，请稍后重试");
        }
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
                if (!Boolean.TRUE.equals(r.getAnonymous())) {
                    ui.setNickname(user.getNickname());
                    ui.setAvatar(user.getAvatar());
                }
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
    @Transactional
    public void reply(Long reviewId, String reply) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) throw new BusinessException("评价不存在");
        review.setReply(reply);
        review.setReplyTime(LocalDateTime.now());
        reviewMapper.updateById(review);
    }
}
