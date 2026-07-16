package com.hotel.module.user.service.impl;

import cn.hutool.core.lang.UUID;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.result.PageResult;
import com.hotel.common.util.JwtUtil;
import com.hotel.module.order.service.OrderService;
import com.hotel.module.order.vo.OrderVO;
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
import com.hotel.module.user.vo.SendCodeVO;
import com.hotel.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final long CODE_EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final long CODE_RESEND_INTERVAL_MILLIS = 60 * 1000L;
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Map<String, VerifyCodeEntry> VERIFY_CODE_CACHE = new ConcurrentHashMap<>();

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OrderService orderService;
    @Value("${app.verify-code.expose-code:false}")
    private boolean exposeVerifyCode;

    @Override
    public void register(RegisterRequest req) {
        String phone = normalizeText(req.getPhone());
        String email = normalizeText(req.getEmail());
        String password = normalizeText(req.getPassword());
        String code = normalizeText(req.getCode());

        validatePhoneNumber(phone);
        validateRequired(password, "密码不能为空");
        validateVerifyCode(phone, "register", code);

        // 手机号/邮箱唯一性校验
        if (userMapper.selectByPhone(phone) != null) {
            throw new BusinessException("手机号已注册");
        }
        if (email != null && userMapper.selectByEmail(email) != null) {
            throw new BusinessException("邮箱已注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname("用户" + UUID.randomUUID().toString(true).substring(0, 6));
        user.setStatus(1);
        userMapper.insert(user);

        // 分配普通用户角色
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(2L); // role_id=2 为 user 角色
        userRoleMapper.insert(userRole);
    }

    private void validateVerifyCode(String phone, String type, String code) {
        validatePhoneNumber(phone);
        if (code == null || code.isBlank()) {
            throw new BusinessException("验证码不能为空");
        }

        String cacheKey = buildVerifyCodeKey(phone, type);
        VerifyCodeEntry entry = VERIFY_CODE_CACHE.get(cacheKey);
        if (entry == null || entry.expireAt() < System.currentTimeMillis()) {
            VERIFY_CODE_CACHE.remove(cacheKey);
            throw new BusinessException("验证码已失效，请重新获取");
        }
        if (!entry.code().equals(code)) {
            throw new BusinessException("验证码错误");
        }
        VERIFY_CODE_CACHE.remove(cacheKey);
    }

    @Override
    public LoginVO login(LoginRequest req) {
        String phone = normalizeText(req.getPhone());
        String email = normalizeText(req.getEmail());
        String password = normalizeText(req.getPassword());
        validateRequired(password, "密码不能为空");
        User user = null;
        if (phone != null) {
            user = userMapper.selectByPhone(phone);
        } else if (email != null) {
            user = userMapper.selectByEmail(email);
        }
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
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
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("头像图片不能超过 2MB");
        }

        String contentType = normalizeText(file.getContentType());
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String extension = extractExtension(file.getOriginalFilename());
        Path avatarDirectory = Paths.get(System.getProperty("user.dir"), "uploads", "avatars")
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(avatarDirectory);
            String fileName = "avatar-" + userId + "-" + UUID.fastUUID().toString(true) + extension;
            Path target = avatarDirectory.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            deleteOldLocalAvatar(user.getAvatar(), avatarDirectory);

            String avatarUrl = "/uploads/avatars/" + fileName;
            user.setAvatar(avatarUrl);
            userMapper.updateById(user);
            return avatarUrl;
        } catch (IOException exception) {
            throw new BusinessException("头像上传失败，请稍后重试");
        }
    }

    @Override
    public PageResult<OrderListVO> getMyOrders(Long userId, Integer page, Integer size, Integer status) {
        PageResult<OrderVO> orderPage = orderService.listByUser(userId, page, size, status);
        List<OrderListVO> records = orderPage.getRecords().stream().map(order -> {
            OrderListVO vo = new OrderListVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setHotelId(order.getHotelId());
            vo.setHotelName(order.getHotelName());
            vo.setHotelImage(null);
            vo.setCheckInDate(order.getCheckInDate());
            vo.setCheckOutDate(order.getCheckOutDate());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setStatus(order.getStatus());
            vo.setStatusText(order.getStatusText());
            vo.setCreateTime(order.getCreateTime());
            return vo;
        }).toList();
        return new PageResult<>(records, orderPage.getTotal(), orderPage.getPage(), orderPage.getSize());
    }

    @Override
    public OrderVO getMyOrderDetail(Long userId, Long orderId) {
        return orderService.detailByUser(userId, orderId);
    }

    @Override
    public SendCodeVO sendCode(String phone, String type) {
        String normalizedPhone = normalizeText(phone);
        validatePhoneNumber(normalizedPhone);
        clearExpiredVerifyCodes();

        String normalizedType = normalizeText(type);
        String bizType = normalizedType == null ? "register" : normalizedType;
        String cacheKey = buildVerifyCodeKey(normalizedPhone, bizType);
        long now = System.currentTimeMillis();
        VerifyCodeEntry currentEntry = VERIFY_CODE_CACHE.get(cacheKey);

        if (currentEntry != null && currentEntry.expireAt() >= now && now - currentEntry.createdAt() < CODE_RESEND_INTERVAL_MILLIS) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String verifyCode = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
        VERIFY_CODE_CACHE.put(
                cacheKey,
                new VerifyCodeEntry(verifyCode, now, now + CODE_EXPIRE_MILLIS)
        );
        log.debug("Verify code generated for phone={}, type={}", maskPhone(normalizedPhone), bizType);
        return new SendCodeVO(
                Math.toIntExact(CODE_EXPIRE_MILLIS / 1000),
                Math.toIntExact(CODE_RESEND_INTERVAL_MILLIS / 1000),
                exposeVerifyCode ? verifyCode : null
        );
    }

    private String buildVerifyCodeKey(String phone, String type) {
        return phone + ":" + type;
    }

    private void validatePhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("手机号不能为空");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
    }

    private void clearExpiredVerifyCodes() {
        long now = System.currentTimeMillis();
        VERIFY_CODE_CACHE.entrySet().removeIf(entry -> entry.getValue().expireAt() < now);
    }

    private void validateRequired(String value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
    }

    private String maskPhone(String phone) {
        return phone.length() >= 7 ? phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4) : phone;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String extractExtension(String originalFilename) {
        String fileName = normalizeText(originalFilename);
        if (fileName == null || !fileName.contains(".")) {
            return ".png";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        return extension.matches("\\.(png|jpg|jpeg|webp|gif)") ? extension : ".png";
    }

    private void deleteOldLocalAvatar(String avatarUrl, Path avatarDirectory) {
        String normalizedUrl = normalizeText(avatarUrl);
        if (normalizedUrl == null || !normalizedUrl.startsWith("/uploads/avatars/")) {
            return;
        }

        try {
            Path oldFile = avatarDirectory.resolve(normalizedUrl.substring("/uploads/avatars/".length())).normalize();
            if (oldFile.startsWith(avatarDirectory)) {
                Files.deleteIfExists(oldFile);
            }
        } catch (IOException exception) {
            log.warn("Failed to delete old avatar file: {}", normalizedUrl, exception);
        }
    }

    private record VerifyCodeEntry(String code, long createdAt, long expireAt) {
    }
}
