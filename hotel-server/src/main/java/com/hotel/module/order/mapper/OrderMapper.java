package com.hotel.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.module.order.entity.Order;
import com.hotel.module.user.vo.OrderListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    IPage<OrderListVO> selectMyOrders(Page<OrderListVO> page,
                                       @Param("userId") Long userId,
                                       @Param("status") Integer status);

    OrderListVO selectOrderById(@Param("id") Long id, @Param("userId") Long userId);
}
