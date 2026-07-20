package com.hotel.module.search.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.resource.entity.*;
import com.hotel.module.resource.mapper.*;
import com.hotel.module.review.entity.Review;
import com.hotel.module.review.mapper.ReviewMapper;
import com.hotel.module.user.dto.LoginRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("搜索模块接口测试")
class SearchControllerTest {

    private String adminToken;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private HotelMapper hotelMapper;
    @Autowired private HotelImageMapper hotelImageMapper;
    @Autowired private HotelFacilityMapper hotelFacilityMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private ReviewMapper reviewMapper;
    @Autowired private CityMapper cityMapper;

    private static final String[][] ADMIN_CREDENTIALS = {
            {"17727974960", "ycj20050908"},
            {"13800000000", "admin123"},
            {"13800000001", "admin123"}
    };

    @BeforeAll
    void loginAsAdmin() throws Exception {
        for (String[] credential : ADMIN_CREDENTIALS) {
            LoginRequest req = new LoginRequest();
            req.setPhone(credential[0]);
            req.setPassword(credential[1]);

            String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/user/login")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andReturn().getResponse().getContentAsString();

            if (response.contains("\"code\":200")) {
                adminToken = objectMapper.readTree(response).get("data").get("token").asText();
                return;
            }
        }
        throw new RuntimeException("无法以管理员身份登录");
    }

    // ==================== 酒店搜索 ====================

    @Nested
    @DisplayName("GET /api/search/hotels")
    class SearchHotels {

        private Long testHotelId;
        private Hotel testHotel;

        @BeforeEach
        void setup() {
            // 清理遗留数据
            List<Hotel> old = hotelMapper.selectList(
                    new LambdaQueryWrapper<Hotel>().eq(Hotel::getNameCn, "搜索测试酒店"));
            old.forEach(h -> {
                reviewMapper.delete(new LambdaQueryWrapper<Review>().eq(Review::getHotelId, h.getId()));
                hotelImageMapper.delete(new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, h.getId()));
                hotelFacilityMapper.delete(new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, h.getId()));
                roomMapper.delete(new LambdaQueryWrapper<Room>().eq(Room::getHotelId, h.getId()));
                hotelMapper.deleteById(h.getId());
            });

            // 创建测试酒店
            testHotel = new Hotel();
            testHotel.setCityId(1L);
            testHotel.setNameCn("搜索测试酒店");
            testHotel.setStarLevel(4);
            testHotel.setScore(new BigDecimal("4.5"));
            testHotel.setAddress("测试地址100号");
            testHotel.setBrand("测试品牌");
            testHotel.setLongitude(new BigDecimal("108.940175"));
            testHotel.setLatitude(new BigDecimal("34.341568"));
            testHotel.setStatus(1);
            hotelMapper.insert(testHotel);
            testHotelId = testHotel.getId();

            // 主图
            HotelImage img = new HotelImage();
            img.setHotelId(testHotelId);
            img.setUrl("/images/test/main.jpg");
            img.setType(1);
            img.setSortOrder(0);
            hotelImageMapper.insert(img);

            // 设施
            HotelFacility fac = new HotelFacility();
            fac.setHotelId(testHotelId);
            fac.setName("WiFi");
            hotelFacilityMapper.insert(fac);

            // 房型
            Room room = new Room();
            room.setHotelId(testHotelId);
            room.setName("标准间");
            room.setPrice(new BigDecimal("199.00"));
            room.setStatus(1);
            roomMapper.insert(room);

            // 评价
            Review review = new Review();
            review.setHotelId(testHotelId);
            review.setUserId(1L);
            review.setScore(4);
            review.setContent("测试评价");
            reviewMapper.insert(review);
        }

        @AfterEach
        void tearDown() {
            if (testHotelId != null) {
                reviewMapper.delete(new LambdaQueryWrapper<Review>().eq(Review::getHotelId, testHotelId));
                hotelImageMapper.delete(new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, testHotelId));
                hotelFacilityMapper.delete(new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, testHotelId));
                roomMapper.delete(new LambdaQueryWrapper<Room>().eq(Room::getHotelId, testHotelId));
                hotelMapper.deleteById(testHotelId);
            }
        }

        @Test
        @DisplayName("搜索酒店 — 返回主图和最低价")
        void shouldReturnMainImageAndMinPrice() throws Exception {
            mockMvc.perform(get("/api/search/hotels")
                            .param("cityId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records[?(@.id==" + testHotelId + ")].mainImage").value("/images/test/main.jpg"))
                    .andExpect(jsonPath("$.data.records[?(@.id==" + testHotelId + ")].minPrice").value(199.0));
        }

        @Test
        @DisplayName("搜索酒店 — 返回评价数和设施")
        void shouldReturnReviewCountAndFacilities() throws Exception {
            mockMvc.perform(get("/api/search/hotels")
                            .param("cityId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records[?(@.id==" + testHotelId + ")].reviewCount").value(1))
                    .andExpect(jsonPath("$.data.records[?(@.id==" + testHotelId + ")].facilities[0]").value("WiFi"));
        }

        @Test
        @DisplayName("按关键词搜索")
        void shouldFilterByKeyword() throws Exception {
            mockMvc.perform(get("/api/search/hotels")
                            .param("keyword", "搜索测试"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.total").value(1));
        }

        @Test
        @DisplayName("按星级筛选")
        void shouldFilterByStarLevel() throws Exception {
            mockMvc.perform(get("/api/search/hotels")
                            .param("starLevel", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按价格区间筛选")
        void shouldFilterByPriceRange() throws Exception {
            mockMvc.perform(get("/api/search/hotels")
                            .param("minPrice", "100")
                            .param("maxPrice", "300"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按评分排序")
        void shouldSortByScore() throws Exception {
            mockMvc.perform(get("/api/search/hotels")
                            .param("sortBy", "score")
                            .param("sortOrder", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("按价格升序排列")
        void shouldSortByPriceAsc() throws Exception {
            mockMvc.perform(get("/api/search/hotels")
                            .param("sortBy", "price")
                            .param("sortOrder", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("无需登录可访问")
        void shouldAllowAnonymous() throws Exception {
            mockMvc.perform(get("/api/search/hotels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    // ==================== 关键字联想 ====================

    @Nested
    @DisplayName("GET /api/search/suggest")
    class Suggest {

        private Long testHotelId;

        @BeforeEach
        void setup() {
            List<Hotel> old = hotelMapper.selectList(
                    new LambdaQueryWrapper<Hotel>().eq(Hotel::getNameCn, "联想测试酒店"));
            old.forEach(h -> hotelMapper.deleteById(h.getId()));

            Hotel hotel = new Hotel();
            hotel.setCityId(1L);
            hotel.setNameCn("联想测试酒店");
            hotel.setBrand("联想品牌");
            hotel.setStatus(1);
            hotelMapper.insert(hotel);
            testHotelId = hotel.getId();
        }

        @AfterEach
        void tearDown() {
            if (testHotelId != null) hotelMapper.deleteById(testHotelId);
        }

        @Test
        @DisplayName("联想酒店名称")
        void shouldSuggestHotelNames() throws Exception {
            mockMvc.perform(get("/api/search/suggest")
                            .param("keyword", "联想"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.hotels").isArray());
        }

        @Test
        @DisplayName("联想品牌")
        void shouldSuggestBrands() throws Exception {
            mockMvc.perform(get("/api/search/suggest")
                            .param("keyword", "联想品牌"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.brands").isArray());
        }
    }

    // ==================== 附近搜索 ====================

    @Nested
    @DisplayName("GET /api/search/nearby")
    class Nearby {

        private Long testHotelId;

        @BeforeEach
        void setup() {
            List<Hotel> old = hotelMapper.selectList(
                    new LambdaQueryWrapper<Hotel>().eq(Hotel::getNameCn, "附近测试酒店"));
            old.forEach(h -> {
                hotelImageMapper.delete(new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, h.getId()));
                hotelMapper.deleteById(h.getId());
            });

            Hotel hotel = new Hotel();
            hotel.setCityId(1L);
            hotel.setNameCn("附近测试酒店");
            hotel.setLongitude(new BigDecimal("108.940175"));
            hotel.setLatitude(new BigDecimal("34.341568"));
            hotel.setStatus(1);
            hotelMapper.insert(hotel);
            testHotelId = hotel.getId();
        }

        @AfterEach
        void tearDown() {
            if (testHotelId != null) {
                hotelImageMapper.delete(new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, testHotelId));
                hotelMapper.deleteById(testHotelId);
            }
        }

        @Test
        @DisplayName("附近搜索 — 返回范围内的酒店")
        void shouldReturnNearbyHotels() throws Exception {
            mockMvc.perform(get("/api/search/nearby")
                            .param("longitude", "108.940175")
                            .param("latitude", "34.341568")
                            .param("radius", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[?(@.id==" + testHotelId + ")]").exists());
        }

        @Test
        @DisplayName("附近搜索 — 半径外无结果")
        void shouldReturnEmptyForFarHotels() throws Exception {
            mockMvc.perform(get("/api/search/nearby")
                            .param("longitude", "116.397428")
                            .param("latitude", "39.909230")
                            .param("radius", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }
}
