package com.hotel.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.user.dto.LoginRequest;
import com.hotel.module.user.dto.RegisterRequest;
import com.hotel.module.user.dto.UpdateUserRequest;
import com.hotel.module.user.entity.UserRole;
import com.hotel.module.user.mapper.UserMapper;
import com.hotel.module.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("用户模块接口测试")
class UserControllerTest {

    private static final String TEST_EMAIL = "test@qq.com";
    private static final String TEST_PHONE = "13900000001";
    private static final String TEST_PASSWORD = "test123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @MockBean
    private JavaMailSender mailSender;

    @AfterEach
    void cleanUp() {
        // 清理Redis验证码
        Set<String> keys = redisTemplate.keys("code:*");
        if (keys != null) keys.forEach(redisTemplate::delete);

        // 清理测试用户
        var u1 = userMapper.selectByPhone(TEST_PHONE);
        if (u1 != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, u1.getId()));
            userMapper.deleteById(u1.getId());
        }
        var u2 = userMapper.selectByEmail(TEST_EMAIL);
        if (u2 != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, u2.getId()));
            userMapper.deleteById(u2.getId());
        }

        SecurityContextHolder.clearContext();
    }

    // ==================== 1. 发送验证码 ====================

    @Nested
    @DisplayName("POST /api/user/send-code")
    class SendCode {

        @Test
        @DisplayName("发送验证码成功")
        void shouldSendCode() throws Exception {
            mockMvc.perform(post("/api/user/send-code")
                            .param("email", TEST_EMAIL)
                            .param("type", "register"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("验证码已发送"));

            // 验证 Redis 中有验证码
            String code = redisTemplate.opsForValue().get("code:" + TEST_EMAIL + ":register");
            assert code != null;
            assert code.length() == 6;
        }

        @Test
        @DisplayName("发送验证码 — type 默认为 register")
        void shouldDefaultType() throws Exception {
            mockMvc.perform(post("/api/user/send-code")
                            .param("email", TEST_EMAIL))
                    .andExpect(status().isOk());

            String code = redisTemplate.opsForValue().get("code:" + TEST_EMAIL + ":register");
            assert code != null;
        }
    }

    // ==================== 2. 用户注册 ====================

    @Nested
    @DisplayName("POST /api/user/register")
    class Register {

        @Test
        @DisplayName("邮箱注册成功")
        void shouldRegisterByEmail() throws Exception {
            // 先发验证码
            redisTemplate.opsForValue().set("code:" + TEST_EMAIL + ":register", "123456");

            RegisterRequest req = new RegisterRequest();
            req.setEmail(TEST_EMAIL);
            req.setPassword(TEST_PASSWORD);
            req.setCode("123456");

            mockMvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // 验证用户已创建
            var user = userMapper.selectByEmail(TEST_EMAIL);
            assert user != null;
            assert user.getEmail().equals(TEST_EMAIL);
            assert user.getStatus() == 1;
            assert user.getNickname() != null;
        }

        @Test
        @DisplayName("验证码错误时注册失败")
        void shouldRejectWrongCode() throws Exception {
            redisTemplate.opsForValue().set("code:" + TEST_EMAIL + ":register", "123456");

            RegisterRequest req = new RegisterRequest();
            req.setEmail(TEST_EMAIL);
            req.setPassword(TEST_PASSWORD);
            req.setCode("000000"); // 错误的验证码

            mockMvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("密码少于6位时校验失败")
        void shouldRejectShortPassword() throws Exception {
            redisTemplate.opsForValue().set("code:" + TEST_EMAIL + ":register", "123456");

            RegisterRequest req = new RegisterRequest();
            req.setEmail(TEST_EMAIL);
            req.setPassword("12345"); // 5位密码
            req.setCode("123456");

            mockMvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("邮箱已注册时拒绝重复注册")
        void shouldRejectDuplicateEmail() throws Exception {
            redisTemplate.opsForValue().set("code:" + TEST_EMAIL + ":register", "123456");

            RegisterRequest req = new RegisterRequest();
            req.setEmail(TEST_EMAIL);
            req.setPassword(TEST_PASSWORD);
            req.setCode("123456");

            // 第一次注册
            mockMvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            // 重新发码
            redisTemplate.opsForValue().set("code:" + TEST_EMAIL + ":register", "654321");
            req.setCode("654321");

            // 第二次注册
            mockMvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    // ==================== 3. 用户登录 ====================

    @Nested
    @DisplayName("POST /api/user/login")
    class Login {

        private String registerAndGetToken() throws Exception {
            redisTemplate.opsForValue().set("code:" + TEST_PHONE + ":register", "123456");

            RegisterRequest req = new RegisterRequest();
            req.setPhone(TEST_PHONE);
            req.setPassword(TEST_PASSWORD);
            req.setCode("123456");
            mockMvc.perform(post("/api/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));

            LoginRequest loginReq = new LoginRequest();
            loginReq.setPhone(TEST_PHONE);
            loginReq.setPassword(TEST_PASSWORD);

            String response = mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            return objectMapper.readTree(response).get("data").get("token").asText();
        }

        @Test
        @DisplayName("手机号登录成功并返回token")
        void shouldLoginByPhone() throws Exception {
            redisTemplate.opsForValue().set("code:" + TEST_PHONE + ":register", "123456");

            RegisterRequest req = new RegisterRequest();
            req.setPhone(TEST_PHONE);
            req.setPassword(TEST_PASSWORD);
            req.setCode("123456");
            mockMvc.perform(post("/api/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));

            LoginRequest loginReq = new LoginRequest();
            loginReq.setPhone(TEST_PHONE);
            loginReq.setPassword(TEST_PASSWORD);

            mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.token").exists())
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.nickname").exists())
                    .andExpect(jsonPath("$.data.roles").exists());
        }

        @Test
        @DisplayName("密码错误时登录失败")
        void shouldRejectWrongPassword() throws Exception {
            redisTemplate.opsForValue().set("code:" + TEST_PHONE + ":register", "123456");

            RegisterRequest req = new RegisterRequest();
            req.setPhone(TEST_PHONE);
            req.setPassword(TEST_PASSWORD);
            req.setCode("123456");
            mockMvc.perform(post("/api/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));

            LoginRequest loginReq = new LoginRequest();
            loginReq.setPhone(TEST_PHONE);
            loginReq.setPassword("wrongpassword");

            mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("密码错误"));
        }

        @Test
        @DisplayName("用户不存在时登录失败")
        void shouldRejectUnknownUser() throws Exception {
            LoginRequest loginReq = new LoginRequest();
            loginReq.setPhone("13800000099");
            loginReq.setPassword(TEST_PASSWORD);

            mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("用户不存在"));
        }
    }

    // ==================== 4. 获取当前用户信息 ====================

    @Nested
    @DisplayName("GET /api/user/info")
    class GetInfo {

        @Test
        @DisplayName("已登录获取用户信息成功")
        void shouldReturnUserInfo() throws Exception {
            String token = registerAndLogin();

            mockMvc.perform(get("/api/user/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.phone").value(TEST_PHONE))
                    .andExpect(jsonPath("$.data.roles").exists())
                    .andExpect(jsonPath("$.data.permissions").exists())
                    .andExpect(jsonPath("$.data.createTime").exists());
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(get("/api/user/info"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("无效token返回401")
        void shouldReturn401InvalidToken() throws Exception {
            mockMvc.perform(get("/api/user/info")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 5. 更新用户信息 ====================

    @Nested
    @DisplayName("PUT /api/user/update")
    class UpdateProfile {

        @Test
        @DisplayName("更新昵称和头像成功")
        void shouldUpdateProfile() throws Exception {
            String token = registerAndLogin();

            UpdateUserRequest req = new UpdateUserRequest();
            req.setNickname("新昵称");
            req.setAvatar("https://example.com/avatar.png");

            mockMvc.perform(put("/api/user/update")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // 验证更新生效
            mockMvc.perform(get("/api/user/info")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                    .andExpect(jsonPath("$.data.avatar").value("https://example.com/avatar.png"));
        }

        @Test
        @DisplayName("未登录更新返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setNickname("test");

            mockMvc.perform(put("/api/user/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 6. 我的订单列表 ====================

    @Nested
    @DisplayName("GET /api/user/orders")
    class MyOrders {

        @Test
        @DisplayName("已登录获取订单列表 — 空列表")
        void shouldReturnEmptyOrders() throws Exception {
            String token = registerAndLogin();

            mockMvc.perform(get("/api/user/orders")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").exists())
                    .andExpect(jsonPath("$.data.total").value(0))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.size").value(10));
        }

        @Test
        @DisplayName("支持分页参数")
        void shouldSupportPaging() throws Exception {
            String token = registerAndLogin();

            mockMvc.perform(get("/api/user/orders")
                            .header("Authorization", "Bearer " + token)
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page").value(2))
                    .andExpect(jsonPath("$.data.size").value(5));
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(get("/api/user/orders"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 7. 订单详情 ====================

    @Nested
    @DisplayName("GET /api/user/orders/{id}")
    class OrderDetail {

        @Test
        @DisplayName("订单不存在时返回错误")
        void shouldReturnErrorForUnknownOrder() throws Exception {
            String token = registerAndLogin();

            mockMvc.perform(get("/api/user/orders/99999")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(get("/api/user/orders/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 辅助方法 ====================

    private String registerAndLogin() throws Exception {
        redisTemplate.opsForValue().set("code:" + TEST_PHONE + ":register", "123456");

        RegisterRequest req = new RegisterRequest();
        req.setPhone(TEST_PHONE);
        req.setPassword(TEST_PASSWORD);
        req.setCode("123456");
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        LoginRequest loginReq = new LoginRequest();
        loginReq.setPhone(TEST_PHONE);
        loginReq.setPassword(TEST_PASSWORD);

        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("data").get("token").asText();
    }
}
