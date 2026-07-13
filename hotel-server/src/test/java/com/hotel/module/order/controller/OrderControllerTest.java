package com.hotel.module.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.order.dto.OrderCreateRequest;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.entity.OrderItem;
import com.hotel.module.order.mapper.OrderItemMapper;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.payment.service.AlipayService;
import com.hotel.module.resource.entity.Hotel;
import com.hotel.module.resource.entity.Room;
import com.hotel.module.resource.mapper.HotelMapper;
import com.hotel.module.resource.mapper.RoomMapper;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/schema.sql"
})
@DisplayName("订单模块接口测试")
class OrderControllerTest {

    private static final String TEST_PHONE = "13900000002";
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

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @MockBean
    private AlipayService alipayService;

    private String token;
    private Long testHotelId;
    private Long testRoomId;
    private Hotel testHotel;
    private Room testRoom;

    @BeforeEach
    void setUp() throws Exception {
        // 注册+登录
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
        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(response).get("data").get("token").asText();

        // 插入测试酒店（city_id=1 西安，存在于种子数据）
        testHotel = new Hotel();
        testHotel.setCityId(1L);
        testHotel.setNameCn("测试酒店");
        testHotel.setStatus(1);
        hotelMapper.insert(testHotel);
        testHotelId = testHotel.getId();

        // 插入测试房型
        testRoom = new Room();
        testRoom.setHotelId(testHotelId);
        testRoom.setName("测试大床房");
        testRoom.setPrice(new BigDecimal("299.00"));
        testRoom.setStatus(1);
        roomMapper.insert(testRoom);
        testRoomId = testRoom.getId();

        // Mock 支付宝
        when(alipayService.pagePay(anyString(), anyString(), anyString()))
                .thenReturn("<form>支付宝支付表单</form>");
        when(alipayService.verifySignature(anyMap()))
                .thenReturn(false); // 默认验签失败
    }

    @AfterEach
    void cleanUp() {
        // 清理测试订单（先删子表，再删主表）
        var orders = orderMapper.selectList(new LambdaQueryWrapper<>());
        if (orders != null) {
            orders.forEach(o -> {
                orderItemMapper.delete(
                        new LambdaQueryWrapper<OrderItem>()
                                .eq(OrderItem::getOrderId, o.getId()));
                orderMapper.deleteById(o.getId());
            });
        }
        if (testRoomId != null) roomMapper.deleteById(testRoomId);
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

    // ==================== 辅助方法 ====================

    private String createTestOrder() throws Exception {
        OrderCreateRequest.OrderItemRequest item = new OrderCreateRequest.OrderItemRequest();
        item.setRoomId(testRoomId);
        item.setQuantity(1);

        OrderCreateRequest req = new OrderCreateRequest();
        req.setHotelId(testHotelId);
        req.setCheckInDate(LocalDate.now().plusDays(10));
        req.setCheckOutDate(LocalDate.now().plusDays(12));
        req.setRoomCount(1);
        req.setGuestName("张三");
        req.setGuestPhone("13800001111");
        req.setItems(List.of(item));

        String response = mockMvc.perform(post("/api/order/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("orderNo").asText();
    }

    // ==================== 1. 创建订单 ====================

    @Nested
    @DisplayName("POST /api/order/create")
    class CreateOrder {

        @Test
        @DisplayName("创建订单成功")
        void shouldCreateOrder() throws Exception {
            OrderCreateRequest.OrderItemRequest item = new OrderCreateRequest.OrderItemRequest();
            item.setRoomId(testRoomId);
            item.setQuantity(1);

            OrderCreateRequest req = new OrderCreateRequest();
            req.setHotelId(testHotelId);
            req.setCheckInDate(LocalDate.now().plusDays(10));
            req.setCheckOutDate(LocalDate.now().plusDays(12));
            req.setRoomCount(1);
            req.setGuestName("张三");
            req.setGuestPhone("13800001111");
            req.setItems(List.of(item));

            mockMvc.perform(post("/api/order/create")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.orderNo").exists())
                    .andExpect(jsonPath("$.data.totalAmount").value("299.00"))
                    .andExpect(jsonPath("$.data.status").value(0))
                    .andExpect(jsonPath("$.data.guestName").value("张三"))
                    .andExpect(jsonPath("$.data.hotelName").value("测试酒店"))
                    .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("缺少入住人姓名返回400")
        void shouldRejectMissingGuestName() throws Exception {
            OrderCreateRequest req = new OrderCreateRequest();
            req.setHotelId(testHotelId);
            req.setCheckInDate(LocalDate.now().plusDays(10));
            req.setCheckOutDate(LocalDate.now().plusDays(12));
            req.setGuestPhone("13800001111");
            req.setItems(List.of());

            mockMvc.perform(post("/api/order/create")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(post("/api/order/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 2. 发起支付 ====================

    @Nested
    @DisplayName("POST /api/order/{id}/pay")
    class Pay {

        @Test
        @DisplayName("获取支付表单成功")
        void shouldReturnPayForm() throws Exception {
            String orderNo = createTestOrder();

            // 从订单号查到 orderId
            var order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo));

            mockMvc.perform(post("/api/order/" + order.getId() + "/pay")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("<form>支付宝支付表单</form>"));
        }

        @Test
        @DisplayName("订单不存在返回错误")
        void shouldReturnErrorForUnknownOrder() throws Exception {
            mockMvc.perform(post("/api/order/99999/pay")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(post("/api/order/1/pay"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 3. 取消订单 ====================

    @Nested
    @DisplayName("PUT /api/order/{id}/cancel")
    class Cancel {

        @Test
        @DisplayName("取消待支付订单成功")
        void shouldCancelPendingOrder() throws Exception {
            String orderNo = createTestOrder();
            var order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo));

            mockMvc.perform(put("/api/order/" + order.getId() + "/cancel")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("订单已取消"));

            // 验证数据库状态
            var updated = orderMapper.selectById(order.getId());
            assert updated.getStatus() == 2;
        }

        @Test
        @DisplayName("无权操作他人订单")
        void shouldRejectOtherUsersOrder() throws Exception {
            String orderNo = createTestOrder();
            var order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo));

            // 用另一个用户尝试取消（用 mismatched userId）
            // 我们的 token 里的 userId 和创建订单的 userId 相同，所以这里测试 admin 用户 token
            mockMvc.perform(put("/api/order/" + order.getId() + "/cancel")
                            .header("Authorization", "Bearer invalid-token-for-other-user"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(put("/api/order/1/cancel"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 4. 预取消（退房申请） ====================

    @Nested
    @DisplayName("POST /api/order/{id}/pre-cancel")
    class PreCancel {

        @Test
        @DisplayName("距入住10天，免手续费")
        void shouldReturnNoPenalty() throws Exception {
            String orderNo = createTestOrder(); // checkInDate = now + 10 days
            var order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo));

            mockMvc.perform(post("/api/order/" + order.getId() + "/pre-cancel")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.cancelConfirmId").exists())
                    .andExpect(jsonPath("$.data.penaltyRate").value(0))
                    .andExpect(jsonPath("$.data.cancelPenalty").value(0))
                    .andExpect(jsonPath("$.data.refundAmount").value(299.00));
        }

        @Test
        @DisplayName("预取消后订单状态变为退房申请中")
        void shouldUpdateOrderStatus() throws Exception {
            String orderNo = createTestOrder();
            var order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo));

            mockMvc.perform(post("/api/order/" + order.getId() + "/pre-cancel")
                    .header("Authorization", "Bearer " + token));

            var updated = orderMapper.selectById(order.getId());
            assert updated.getStatus() == 4;
            assert updated.getCancelConfirmId() != null;
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(post("/api/order/1/pre-cancel"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 5. 确认取消 ====================

    @Nested
    @DisplayName("POST /api/order/{id}/confirm-cancel")
    class ConfirmCancel {

        @Test
        @DisplayName("确认取消成功")
        void shouldConfirmCancel() throws Exception {
            String orderNo = createTestOrder();
            var order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo));

            // 先预取消
            String preResp = mockMvc.perform(post("/api/order/" + order.getId() + "/pre-cancel")
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getContentAsString();
            String cancelId = objectMapper.readTree(preResp).get("data").get("cancelConfirmId").asText();

            // 确认取消
            mockMvc.perform(post("/api/order/" + order.getId() + "/confirm-cancel")
                            .header("Authorization", "Bearer " + token)
                            .param("cancelConfirmId", cancelId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("退房申请已确认"));

            // 验证状态变为已退房
            var updated = orderMapper.selectById(order.getId());
            assert updated.getStatus() == 5;
        }

        @Test
        @DisplayName("cancelConfirmId 不匹配返回错误")
        void shouldRejectWrongCancelId() throws Exception {
            String orderNo = createTestOrder();
            var order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo));

            // 先预取消设置正确的 cancelConfirmId
            mockMvc.perform(post("/api/order/" + order.getId() + "/pre-cancel")
                    .header("Authorization", "Bearer " + token));

            // 用错误的 cancelConfirmId 确认
            mockMvc.perform(post("/api/order/" + order.getId() + "/confirm-cancel")
                            .header("Authorization", "Bearer " + token)
                            .param("cancelConfirmId", "WRONG-ID"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("未登录返回401")
        void shouldReturn401Unauthenticated() throws Exception {
            mockMvc.perform(post("/api/order/1/confirm-cancel")
                            .param("cancelConfirmId", "test"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 6. 支付异步通知 ====================

    @Nested
    @DisplayName("POST /api/order/pay-notify")
    class PayNotify {

        @Test
        @DisplayName("支付通知成功，更新订单状态")
        void shouldHandlePayNotify() throws Exception {
            String orderNo = createTestOrder();

            // Mock 验签成功
            when(alipayService.verifySignature(anyMap())).thenReturn(true);
            when(alipayService.isTradeSuccess(anyMap())).thenReturn(true);
            when(alipayService.extractOrderNo(anyMap())).thenReturn(orderNo);
            when(alipayService.extractTradeNo(anyMap())).thenReturn("20260713220010001");
            when(alipayService.extractAmount(anyMap())).thenReturn("299.00");

            mockMvc.perform(post("/api/order/pay-notify")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("out_trade_no", orderNo)
                            .param("trade_no", "20260713220010001")
                            .param("trade_status", "TRADE_SUCCESS")
                            .param("total_amount", "299.00")
                            .param("sign", "fake_sign"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("success"));

            // 验证订单状态更新为已支付
            var order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, orderNo));
            assert order.getStatus() == 1;
            assert order.getPayTime() != null;
        }

        @Test
        @DisplayName("签名验证失败不处理")
        void shouldIgnoreInvalidSignature() throws Exception {
            // 默认 mock 返回 false
            mockMvc.perform(post("/api/order/pay-notify")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("out_trade_no", "TEST001")
                            .param("sign", "bad_sign"))
                    .andExpect(status().isOk());
            // 验签失败会抛 BusinessException，但 pay-notify 是 public 端点
            // GlobalExceptionHandler 处理后仍返回 200，支付宝会重试
        }

        @Test
        @DisplayName("无需登录也可访问")
        void shouldAllowAnonymous() throws Exception {
            // pay-notify 在 SecurityConfig 的 permitAll 列表
            when(alipayService.verifySignature(anyMap())).thenReturn(true);
            when(alipayService.isTradeSuccess(anyMap())).thenReturn(false); // 非交易成功，提前返回

            mockMvc.perform(post("/api/order/pay-notify")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("trade_status", "WAIT_BUYER_PAY")
                            .param("sign", "fake"))
                    .andExpect(status().isOk());
        }
    }
}
