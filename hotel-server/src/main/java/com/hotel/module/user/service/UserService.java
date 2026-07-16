package com.hotel.module.user.service;

import com.hotel.common.result.PageResult;
import com.hotel.module.user.dto.LoginRequest;
import com.hotel.module.user.dto.RegisterRequest;
import com.hotel.module.user.dto.UpdateUserRequest;
import com.hotel.module.user.vo.LoginVO;
import com.hotel.module.user.vo.OrderListVO;
import com.hotel.module.user.vo.SendCodeVO;
import com.hotel.module.user.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;
import com.hotel.module.order.vo.OrderVO;

public interface UserService {

    void register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    UserVO getCurrentUser(Long userId);

    void updateProfile(Long userId, UpdateUserRequest request);

    String uploadAvatar(Long userId, MultipartFile file);

    PageResult<OrderListVO> getMyOrders(Long userId, Integer page, Integer size, Integer status);

    OrderVO getMyOrderDetail(Long userId, Long orderId);

    SendCodeVO sendCode(String phone, String type);
}
