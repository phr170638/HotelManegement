package com.hotel.module.resource.controller;

import com.hotel.common.result.PageResult;
import com.hotel.common.result.R;
import com.hotel.module.resource.entity.City;
import com.hotel.module.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "资源管理-城市")
@RestController
@RequestMapping("/api/resource/cities")
@RequiredArgsConstructor
public class CityController {

    private final ResourceService resourceService;

    @Operation(summary = "城市列表")
    @GetMapping
    public R<PageResult<City>> list(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "20") Integer size,
                                     @RequestParam(required = false) Long countryId,
                                     @RequestParam(required = false) String keyword) {
        return R.ok(resourceService.listCities(page, size, countryId, keyword));
    }

    @Operation(summary = "热门城市")
    @GetMapping("/hot")
    public R<List<City>> hot() {
        return R.ok(resourceService.listHotCities());
    }

    @Operation(summary = "新增城市")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> save(@RequestBody City city) {
        resourceService.saveCity(city);
        return R.ok();
    }

    @Operation(summary = "更新城市")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @RequestBody City city) {
        city.setId(id);
        resourceService.updateCity(city);
        return R.ok();
    }

    @Operation(summary = "删除城市")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        resourceService.deleteCity(id);
        return R.ok();
    }
}
