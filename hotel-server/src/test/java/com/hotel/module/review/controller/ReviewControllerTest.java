package com.hotel.module.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.review.dto.ReviewCreateRequest;
import com.hotel.module.review.entity.Review;
import com.hotel.module.review.mapper.ReviewMapper;
import com.hotel.module.resource.entity.Hotel;
import com.hotel.module.resource.mapper.HotelMapper;
import com.hotel.module.user.dto.LoginRequest;
import com.hotel.module.user.dto.RegisterRequest;
import UserRole;
import com.hotel.module.user.mapper.UserMapper;
import com.hotel.module.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/schema.sql"
})
@DisplayName("评价模块接口测试")
class ReviewControllerTest {

    private static final String TEST_PHONE = "13900000003";
    private static final String TEST_PASSWORD = "test123456";
    private static final String ADMIN_PHONE = "13800000000";
    private static final String ADMIN_PASSWORD = "admin123";

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

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    private Long testHotelId;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // 插入测试酒店
        Hotel hotel = new Hotel();
        hotel.setCityId(1L);
        hotel.setNameCn("评价测试酒店");
        hotel.setStatus(1);
        hotelMapper.insert(hotel);
        testHotelId = hotel.getId();

        // 注册+登录普通用户
        redisTemplate.opsForValue().set("code:" + TEST_PHONE + ":register", "123456");

        RegisterRequest regReq = new RegisterRequest();
        regReq.setPhone(TEST_PHONE);
        regReq.setPassword(TEST_PASSWORD);
        regReq.setCode("123456");
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regReq)));

        LoginRequest loginReq = new LoginRequest();
        loginReq.setPhone(TEST_PHONE);
        loginReq.setPassword(TEST_PASSWORD);
        String resp = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse().getContentAsString();
        userToken = objectMapper.readTree(resp).get("data").get("token").asText();

        // 登录管理员
        LoginRequest adminReq = new LoginRequest();
        adminReq.setPhone(ADMIN_PHONE);
        adminReq.setPassword(ADMIN_PASSWORD);
        String adminResp = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminResp).get("data").get("token").asText();
    }

    @AfterEach
    void cleanUp() {
        // 清理评价
        var reviews = reviewMapper.selectList(new LambdaQueryWrapper<>());
        if (reviews != null) reviews.forEach(r -> reviewMapper.deleteById(r.getId()));

        // 清理测试酒店
        if (testHotelId != null) hotelMapper.deleteById(testHotelId);

        // 清理测试用户
        var user = userMapper.selectByPhone(TEST_PHONE);
        if (user != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                    .eq(UserRole::getUserId, user.getId()));
            userMapper.deleteById(user.getId());
        }

        // 清理 Redis
        var keys = redisTemplate.keys("code:*");
        if (keys != null) keys.forEach(redisTemplate::delete);
    }

    // ==================== 1. 酒店评价列表 ====================

    @Nested
    @DisplayName("GET /api/review/hotel/{hotelId}")
    class ListByHotel {

        @Test
        @DisplayName("获取酒店评价列表（空列表）")
        void shouldReturnEmptyReviews() throws Exception {
            mockMvc.perform(get("/api/review/hotel/" + testHotelId)
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").exists())
                    .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        @DisplayName("支持按评分筛选")
        void shouldFilterByScore() throws Exception {
            mockMvc.perform(get("/api/review/hotel/" + testHotelId)
                            .param("score", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("无需登录可访问")
        void shouldAllowAnonymous() throws Exception {
            mockMvc.perform(get("/api/review/hotel/" + testHotelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    // ==================== 2. 发表评价 ====================

    @Nested
    @DisplayName("POST /api/review/create")
    class CreateReview {

        @Test
        @DisplayName("发表评价成功")
        void shouldCreateReview() throws Exception {
            ReviewCreateRequest req = new ReviewCreateRequest();
            req.setHotelId(testHotelId);
            req.setScore(5);
            req.setContent("非常不错的酒店，推荐！");
            req.setImages(List.of("https://img.example.com/1.jpg", "https://img.example.com/2.jpg"));

            mockMvc.perform(post("/api/review/create")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // 验证评价已写入数据库
            var reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                    .eq(Review::getHotelId, testHotelId));
            assert reviews.size() == 1;
            assert reviews.get(0).getScore() == 5;
            assert reviews.get(0).getContent().equals("非常不错的酒店，推荐！");
        }

        @Test
        @DisplayName("发表评价后列表可见")
        void shouldAppearInList() throws Exception {
            // 发表评价
            ReviewCreateRequest req = new ReviewCreateRequest();
            req.setHotelId(testHotelId);
            req.setScore(4);
            req.setContent("不错");
            mockMvc.perform(post("/api/review/create")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));

            // 列表应该有一条
            mockMvc.perform(get("/api/review/hotel/" + testHotelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].score").value(4))
                    .andExpect(jsonPath("$.data.records[0].user.nickname").exists());
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(post("/api/review/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 3. 商家回复 ====================

    @Nested
    @DisplayName("PUT /api/review/{id}/reply")
    class Reply {

        @Test
        @DisplayName("管理员回复成功")
        void shouldReplyAsAdmin() throws Exception {
            // 先发表评价
            ReviewCreateRequest req = new ReviewCreateRequest();
            req.setHotelId(testHotelId);
            req.setScore(4);
            req.setContent("环境很好");
            mockMvc.perform(post("/api/review/create")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));

            // 获取评价ID
            var reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                    .eq(Review::getHotelId, testHotelId));
            Long reviewId = reviews.get(0).getId();

            // 管理员回复
            mockMvc.perform(put("/api/review/" + reviewId + "/reply")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reply\":\"感谢您的评价，欢迎再次光临！\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // 验证回复已保存
            var updated = reviewMapper.selectById(reviewId);
            assert updated.getReply().equals("感谢您的评价，欢迎再次光临！");
            assert updated.getReplyTime() != null;
        }

        @Test
        @DisplayName("普通用户回复返回403")
        void shouldRejectNonAdmin() throws Exception {
            mockMvc.perform(put("/api/review/1/reply")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reply\":\"test\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(put("/api/review/1/reply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reply\":\"test\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
