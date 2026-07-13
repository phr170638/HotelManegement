package com.hotel.module.search.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.result.PageResult;
import com.hotel.module.resource.entity.Hotel;
import com.hotel.module.resource.mapper.HotelMapper;
import com.hotel.module.search.dto.HotelSearchRequest;
import com.hotel.module.search.service.SearchService;
import com.hotel.module.search.vo.HotelSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final HotelMapper hotelMapper;

    @Override
    public PageResult<HotelSearchVO> search(HotelSearchRequest req) {
        Page<Hotel> page = new Page<>(req.getPage(), req.getSize());
        Page<Hotel> result = hotelMapper.searchHotels(page, req);

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
}
