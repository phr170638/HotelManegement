package com.hotel.module.search.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.result.PageResult;
import com.hotel.module.resource.entity.City;
import com.hotel.module.resource.entity.Hotel;
import com.hotel.module.resource.entity.HotelFacility;
import com.hotel.module.resource.entity.HotelImage;
import com.hotel.module.resource.entity.Room;
import com.hotel.module.resource.mapper.CityMapper;
import com.hotel.module.resource.mapper.HotelFacilityMapper;
import com.hotel.module.resource.mapper.HotelImageMapper;
import com.hotel.module.resource.mapper.HotelMapper;
import com.hotel.module.resource.mapper.RoomMapper;
import com.hotel.module.search.dto.HotelSearchRequest;
import com.hotel.module.search.service.SearchService;
import com.hotel.module.search.vo.HotelSearchVO;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final HotelMapper hotelMapper;
    private final CityMapper cityMapper;
    private final HotelImageMapper hotelImageMapper;
    private final HotelFacilityMapper hotelFacilityMapper;
    private final RoomMapper roomMapper;

    @Override
    public PageResult<HotelSearchVO> search(HotelSearchRequest req) {
        Page<Hotel> page = new Page<>(req.getPage(), req.getSize());
        Page<Hotel> result = hotelMapper.searchHotels(page, req);

        List<HotelSearchVO> records = result.getRecords().stream().map(h -> {
            HotelSearchVO vo = new HotelSearchVO();
            vo.setId(h.getId());
            vo.setCityId(h.getCityId());
            vo.setNameCn(h.getNameCn());
            vo.setNameEn(h.getNameEn());
            vo.setStarLevel(h.getStarLevel());
            vo.setScore(h.getScore());
            vo.setAddress(h.getAddress());
            vo.setLongitude(h.getLongitude());
            vo.setLatitude(h.getLatitude());
            vo.setBrand(h.getBrand());
            enrichSearchSummary(vo, h);
            return vo;
        }).toList();

        return new PageResult<>(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public Map<String, List<String>> suggest(String keyword, Long cityId) {
        // TODO: 实现关键字联想查询
        Map<String, List<String>> result = new HashMap<>();
        result.put("hotels", Collections.emptyList());
        result.put("landmarks", Collections.emptyList());
        result.put("brands", Collections.emptyList());
        return result;
    }

    @Override
    public List<HotelSearchVO> nearby(String longitude, String latitude, Integer radius) {
        // TODO: 实现地理位置范围搜索（需集成高德/百度地图API）
        return Collections.emptyList();
    }

    private void enrichSearchSummary(HotelSearchVO vo, Hotel hotel) {
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
        vo.setReviewCount(0);
    }
}
