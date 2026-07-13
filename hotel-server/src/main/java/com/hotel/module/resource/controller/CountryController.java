package com.hotel.module.resource.controller;

import com.hotel.common.result.PageResult;
import com.hotel.common.result.R;
import com.hotel.module.resource.entity.Country;
import com.hotel.module.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "资源管理-国家")
@RestController
@RequestMapping("/api/resource/countries")
@RequiredArgsConstructor
public class CountryController {

    private final ResourceService resourceService;

    @Operation(summary = "国家列表")
    @GetMapping
    public R<PageResult<Country>> list(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "20") Integer size,
                                        @RequestParam(required = false) String keyword) {
        return R.ok(resourceService.listCountries(page, size, keyword));
    }

    @Operation(summary = "新增国家")
    @PostMapping
    public R<Void> save(@RequestBody Country country) {
        resourceService.saveCountry(country);
        return R.ok();
    }

    @Operation(summary = "更新国家")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Country country) {
        country.setId(id);
        resourceService.updateCountry(country);
        return R.ok();
    }

    @Operation(summary = "删除国家")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        resourceService.deleteCountry(id);
        return R.ok();
    }
}
