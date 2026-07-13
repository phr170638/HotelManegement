package com.hotel.module.resource.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.resource.dto.HotelSaveRequest;
import com.hotel.module.resource.entity.*;
import com.hotel.module.resource.mapper.*;
import com.hotel.module.user.dto.LoginRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@DisplayName("资源管理模块接口测试")
class ResourceControllerTest {

    private String adminToken;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private HotelMapper hotelMapper;
    @Autowired private HotelImageMapper hotelImageMapper;
    @Autowired private HotelFacilityMapper hotelFacilityMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private RoomImageMapper roomImageMapper;
    @Autowired private RoomFacilityMapper roomFacilityMapper;
    @Autowired private CountryMapper countryMapper;
    @Autowired private CityMapper cityMapper;

    private static final String ADMIN_PASSWORD = "admin123";

    @BeforeAll
    void loginAsAdmin() throws Exception {
        // 尝试两套种子数据的管理员：schema.sql用13800000000，Seed.sql用13800000001
        String[] phones = {"13800000000", "13800000001"};
        for (String phone : phones) {
            LoginRequest req = new LoginRequest();
            req.setPhone(phone);
            req.setPassword(ADMIN_PASSWORD);

            String response = mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andReturn().getResponse().getContentAsString();

            if (response.contains("\"code\":200")) {
                adminToken = objectMapper.readTree(response).get("data").get("token").asText();
                return;
            }
        }
        throw new RuntimeException("无法以管理员身份登录");
    }

    @AfterEach
    void cleanUp() {
        // 测试数据在删除方法中已自行清理；
        // 若测试失败未清理干净，手动清理以 hotel name 匹配的测试数据
    }

    // ==================== 辅助方法 ====================

    private void deleteHotelCascade(Long hotelId) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getHotelId, hotelId));
        for (Room room : rooms) {
            roomImageMapper.delete(new LambdaQueryWrapper<RoomImage>().eq(RoomImage::getRoomId, room.getId()));
            roomFacilityMapper.delete(new LambdaQueryWrapper<RoomFacility>().eq(RoomFacility::getRoomId, room.getId()));
            roomMapper.deleteById(room.getId());
        }
        hotelImageMapper.delete(new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, hotelId));
        hotelFacilityMapper.delete(new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, hotelId));
        hotelMapper.deleteById(hotelId);
    }

    // ==================== 酒店更新 ====================

    @Nested
    @DisplayName("PUT /api/resource/hotels/{id} — 酒店更新")
    class HotelUpdate {

        @BeforeEach
        void cleanUpBefore() {
            for (String name : List.of("级联更新测试酒店", "级联设施更新测试")) {
                List<Hotel> hotels = hotelMapper.selectList(
                    new LambdaQueryWrapper<Hotel>().eq(Hotel::getNameCn, name));
                hotels.forEach(h -> deleteHotelCascade(h.getId()));
            }
        }

        @Test
        @DisplayName("更新酒店图片 — 旧图删除，新图插入")
        void shouldReplaceImagesOnUpdate() throws Exception {
            // 1. 先创建一个酒店（带图片）
            HotelSaveRequest createReq = new HotelSaveRequest();
            createReq.setCityId(1L);
            createReq.setNameCn("级联更新测试酒店");
            createReq.setStarLevel(4);
            createReq.setImageUrls(List.of("/images/hotels/test/old-1.jpg", "/images/hotels/test/old-2.jpg"));
            createReq.setFacilities(List.of("WiFi"));

            String createResp = mockMvc.perform(post("/api/resource/hotels")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // 从创建响应中提取不了ID，直接从DB查
            Hotel created = hotelMapper.selectOne(new LambdaQueryWrapper<Hotel>()
                    .eq(Hotel::getNameCn, "级联更新测试酒店"));
            Long hotelId = created.getId();
            long oldImageCount = hotelImageMapper.selectCount(
                    new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, hotelId));
            assert oldImageCount == 2 : "创建后应有2张图片，实际：" + oldImageCount;

            // 2. 更新酒店 — 换一批新图片
            HotelSaveRequest updateReq = new HotelSaveRequest();
            updateReq.setCityId(1L);
            updateReq.setNameCn("级联更新测试酒店-已更新");
            updateReq.setStarLevel(5);
            updateReq.setImageUrls(List.of("/images/hotels/test/new-1.jpg", "/images/hotels/test/new-2.jpg", "/images/hotels/test/new-3.jpg"));

            mockMvc.perform(put("/api/resource/hotels/" + hotelId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // 3. 验证：旧图片已删除，新图片已插入
            List<HotelImage> images = hotelImageMapper.selectList(
                    new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, hotelId).orderByAsc(HotelImage::getSortOrder));
            assert images.size() == 3 : "更新后应有3张图片，实际：" + images.size();
            assert images.get(0).getUrl().equals("/images/hotels/test/new-1.jpg");
            assert images.get(1).getUrl().equals("/images/hotels/test/new-2.jpg");
            assert images.get(2).getUrl().equals("/images/hotels/test/new-3.jpg");

            // 清理
            deleteHotelCascade(hotelId);
        }

        @Test
        @DisplayName("更新酒店设施 — 旧设施删除，新设施插入")
        void shouldReplaceFacilitiesOnUpdate() throws Exception {
            // 创建酒店（带设施）
            HotelSaveRequest createReq = new HotelSaveRequest();
            createReq.setCityId(1L);
            createReq.setNameCn("级联设施更新测试");
            createReq.setStarLevel(3);
            createReq.setFacilities(List.of("WiFi", "停车场", "泳池"));

            mockMvc.perform(post("/api/resource/hotels")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isOk());

            Hotel created = hotelMapper.selectOne(new LambdaQueryWrapper<Hotel>()
                    .eq(Hotel::getNameCn, "级联设施更新测试"));
            Long hotelId = created.getId();
            long oldCount = hotelFacilityMapper.selectCount(
                    new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, hotelId));
            assert oldCount == 3 : "创建后应有3个设施，实际：" + oldCount;

            // 更新 — 换一批设施
            HotelSaveRequest updateReq = new HotelSaveRequest();
            updateReq.setCityId(1L);
            updateReq.setNameCn("级联设施更新测试-已更新");
            updateReq.setStarLevel(3);
            updateReq.setFacilities(List.of("健身房", "SPA"));

            mockMvc.perform(put("/api/resource/hotels/" + hotelId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk());

            // 验证：旧设施已删除，新设施已插入
            List<HotelFacility> facilities = hotelFacilityMapper.selectList(
                    new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, hotelId));
            assert facilities.size() == 2 : "更新后应有2个设施，实际：" + facilities.size();
            List<String> names = facilities.stream().map(HotelFacility::getName).toList();
            assert names.contains("健身房") : "应包含'健身房'";
            assert names.contains("SPA") : "应包含'SPA'";

            // 清理
            deleteHotelCascade(hotelId);
        }

        @Test
        @DisplayName("未登录更新酒店 — 返回403")
        void shouldReturn403WithoutAuth() throws Exception {
            HotelSaveRequest req = new HotelSaveRequest();
            req.setCityId(1L);
            req.setNameCn("未登录更新");

            mockMvc.perform(put("/api/resource/hotels/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== 酒店删除 ====================

    @Nested
    @DisplayName("DELETE /api/resource/hotels/{id} — 酒店删除")
    class HotelDelete {

        @BeforeEach
        void cleanUpBefore() {
            for (String name : List.of("删除测试酒店", "级联删图测试", "级联删设施测试", "级联删房型测试")) {
                List<Hotel> hotels = hotelMapper.selectList(
                    new LambdaQueryWrapper<Hotel>().eq(Hotel::getNameCn, name));
                hotels.forEach(h -> deleteHotelCascade(h.getId()));
            }
        }

        @Test
        @DisplayName("删除酒店 — 酒店记录消失")
        void shouldDeleteHotelRecord() throws Exception {
            HotelSaveRequest req = new HotelSaveRequest();
            req.setCityId(1L);
            req.setNameCn("删除测试酒店");
            req.setStarLevel(3);

            mockMvc.perform(post("/api/resource/hotels")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            Hotel created = hotelMapper.selectOne(new LambdaQueryWrapper<Hotel>()
                    .eq(Hotel::getNameCn, "删除测试酒店"));
            Long hotelId = created.getId();

            mockMvc.perform(delete("/api/resource/hotels/" + hotelId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            // 验证酒店已删除
            Hotel deleted = hotelMapper.selectById(hotelId);
            assert deleted == null : "酒店记录应该被删除";
        }

        @Test
        @DisplayName("删除酒店 — 酒店图片同时删除")
        void shouldCascadeDeleteHotelImages() throws Exception {
            HotelSaveRequest req = new HotelSaveRequest();
            req.setCityId(1L);
            req.setNameCn("级联删图测试");
            req.setStarLevel(3);
            req.setImageUrls(List.of("/images/test/1.jpg", "/images/test/2.jpg"));

            mockMvc.perform(post("/api/resource/hotels")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            Hotel created = hotelMapper.selectOne(new LambdaQueryWrapper<Hotel>()
                    .eq(Hotel::getNameCn, "级联删图测试"));
            Long hotelId = created.getId();

            mockMvc.perform(delete("/api/resource/hotels/" + hotelId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            Long imageCount = hotelImageMapper.selectCount(
                    new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, hotelId));
            assert imageCount == 0 : "酒店图片应该被级联删除，实际剩余：" + imageCount;
        }

        @Test
        @DisplayName("删除酒店 — 酒店设施同时删除")
        void shouldCascadeDeleteHotelFacilities() throws Exception {
            HotelSaveRequest req = new HotelSaveRequest();
            req.setCityId(1L);
            req.setNameCn("级联删设施测试");
            req.setStarLevel(3);
            req.setFacilities(List.of("WiFi", "停车场"));

            mockMvc.perform(post("/api/resource/hotels")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            Hotel created = hotelMapper.selectOne(new LambdaQueryWrapper<Hotel>()
                    .eq(Hotel::getNameCn, "级联删设施测试"));
            Long hotelId = created.getId();

            mockMvc.perform(delete("/api/resource/hotels/" + hotelId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            Long count = hotelFacilityMapper.selectCount(
                    new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, hotelId));
            assert count == 0 : "酒店设施应该被级联删除，实际剩余：" + count;
        }

        @Test
        @DisplayName("删除酒店 — 房型及房型图片/设施同时删除")
        void shouldCascadeDeleteRoomsAndRoomDetails() throws Exception {
            // 创建酒店
            HotelSaveRequest req = new HotelSaveRequest();
            req.setCityId(1L);
            req.setNameCn("级联删房型测试");
            req.setStarLevel(4);

            mockMvc.perform(post("/api/resource/hotels")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            Hotel created = hotelMapper.selectOne(new LambdaQueryWrapper<Hotel>()
                    .eq(Hotel::getNameCn, "级联删房型测试"));
            Long hotelId = created.getId();

            // 手动插入一个房型 + 图片 + 设施（因为 saveRoom 目前也没有级联保存）
            Room room = new Room();
            room.setHotelId(hotelId);
            room.setName("测试房型");
            room.setPrice(new BigDecimal("299.00"));
            room.setStatus(1);
            roomMapper.insert(room);

            RoomImage roomImg = new RoomImage();
            roomImg.setRoomId(room.getId());
            roomImg.setUrl("/images/rooms/test/01.jpg");
            roomImageMapper.insert(roomImg);

            RoomFacility roomFac = new RoomFacility();
            roomFac.setRoomId(room.getId());
            roomFac.setName("空调");
            roomFacilityMapper.insert(roomFac);

            // 删除酒店
            mockMvc.perform(delete("/api/resource/hotels/" + hotelId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            // 验证房型已删除
            Long roomCount = roomMapper.selectCount(
                    new LambdaQueryWrapper<Room>().eq(Room::getHotelId, hotelId));
            assert roomCount == 0 : "房型应该被级联删除，实际剩余：" + roomCount;

            // 验证房型图片/设施已删除
            Long riCount = roomImageMapper.selectCount(
                    new LambdaQueryWrapper<RoomImage>().eq(RoomImage::getRoomId, room.getId()));
            assert riCount == 0 : "房型图片应该被级联删除，实际剩余：" + riCount;

            Long rfCount = roomFacilityMapper.selectCount(
                    new LambdaQueryWrapper<RoomFacility>().eq(RoomFacility::getRoomId, room.getId()));
            assert rfCount == 0 : "房型设施应该被级联删除，实际剩余：" + rfCount;
        }

        @Test
        @DisplayName("未登录删除酒店 — 返回403")
        void shouldReturn403WithoutAuth() throws Exception {
            mockMvc.perform(delete("/api/resource/hotels/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== 房型删除 ====================

    @Nested
    @DisplayName("DELETE /api/resource/rooms/{id} — 房型删除")
    class RoomDelete {

        private Long testHotelId;
        private Long testRoomId;

        @BeforeEach
        void setup() {
            // 创建酒店（作为房型的父记录）
            Hotel hotel = new Hotel();
            hotel.setCityId(1L);
            hotel.setNameCn("房型测试酒店");
            hotel.setStarLevel(3);
            hotel.setStatus(1);
            hotelMapper.insert(hotel);
            testHotelId = hotel.getId();

            // 创建房型
            Room room = new Room();
            room.setHotelId(testHotelId);
            room.setName("待删除房型");
            room.setPrice(new BigDecimal("299.00"));
            room.setStatus(1);
            roomMapper.insert(room);
            testRoomId = room.getId();
        }

        @AfterEach
        void tearDown() {
            // 清理酒店（级联清理所有房型）
            deleteHotelCascade(testHotelId);
        }

        @Test
        @DisplayName("删除房型 — 房型图片同时删除")
        void shouldCascadeDeleteRoomImages() throws Exception {
            RoomImage img = new RoomImage();
            img.setRoomId(testRoomId);
            img.setUrl("/images/rooms/test/01.jpg");
            roomImageMapper.insert(img);

            mockMvc.perform(delete("/api/resource/rooms/" + testRoomId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            Long count = roomImageMapper.selectCount(
                    new LambdaQueryWrapper<RoomImage>().eq(RoomImage::getRoomId, testRoomId));
            assert count == 0 : "房型图片应该被级联删除，实际剩余：" + count;
        }

        @Test
        @DisplayName("删除房型 — 房型设施同时删除")
        void shouldCascadeDeleteRoomFacilities() throws Exception {
            RoomFacility fac = new RoomFacility();
            fac.setRoomId(testRoomId);
            fac.setName("空调");
            roomFacilityMapper.insert(fac);

            mockMvc.perform(delete("/api/resource/rooms/" + testRoomId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            Long count = roomFacilityMapper.selectCount(
                    new LambdaQueryWrapper<RoomFacility>().eq(RoomFacility::getRoomId, testRoomId));
            assert count == 0 : "房型设施应该被级联删除，实际剩余：" + count;
        }

        @Test
        @DisplayName("删除房型 — 房型记录消失")
        void shouldDeleteRoomRecord() throws Exception {
            mockMvc.perform(delete("/api/resource/rooms/" + testRoomId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            Room deleted = roomMapper.selectById(testRoomId);
            assert deleted == null : "房型记录应该被删除";
        }

        @Test
        @DisplayName("未登录删除房型 — 返回403")
        void shouldReturn403WithoutAuth() throws Exception {
            mockMvc.perform(delete("/api/resource/rooms/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== 国家删除 ====================

    @Nested
    @DisplayName("DELETE /api/resource/countries/{id} — 国家删除")
    class CountryDelete {

        private Long emptyCountryId;

        @BeforeEach
        void setup() {
            Country c = new Country();
            c.setCode("ZZ");
            c.setNameCn("空国家测试");
            c.setNameEn("Empty Test Country");
            countryMapper.insert(c);
            emptyCountryId = c.getId();
        }

        @AfterEach
        void tearDown() {
            countryMapper.deleteById(emptyCountryId);
        }

        @Test
        @DisplayName("删除有城市的国家 — 返回400错误")
        void shouldFailWhenCountryHasCities() throws Exception {
            // 国家1（中国）有多个城市，不能直接删除
            mockMvc.perform(delete("/api/resource/countries/1")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("删除无城市的国家 — 成功")
        void shouldDeleteEmptyCountry() throws Exception {
            mockMvc.perform(delete("/api/resource/countries/" + emptyCountryId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            Country deleted = countryMapper.selectById(emptyCountryId);
            assert deleted == null : "国家记录应该被删除";
        }

        @Test
        @DisplayName("未登录删除国家 — 返回403")
        void shouldReturn403WithoutAuth() throws Exception {
            mockMvc.perform(delete("/api/resource/countries/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== 城市删除 ====================

    @Nested
    @DisplayName("DELETE /api/resource/cities/{id} — 城市删除")
    class CityDelete {

        private Long emptyCityId;

        @BeforeEach
        void setup() {
            City c = new City();
            c.setCountryId(1L);
            c.setNameCn("空城市测试");
            c.setCode("EMPTY");
            c.setHot(0);
            cityMapper.insert(c);
            emptyCityId = c.getId();
        }

        @AfterEach
        void tearDown() {
            cityMapper.deleteById(emptyCityId);
        }

        @Test
        @DisplayName("删除有酒店的城市 — 返回400错误")
        void shouldFailWhenCityHasHotels() throws Exception {
            // 城市1（北京）有酒店，不能直接删除
            mockMvc.perform(delete("/api/resource/cities/1")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("删除无酒店的城市 — 成功")
        void shouldDeleteEmptyCity() throws Exception {
            mockMvc.perform(delete("/api/resource/cities/" + emptyCityId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            City deleted = cityMapper.selectById(emptyCityId);
            assert deleted == null : "城市记录应该被删除";
        }

        @Test
        @DisplayName("未登录删除城市 — 返回403")
        void shouldReturn403WithoutAuth() throws Exception {
            mockMvc.perform(delete("/api/resource/cities/1"))
                    .andExpect(status().isForbidden());
        }
    }
}
