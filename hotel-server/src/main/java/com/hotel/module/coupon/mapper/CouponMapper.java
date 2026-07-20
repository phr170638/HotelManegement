package com.hotel.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.module.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    @Select("SELECT * FROM t_coupon WHERE receive_code = #{receiveCode} LIMIT 1")
    Coupon selectByReceiveCode(String receiveCode);

    @Update("UPDATE t_coupon SET issue_num = issue_num + 1 WHERE id = #{id} AND issue_num < total_num")
    int incrementIssueNumIfAvailable(Long id);

    @Select("SELECT * FROM t_coupon WHERE receive_code IS NULL OR receive_code = '' ORDER BY id ASC")
    List<Coupon> selectPendingReceiveCodeCoupons();

    @Update("UPDATE t_coupon SET receive_code = #{receiveCode} WHERE id = #{id} AND (receive_code IS NULL OR receive_code = '')")
    int updateReceiveCodeIfBlank(Long id, String receiveCode);
}
