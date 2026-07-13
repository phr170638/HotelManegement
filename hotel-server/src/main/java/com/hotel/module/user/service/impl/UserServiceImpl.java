package com.hotel.module.user.service.impl;

import cn.hutool.core.lang.UUID;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.result.PageResult;
import com.hotel.common.util.JwtUtil;
import com.hotel.module.user.dto.LoginRequest;
import com.hotel.module.user.dto.RegisterRequest;
import com.hotel.module.user.dto.UpdateUserRequest;
import com.hotel.module.user.entity.User;
import com.hotel.module.user.entity.UserRole;
import com.hotel.module.user.mapper.UserMapper;
import com.hotel.module.user.mapper.UserRoleMapper;
import com.hotel.module.user.service.UserService;
import com.hotel.module.user.vo.LoginVO;
import com.hotel.module.user.vo.OrderListVO;
import com.hotel.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest req) {
        // 验证码校验（TODO: 实际对接短信/邮箱服务）
        if (!"888888".equals(req.getCode())) {
            throw new BusinessException("验证码错误");
        }

        // 手机号/邮箱唯一性校验
        if (req.getPhone() != null && userMapper.selectByPhone(req.getPhone()) != null) {
            throw new BusinessException("手机号已注册");
        }
        if (req.getEmail() != null && userMapper.selectByEmail(req.getEmail()) != null) {
            throw new BusinessException("邮箱已注册");
        }

        User user = new User();
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname("用户" + UUID.randomUUID().toString(true).substring(0, 6));
        user.setStatus(1);
        userMapper.insert(user);

        // 分配普通用户角色
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(2L); // role_id=2 为 user 角色
        userRoleMapper.insert(userRole);
    }

    @Override
    public LoginVO login(LoginRequest req) {
        User user = null;
        if (req.getPhone() != null) {
            user = userMapper.selectByPhone(req.getPhone());
        } else if (req.getEmail() != null) {
            user = userMapper.selectByEmail(req.getEmail());
        }
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        List<String> roles = userMapper.selectRoleNamesByUserId(user.getId());
        List<String> permissions = userMapper.selectPermissionNamesByUserId(user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), roles, permissions);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setRoles(roles);
        return vo;
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(userMapper.selectRoleNamesByUserId(userId));
        vo.setPermissions(userMapper.selectPermissionNamesByUserId(userId));
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    public void updateProfile(Long userId, UpdateUserRequest req) {
        User user = userMapper.selectById(userId);
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        userMapper.updateById(user);
    }

    @Override
    public PageResult<OrderListVO> getMyOrders(Long userId, Integer page, Integer size, Integer status) {
        // TODO: 从订单表查询用户订单
        return new PageResult<>(List.of(), 0L, page, size);
    }

    @Override
    public void sendCode(String phone, String type) {
        // TODO: 对接短信/邮箱验证码服务，当前开发阶段固定返回888888
    }
}
