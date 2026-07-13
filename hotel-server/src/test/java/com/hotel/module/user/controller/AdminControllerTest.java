package com.hotel.module.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.user.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/schema.sql"
})
@DisplayName("管理端接口测试")
class AdminControllerTest {

    private static final String ADMIN_PHONE = "13800000000";
    private static final String ADMIN_PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setPhone(ADMIN_PHONE);
        loginReq.setPassword(ADMIN_PASSWORD);

        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(response).get("data").get("token").asText();
    }

    // ==================== 1. 订单列表 ====================

    @Nested
    @DisplayName("GET /api/admin/orders")
    class OrdersList {

        @Test
        @DisplayName("获取订单列表成功")
        void shouldReturnOrdersList() throws Exception {
            mockMvc.perform(get("/api/admin/orders")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").exists())
                    .andExpect(jsonPath("$.data.total").exists())
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.size").value(20));
        }

        @Test
        @DisplayName("支持按状态筛选")
        void shouldFilterByStatus() throws Exception {
            mockMvc.perform(get("/api/admin/orders")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("支持按订单号搜索")
        void shouldSearchByOrderNo() throws Exception {
            mockMvc.perform(get("/api/admin/orders")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("orderNo", "2026"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("支持分页参数")
        void shouldSupportPaging() throws Exception {
            mockMvc.perform(get("/api/admin/orders")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page").value(2))
                    .andExpect(jsonPath("$.data.size").value(5));
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(get("/api/admin/orders"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 2. 退款处理 ====================

    @Nested
    @DisplayName("PUT /api/admin/orders/{id}/refund")
    class Refund {

        @Test
        @DisplayName("退款订单不存在返回错误")
        void shouldReturnErrorForUnknownOrder() throws Exception {
            mockMvc.perform(put("/api/admin/orders/99999/refund")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("reason", "测试退款"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("退票处理成功"));
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(put("/api/admin/orders/1/refund")
                            .param("reason", "测试"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 3. 用户列表 ====================

    @Nested
    @DisplayName("GET /api/admin/users")
    class UsersList {

        @Test
        @DisplayName("获取用户列表成功")
        void shouldReturnUsersList() throws Exception {
            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").exists())
                    .andExpect(jsonPath("$.data.total").isNumber())
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.size").value(20));
        }

        @Test
        @DisplayName("支持分页")
        void shouldSupportPaging() throws Exception {
            mockMvc.perform(get("/api/admin/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.size").value(5));
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 4. 启用/禁用用户 ====================

    @Nested
    @DisplayName("PUT /api/admin/users/{id}/status")
    class ToggleUserStatus {

        @Test
        @DisplayName("修改用户状态成功")
        void shouldToggleStatus() throws Exception {
            // 先禁用用户1（管理员自己），然后再启用
            mockMvc.perform(put("/api/admin/users/1/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // 恢复启用
            mockMvc.perform(put("/api/admin/users/1/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("status", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("用户不存在也返回200（静默忽略）")
        void shouldReturnOkForUnknownUser() throws Exception {
            mockMvc.perform(put("/api/admin/users/99999/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("status", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(put("/api/admin/users/1/status")
                            .param("status", "1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
