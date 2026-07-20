package com.hotel.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.module.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    @Select("SELECT COUNT(1) FROM t_user_coupon WHERE user_id = #{userId} AND coupon_id = #{couponId}")
    Integer countByUserIdAndCouponId(Long userId, Long couponId);

    @Select("SELECT * FROM t_user_coupon WHERE user_id = #{userId} ORDER BY receive_time DESC, id DESC")
    List<UserCoupon> selectByUserId(Long userId);

    @Select("SELECT * FROM t_user_coupon WHERE id = #{id} AND user_id = #{userId} LIMIT 1")
    UserCoupon selectByIdAndUserId(Long id, Long userId);

    @Update("""
            UPDATE t_user_coupon
            SET status = 3
            WHERE id = #{id}
              AND user_id = #{userId}
              AND status = 0
            """)
    int lockForOrder(Long id, Long userId);

    @Update("""
            UPDATE t_user_coupon
            SET status = 0,
                use_time = NULL
            WHERE id = #{id}
              AND user_id = #{userId}
              AND status = 3
            """)
    int releaseForOrder(Long id, Long userId);

    @Update("""
            UPDATE t_user_coupon
            SET status = 1,
                use_time = #{useTime}
            WHERE id = #{id}
              AND user_id = #{userId}
              AND status IN (0, 3)
            """)
    int markUsed(Long id, Long userId, java.time.LocalDateTime useTime);
}
