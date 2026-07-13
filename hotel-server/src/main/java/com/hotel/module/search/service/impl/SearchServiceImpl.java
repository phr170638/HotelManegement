package com.hotel.module.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.result.PageResult;
import com.hotel.module.resource.entity.Hotel;
import com.hotel.module.resource.entity.HotelFacility;
import com.hotel.module.resource.entity.HotelImage;
import com.hotel.module.resource.entity.Room;
import com.hotel.module.resource.mapper.HotelFacilityMapper;
import com.hotel.module.resource.mapper.HotelImageMapper;
import com.hotel.module.resource.mapper.HotelMapper;
import com.hotel.module.resource.mapper.RoomMapper;
import com.hotel.module.review.entity.Review;
import com.hotel.module.review.mapper.ReviewMapper;
import com.hotel.module.search.dto.HotelSearchRequest;
import com.hotel.module.search.service.SearchService;
import com.hotel.module.search.vo.HotelSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final HotelMapper hotelMapper;
    private final HotelImageMapper hotelImageMapper;
    private final HotelFacilityMapper hotelFacilityMapper;
    private final RoomMapper roomMapper;
    private final ReviewMapper reviewMapper;

    @Override
    public PageResult<HotelSearchVO> search(HotelSearchRequest req) {
        if (req.getSortBy() == null || req.getSortBy().isEmpty()) {
            req.setSortBy("score");
            req.setSortOrder("desc");
        }

        Page<Hotel> page = new Page<>(req.getPage(), req.getSize());
        Page<Hotel> result = hotelMapper.searchHotels(page, req);

        List<Long> hotelIds = result.getRecords().stream().map(Hotel::getId).toList();

        Map<Long, String> mainImageMap = loadMainImages(hotelIds);
        Map<Long, BigDecimal> minPriceMap = loadMinPrices(hotelIds);
        Map<Long, Long> reviewCountMap = loadReviewCounts(hotelIds);
        Map<Long, List<String>> facilitiesMap = loadFacilities(hotelIds);

        List<HotelSearchVO> records = result.getRecords().stream().map(h -> {
            HotelSearchVO vo = new HotelSearchVO();
            vo.setId(h.getId());
            vo.setNameCn(h.getNameCn());
            vo.setNameEn(h.getNameEn());
            vo.setStarLevel(h.getStarLevel());
            vo.setScore(h.getScore());
            vo.setAddress(h.getAddress());
            vo.setLongitude(h.getLongitude());
            vo.setLatitude(h.getLatitude());
            vo.setBrand(h.getBrand());
            vo.setMainImage(mainImageMap.get(h.getId()));
            vo.setMinPrice(minPriceMap.get(h.getId()));
            vo.setReviewCount(reviewCountMap.getOrDefault(h.getId(), 0L).intValue());
            vo.setFacilities(facilitiesMap.getOrDefault(h.getId(), Collections.emptyList()));
            return vo;
        }).toList();

        return new PageResult<>(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public Map<String, List<String>> suggest(String keyword, Long cityId) {
        Map<String, List<String>> resultMap = new HashMap<>();

        LambdaQueryWrapper<Hotel> wrapper = new LambdaQueryWrapper<Hotel>()
                .and(w -> w.like(Hotel::getNameCn, keyword).or().like(Hotel::getBrand, keyword))
                .eq(Hotel::getStatus, 1);
        if (cityId != null) wrapper.eq(Hotel::getCityId, cityId);
        wrapper.last("LIMIT 20");

        List<Hotel> hotels = hotelMapper.selectList(wrapper);

        List<String> hotelNames = hotels.stream()
                .map(Hotel::getNameCn)
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .toList();

        List<String> brands = hotels.stream()
                .map(Hotel::getBrand)
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .toList();

        resultMap.put("hotels", hotelNames);
        resultMap.put("brands", brands);
        resultMap.put("landmarks", Collections.emptyList());
        return resultMap;
    }

    @Override
    public List<HotelSearchVO> nearby(String longitude, String latitude, Integer radius) {
        double lng = Double.parseDouble(longitude);
        double lat = Double.parseDouble(latitude);

        List<Hotel> hotels = hotelMapper.selectList(
                new LambdaQueryWrapper<Hotel>()
                        .eq(Hotel::getStatus, 1)
                        .isNotNull(Hotel::getLatitude)
                        .isNotNull(Hotel::getLongitude));

        List<Hotel> nearbyHotels = hotels.stream()
                .filter(h -> haversine(lat, lng, h.getLatitude().doubleValue(), h.getLongitude().doubleValue()) <= radius)
                .sorted(Comparator.comparingDouble(h ->
                        haversine(lat, lng, h.getLatitude().doubleValue(), h.getLongitude().doubleValue())))
                .limit(50)
                .toList();

        if (nearbyHotels.isEmpty()) return Collections.emptyList();

        List<Long> hotelIds = nearbyHotels.stream().map(Hotel::getId).toList();

        Map<Long, String> mainImageMap = loadMainImages(hotelIds);
        Map<Long, BigDecimal> minPriceMap = loadMinPrices(hotelIds);
        Map<Long, Long> reviewCountMap = loadReviewCounts(hotelIds);
        Map<Long, List<String>> facilitiesMap = loadFacilities(hotelIds);

        return nearbyHotels.stream().map(h -> {
            HotelSearchVO vo = new HotelSearchVO();
            vo.setId(h.getId());
            vo.setNameCn(h.getNameCn());
            vo.setNameEn(h.getNameEn());
            vo.setStarLevel(h.getStarLevel());
            vo.setScore(h.getScore());
            vo.setAddress(h.getAddress());
            vo.setLongitude(h.getLongitude());
            vo.setLatitude(h.getLatitude());
            vo.setBrand(h.getBrand());
            vo.setMainImage(mainImageMap.get(h.getId()));
            vo.setMinPrice(minPriceMap.get(h.getId()));
            vo.setReviewCount(reviewCountMap.getOrDefault(h.getId(), 0L).intValue());
            vo.setFacilities(facilitiesMap.getOrDefault(h.getId(), Collections.emptyList()));
            return vo;
        }).toList();
    }

    private Map<Long, String> loadMainImages(List<Long> hotelIds) {
        if (hotelIds.isEmpty()) return Collections.emptyMap();
        Map<Long, String> map = new HashMap<>();
        List<HotelImage> images = hotelImageMapper.selectList(
                new LambdaQueryWrapper<HotelImage>()
                        .in(HotelImage::getHotelId, hotelIds)
                        .eq(HotelImage::getType, 1)
                        .orderByAsc(HotelImage::getSortOrder));
        for (HotelImage img : images) {
            map.putIfAbsent(img.getHotelId(), img.getUrl());
        }
        return map;
    }

    private Map<Long, BigDecimal> loadMinPrices(List<Long> hotelIds) {
        if (hotelIds.isEmpty()) return Collections.emptyMap();
        Map<Long, BigDecimal> map = new HashMap<>();
        List<Room> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>()
                        .in(Room::getHotelId, hotelIds)
                        .eq(Room::getStatus, 1));
        for (Room room : rooms) {
            map.merge(room.getHotelId(), room.getPrice(),
                    (existing, incoming) -> existing.compareTo(incoming) < 0 ? existing : incoming);
        }
        return map;
    }

    private Map<Long, Long> loadReviewCounts(List<Long> hotelIds) {
        if (hotelIds.isEmpty()) return Collections.emptyMap();
        Map<Long, Long> map = new HashMap<>();
        for (Long hotelId : hotelIds) {
            map.put(hotelId, reviewMapper.selectCount(
                    new LambdaQueryWrapper<Review>()
                            .eq(Review::getHotelId, hotelId)));
        }
        return map;
    }

    private Map<Long, List<String>> loadFacilities(List<Long> hotelIds) {
        if (hotelIds.isEmpty()) return Collections.emptyMap();
        List<HotelFacility> facilities = hotelFacilityMapper.selectList(
                new LambdaQueryWrapper<HotelFacility>()
                        .in(HotelFacility::getHotelId, hotelIds));
        return facilities.stream().collect(Collectors.groupingBy(
                HotelFacility::getHotelId,
                Collectors.mapping(HotelFacility::getName, Collectors.toList())));
    }

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
