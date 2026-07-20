package com.hotel.module.user.controller;

import com.hotel.common.result.PageResult;
import com.hotel.common.result.R;
import com.hotel.module.user.dto.LoginRequest;
import com.hotel.module.user.dto.RegisterRequest;
import com.hotel.module.user.dto.UpdateUserRequest;
import com.hotel.module.user.service.UserSignInService;
import com.hotel.module.user.service.UserService;
import com.hotel.module.user.vo.LoginVO;
import com.hotel.module.user.vo.OrderListVO;
import com.hotel.module.user.vo.SendCodeVO;
import com.hotel.module.user.vo.SignInStatusVO;
import com.hotel.module.user.vo.UserVO;
import com.hotel.module.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户模块", description = "注册、登录、个人信息")
@Validated
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSignInService userSignInService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return R.ok();
    }

    @Operation(summary = "发送验证码")
    @PostMapping("/send-code")
    public R<SendCodeVO> sendCode(@RequestParam @Email(message = "邮箱格式不正确") String email,
                                  @RequestParam(required = false) String type) {
        return R.ok("验证码已发送", userService.sendCode(email, type));
    }

    @Operation(summary = "用户登录")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(userService.login(request));
    }

    @Operation(summary = "用户登录（表单）")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public R<LoginVO> loginForm(@Valid LoginRequest request) {
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

    @Operation(summary = "上传用户头像")
    @PostMapping("/avatar")
    public R<String> uploadAvatar(@AuthenticationPrincipal Long userId,
                                  @RequestPart("file") MultipartFile file) {
        return R.ok(userService.uploadAvatar(userId, file));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping("/orders")
    public R<PageResult<OrderListVO>> orders(@AuthenticationPrincipal Long userId,
                                              @RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              @RequestParam(required = false) Integer status) {
        return R.ok(userService.getMyOrders(userId, page, size, status));
    }

    @Operation(summary = "我的订单详情")
    @GetMapping("/orders/{id}")
    public R<OrderVO> orderDetail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return R.ok(userService.getMyOrderDetail(userId, id));
    }

    @Operation(summary = "用户签到")
    @PostMapping("/sign-in")
    public R<SignInStatusVO> signIn(@AuthenticationPrincipal Long userId) {
        SignInStatusVO statusVO = userSignInService.signIn(userId);
        String message = Boolean.TRUE.equals(statusVO.getJustSignedIn()) ? "签到成功" : "今日已签到";
        return R.ok(message, statusVO);
    }

    @Operation(summary = "获取签到状态")
    @GetMapping("/sign-in/status")
    public R<SignInStatusVO> signInStatus(@AuthenticationPrincipal Long userId) {
        return R.ok(userSignInService.getSignInStatus(userId));
    }
}
