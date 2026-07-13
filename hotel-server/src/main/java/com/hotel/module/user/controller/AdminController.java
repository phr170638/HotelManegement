package com.hotel.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.result.PageResult;
import com.hotel.common.result.R;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端", description = "管理员接口 — 订单、用户管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public R<PageResult<Order>> orders(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "20") Integer size,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String orderNo,
                                        @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Order::getStatus, status);
        if (orderNo != null) wrapper.like(Order::getOrderNo, orderNo);
        if (keyword != null) wrapper.like(Order::getGuestName, keyword);
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "退票处理")
    @PutMapping("/orders/{id}/refund")
    public R<Void> refund(@PathVariable Long id, @RequestParam String reason) {
        // TODO: 实现退票退款逻辑
        Order order = orderMapper.selectById(id);
        if (order != null) {
            order.setStatus(5); // 已退房
            orderMapper.updateById(order);
        }
        return R.okMsg("退票处理成功");
    }

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public R<PageResult<User>> users(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "20") Integer size) {
        Page<User> result = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        return R.ok(PageResult.of(result));
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/users/{id}/status")
    public R<Void> toggleUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setStatus(status);
            userMapper.updateById(user);
        }
        return R.ok();
    }
}
