package com.hotel.module.search.service;

import com.hotel.common.result.PageResult;
import com.hotel.module.search.dto.HotelSearchRequest;
import com.hotel.module.search.vo.HotelSearchVO;

import java.util.List;
import java.util.Map;

public interface SearchService {

    PageResult<HotelSearchVO> search(HotelSearchRequest request);

    Map<String, List<String>> suggest(String keyword, Long cityId);

    List<HotelSearchVO> nearby(String longitude, String latitude, Integer radius);
}
