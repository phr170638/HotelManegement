package com.hotel.module.search.controller;

import com.hotel.common.result.PageResult;
import com.hotel.common.result.R;
import com.hotel.module.search.dto.HotelSearchRequest;
import com.hotel.module.search.service.SearchService;
import com.hotel.module.search.vo.HotelSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "酒店搜索", description = "酒店搜索、附近搜索、关键字联想")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "酒店搜索")
    @GetMapping("/hotels")
    public R<PageResult<HotelSearchVO>> search(HotelSearchRequest request) {
        return R.ok(searchService.search(request));
    }

    @Operation(summary = "附近搜索")
    @GetMapping("/nearby")
    public R<List<HotelSearchVO>> nearby(@RequestParam String longitude,
                                          @RequestParam String latitude,
                                          @RequestParam(defaultValue = "5") Integer radius) {
        return R.ok(searchService.nearby(longitude, latitude, radius));
    }

    @Operation(summary = "关键字联想")
    @GetMapping("/suggest")
    public R<Map<String, List<String>>> suggest(@RequestParam String keyword,
                                                 @RequestParam(required = false) Long cityId) {
        return R.ok(searchService.suggest(keyword, cityId));
    }
}
