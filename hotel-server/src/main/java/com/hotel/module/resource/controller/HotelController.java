package com.hotel.module.resource.controller;

import com.hotel.common.result.PageResult;
import com.hotel.common.result.R;
import com.hotel.module.resource.dto.HotelSaveRequest;
import com.hotel.module.resource.service.ResourceService;
import com.hotel.module.resource.vo.HotelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "资源管理-酒店")
@RestController
@RequestMapping("/api/resource/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final ResourceService resourceService;

    @Operation(summary = "酒店列表")
    @GetMapping
    public R<PageResult<HotelVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "20") Integer size,
                                        @RequestParam(required = false) Long cityId,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer starLevel) {
        return R.ok(resourceService.listHotels(page, size, cityId, keyword, starLevel));
    }

    @Operation(summary = "酒店详情")
    @GetMapping("/{id}")
    public R<HotelVO> detail(@PathVariable Long id) {
        return R.ok(resourceService.getHotelDetail(id));
    }

    @Operation(summary = "新增酒店")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> save(@Valid @RequestBody HotelSaveRequest request) {
        resourceService.saveHotel(request);
        return R.ok();
    }

    @Operation(summary = "更新酒店")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody HotelSaveRequest request) {
        resourceService.updateHotel(id, request);
        return R.ok();
    }

    @Operation(summary = "删除酒店")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        resourceService.deleteHotel(id);
        return R.ok();
    }
}
