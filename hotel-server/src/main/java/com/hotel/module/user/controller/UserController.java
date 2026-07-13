package com.hotel.module.user.controller;

import com.hotel.common.result.PageResult;
import com.hotel.common.result.R;
import com.hotel.module.user.dto.LoginRequest;
import com.hotel.module.user.dto.RegisterRequest;
import com.hotel.module.user.dto.UpdateUserRequest;
import com.hotel.module.user.service.UserService;
import com.hotel.module.user.vo.LoginVO;
import com.hotel.module.user.vo.OrderListVO;
import com.hotel.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户模块", description = "注册、登录、个人信息")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return R.ok();
    }

    @Operation(summary = "发送验证码")
    @PostMapping("/send-code")
    public R<Void> sendCode(@RequestParam String email,
                             @RequestParam(defaultValue = "register") String type) {
        userService.sendCode(email, type);
        return R.okMsg("验证码已发送");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(userService.login(request));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public R<UserVO> info(@AuthenticationPrincipal Long userId) {
        return R.ok(userService.getCurrentUser(userId));
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/update")
    public R<Void> update(@AuthenticationPrincipal Long userId,
                           @Valid @RequestBody UpdateUserRequest request) {
        userService.updateProfile(userId, request);
        return R.ok();
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/orders")
    public R<PageResult<OrderListVO>> orders(@AuthenticationPrincipal Long userId,
                                              @RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              @RequestParam(required = false) Integer status) {
        return R.ok(userService.getMyOrders(userId, page, size, status));
    }

    @Operation(summary = "根据id获取订单详情")
    @GetMapping("/orders/{id}")
    public R<OrderListVO> order(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return R.ok(userService.getOrderById(userId, id));
    }
}
