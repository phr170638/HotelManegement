package com.hotel.module.resource.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.result.PageResult;
import com.hotel.module.review.mapper.ReviewMapper;
import com.hotel.module.resource.dto.HotelSaveRequest;
import com.hotel.module.resource.dto.RoomSaveRequest;
import com.hotel.module.resource.entity.*;
import com.hotel.module.resource.mapper.*;
import com.hotel.module.resource.service.ResourceService;
import com.hotel.module.resource.vo.HotelVO;
import com.hotel.module.resource.vo.RoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final CountryMapper countryMapper;
    private final CityMapper cityMapper;
    private final HotelMapper hotelMapper;
    private final HotelImageMapper hotelImageMapper;
    private final HotelFacilityMapper hotelFacilityMapper;
    private final RoomMapper roomMapper;
    private final RoomImageMapper roomImageMapper;
    private final RoomFacilityMapper roomFacilityMapper;
    private final BedTypeMapper bedTypeMapper;
    private final BreakfastMapper breakfastMapper;
    private final ReviewMapper reviewMapper;

    // ========== 国家 ==========

    @Override
    public PageResult<Country> listCountries(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<Country> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Country::getNameCn, keyword).or().like(Country::getNameEn, keyword);
        }
        IPage<Country> result = countryMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result);
    }

    @Override
    public Country getCountry(Long id) {
        return countryMapper.selectById(id);
    }

    @Override
    public void saveCountry(Country country) {
        countryMapper.insert(country);
    }

    @Override
    public void updateCountry(Country country) {
        countryMapper.updateById(country);
    }

    @Override
    public void deleteCountry(Long id) {
        Long cityCount = cityMapper.selectCount(new LambdaQueryWrapper<City>().eq(City::getCountryId, id));
        if (cityCount != null && cityCount > 0) {
            throw new BusinessException(400, "该国家下仍有关联城市，无法删除");
        }
        countryMapper.deleteById(id);
    }

    // ========== 城市 ==========

    @Override
    public PageResult<City> listCities(Integer page, Integer size, Long countryId, String keyword) {
        LambdaQueryWrapper<City> wrapper = new LambdaQueryWrapper<>();
        if (countryId != null) wrapper.eq(City::getCountryId, countryId);
        if (StringUtils.hasText(keyword)) wrapper.like(City::getNameCn, keyword);
        IPage<City> result = cityMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result);
    }

    @Override
    public List<City> listHotCities() {
        return cityMapper.selectHotCities();
    }

    @Override
    public void saveCity(City city) { cityMapper.insert(city); }

    @Override
    public void updateCity(City city) { cityMapper.updateById(city); }

    @Override
    public void deleteCity(Long id) {
        Long hotelCount = hotelMapper.selectCount(new LambdaQueryWrapper<Hotel>().eq(Hotel::getCityId, id));
        if (hotelCount != null && hotelCount > 0) {
            throw new BusinessException(400, "该城市下仍有关联酒店，无法删除");
        }
        cityMapper.deleteById(id);
    }

    // ========== 酒店 ==========

    @Override
    public PageResult<HotelVO> listHotels(Integer page, Integer size, Long cityId, String keyword, Integer starLevel) {
        LambdaQueryWrapper<Hotel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Hotel::getStatus, 1);
        if (cityId != null) wrapper.eq(Hotel::getCityId, cityId);
        if (starLevel != null) wrapper.eq(Hotel::getStarLevel, starLevel);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(Hotel::getNameCn, keyword)
                    .or()
                    .like(Hotel::getNameEn, keyword)
                    .or()
                    .like(Hotel::getBrand, keyword)
                    .or()
                    .like(Hotel::getAddress, keyword));
        }
        wrapper.orderByDesc(Hotel::getScore);

        IPage<Hotel> result = hotelMapper.selectPage(new Page<>(page, size), wrapper);
        List<HotelVO> records = result.getRecords().stream().map(h -> {
            HotelVO vo = new HotelVO();
            BeanUtil.copyProperties(h, vo);
            enrichHotelSummary(vo, h);
            return vo;
        }).toList();
        return new PageResult<>(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public HotelVO getHotelDetail(Long id) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null) throw new BusinessException(404, "酒店不存在");

        HotelVO vo = new HotelVO();
        BeanUtil.copyProperties(hotel, vo);

        // 城市名
        City city = cityMapper.selectById(hotel.getCityId());
        if (city != null) vo.setCityName(city.getNameCn());

        // 图片
        List<HotelImage> images = hotelImageMapper.selectList(
                new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, id).orderByAsc(HotelImage::getSortOrder));
        vo.setImages(images.stream().map(i -> {
            HotelVO.ImageVO iv = new HotelVO.ImageVO();
            BeanUtil.copyProperties(i, iv);
            return iv;
        }).toList());

        // 设施
        List<HotelFacility> facilities = hotelFacilityMapper.selectList(
                new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, id));
        vo.setFacilities(facilities.stream().map(HotelFacility::getName).toList());

        // 房型
        vo.setRooms(listRoomsByHotelId(id));
        applyReviewSummary(vo, hotel);

        return vo;
    }

    @Override
    @Transactional
    public void saveHotel(HotelSaveRequest req) {
        Hotel hotel = new Hotel();
        BeanUtil.copyProperties(req, hotel);
        normalizeHotelFields(hotel);
        hotel.setStatus(1);
        hotel.setScore(java.math.BigDecimal.ZERO);
        hotelMapper.insert(hotel);

        // 保存图片
        if (req.getImageUrls() != null) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                HotelImage img = new HotelImage();
                img.setHotelId(hotel.getId());
                img.setUrl(req.getImageUrls().get(i));
                img.setType(i == 0 ? 1 : 4);
                img.setSortOrder(i);
                hotelImageMapper.insert(img);
            }
        }

        // 保存设施
        if (req.getFacilities() != null) {
            for (String name : req.getFacilities()) {
                HotelFacility facility = new HotelFacility();
                facility.setHotelId(hotel.getId());
                facility.setName(name);
                hotelFacilityMapper.insert(facility);
            }
        }
    }

    @Override
    @Transactional
    public void updateHotel(Long id, HotelSaveRequest req) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null) throw new BusinessException("酒店不存在");
        BeanUtil.copyProperties(req, hotel, "id");
        normalizeHotelFields(hotel);
        hotelMapper.updateById(hotel);

        if (req.getImageUrls() != null) {
            hotelImageMapper.delete(new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, id));
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                HotelImage img = new HotelImage();
                img.setHotelId(id);
                img.setUrl(req.getImageUrls().get(i));
                img.setType(i == 0 ? 1 : 4);
                img.setSortOrder(i);
                hotelImageMapper.insert(img);
            }
        }

        if (req.getFacilities() != null) {
            hotelFacilityMapper.delete(new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, id));
            for (String name : req.getFacilities()) {
                HotelFacility facility = new HotelFacility();
                facility.setHotelId(id);
                facility.setName(name);
                hotelFacilityMapper.insert(facility);
            }
        }
    }

    @Override
    @Transactional
    public void deleteHotel(Long id) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getHotelId, id));
        for (Room room : rooms) {
            roomImageMapper.delete(new LambdaQueryWrapper<RoomImage>().eq(RoomImage::getRoomId, room.getId()));
            roomFacilityMapper.delete(new LambdaQueryWrapper<RoomFacility>().eq(RoomFacility::getRoomId, room.getId()));
        }
        roomMapper.delete(new LambdaQueryWrapper<Room>().eq(Room::getHotelId, id));
        hotelImageMapper.delete(new LambdaQueryWrapper<HotelImage>().eq(HotelImage::getHotelId, id));
        hotelFacilityMapper.delete(new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, id));
        reviewMapper.delete(new LambdaQueryWrapper<com.hotel.module.review.entity.Review>().eq(com.hotel.module.review.entity.Review::getHotelId, id));
        hotelMapper.deleteById(id);
    }

    private void enrichHotelSummary(HotelVO vo, Hotel hotel) {
        City city = cityMapper.selectById(hotel.getCityId());
        if (city != null) {
            vo.setCityName(city.getNameCn());
        }

        List<HotelImage> images = hotelImageMapper.selectList(
                new LambdaQueryWrapper<HotelImage>()
                        .eq(HotelImage::getHotelId, hotel.getId())
                        .orderByAsc(HotelImage::getSortOrder)
        );
        if (!images.isEmpty()) {
            vo.setMainImage(images.get(0).getUrl());
        }

        List<HotelFacility> facilities = hotelFacilityMapper.selectList(
                new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelId, hotel.getId())
        );
        vo.setFacilities(facilities.stream().map(HotelFacility::getName).limit(4).toList());

        List<Room> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>()
                        .eq(Room::getHotelId, hotel.getId())
                        .eq(Room::getStatus, 1)
        );
        Optional<java.math.BigDecimal> minPrice = rooms.stream()
                .map(Room::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(java.math.BigDecimal::compareTo);
        vo.setMinPrice(minPrice.orElse(null));
        applyReviewSummary(vo, hotel);
    }

    private void applyReviewSummary(HotelVO vo, Hotel hotel) {
        Integer reviewCount = reviewMapper.countByHotelId(hotel.getId());
        vo.setReviewCount(reviewCount == null ? 0 : reviewCount);

        java.math.BigDecimal avgScore = reviewMapper.avgScoreByHotelId(hotel.getId());
        vo.setScore(avgScore != null ? avgScore : hotel.getScore());
    }

    private void normalizeHotelFields(Hotel hotel) {
        hotel.setNameCn(normalizeText(hotel.getNameCn()));
        hotel.setNameEn(normalizeText(hotel.getNameEn()));
        hotel.setAddress(normalizeText(hotel.getAddress()));
        hotel.setBrand(normalizeText(hotel.getBrand()));
        hotel.setDescription(normalizeText(hotel.getDescription()));
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    // ========== 房型 ==========

    @Override
    public List<RoomVO> listRoomsByHotelId(Long hotelId) {
        List<Room> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>().eq(Room::getHotelId, hotelId).eq(Room::getStatus, 1));
        return rooms.stream().map(r -> {
            RoomVO vo = new RoomVO();
            BeanUtil.copyProperties(r, vo, "cancelable");
            vo.setCancelable(r.getCancelable());

            BedType bt = bedTypeMapper.selectById(r.getBedTypeId());
            if (bt != null) vo.setBedType(bt.getName());

            Breakfast bf = breakfastMapper.selectById(r.getBreakfastId());
            if (bf != null) vo.setBreakfast(bf.getName());

            List<RoomImage> images = roomImageMapper.selectList(
                    new LambdaQueryWrapper<RoomImage>().eq(RoomImage::getRoomId, r.getId()));
            vo.setImages(images.stream().map(RoomImage::getUrl).toList());

            List<RoomFacility> facilities = roomFacilityMapper.selectList(
                    new LambdaQueryWrapper<RoomFacility>().eq(RoomFacility::getRoomId, r.getId()));
            vo.setFacilities(facilities.stream().map(RoomFacility::getName).toList());

            return vo;
        }).toList();
    }

    @Override
    public void saveRoom(RoomSaveRequest req) {
        Room room = new Room();
        BeanUtil.copyProperties(req, room);
        room.setStatus(1);
        roomMapper.insert(room);
    }

    @Override
    public void updateRoom(Long id, RoomSaveRequest req) {
        Room room = roomMapper.selectById(id);
        if (room == null) throw new BusinessException("房型不存在");
        BeanUtil.copyProperties(req, room, "id");
        roomMapper.updateById(room);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        roomImageMapper.delete(new LambdaQueryWrapper<RoomImage>().eq(RoomImage::getRoomId, id));
        roomFacilityMapper.delete(new LambdaQueryWrapper<RoomFacility>().eq(RoomFacility::getRoomId, id));
        roomMapper.deleteById(id);
    }

    // ========== 字典 ==========

    @Override
    public List<Breakfast> listBreakfasts() {
        return breakfastMapper.selectList(null);
    }

    @Override
    public List<BedType> listBedTypes() {
        return bedTypeMapper.selectList(null);
    }
}
