package com.hotel.module.search.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.result.PageResult;
import com.hotel.module.review.mapper.ReviewMapper;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ReviewMapper reviewMapper;

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
        BigDecimal targetLongitude = new BigDecimal(longitude);
        BigDecimal targetLatitude = new BigDecimal(latitude);
        int radiusKm = radius == null ? 5 : radius;

        return hotelMapper.selectList(new LambdaQueryWrapper<Hotel>().eq(Hotel::getStatus, 1)).stream()
                .filter(hotel -> hotel.getLongitude() != null && hotel.getLatitude() != null)
                .filter(hotel -> calculateDistanceKm(
                        targetLongitude.doubleValue(),
                        targetLatitude.doubleValue(),
                        hotel.getLongitude().doubleValue(),
                        hotel.getLatitude().doubleValue()
                ) <= radiusKm)
                .map(hotel -> {
                    HotelSearchVO vo = new HotelSearchVO();
                    vo.setId(hotel.getId());
                    vo.setCityId(hotel.getCityId());
                    vo.setNameCn(hotel.getNameCn());
                    vo.setNameEn(hotel.getNameEn());
                    vo.setStarLevel(hotel.getStarLevel());
                    vo.setScore(hotel.getScore());
                    vo.setAddress(hotel.getAddress());
                    vo.setLongitude(hotel.getLongitude());
                    vo.setLatitude(hotel.getLatitude());
                    vo.setBrand(hotel.getBrand());
                    enrichSearchSummary(vo, hotel);
                    return vo;
                })
                .toList();
    }

    private double calculateDistanceKm(double longitude1, double latitude1, double longitude2, double latitude2) {
        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(latitude2 - latitude1);
        double lonDistance = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadiusKm * c).setScale(3, RoundingMode.HALF_UP).doubleValue();
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

        Integer reviewCount = reviewMapper.countByHotelId(hotel.getId());
        vo.setReviewCount(reviewCount == null ? 0 : reviewCount);

        java.math.BigDecimal avgScore = reviewMapper.avgScoreByHotelId(hotel.getId());
        if (avgScore != null) {
            vo.setScore(avgScore);
        }
    }
}
