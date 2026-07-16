package com.hotel.module.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.module.review.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    @Select("SELECT COUNT(*) FROM t_review WHERE hotel_id = #{hotelId}")
    Integer countByHotelId(@Param("hotelId") Long hotelId);

    @Select("SELECT ROUND(AVG(score), 1) FROM t_review WHERE hotel_id = #{hotelId}")
    BigDecimal avgScoreByHotelId(@Param("hotelId") Long hotelId);
}
