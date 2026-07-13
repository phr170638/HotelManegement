package com.hotel.module.resource.controller;

import com.hotel.common.result.R;
import com.hotel.module.resource.entity.Breakfast;
import com.hotel.module.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "资源管理-早餐")
@RestController
@RequestMapping("/api/resource/breakfasts")
@RequiredArgsConstructor
public class BreakfastController {

    private final ResourceService resourceService;

    @Operation(summary = "早餐列表")
    @GetMapping
    public R<List<Breakfast>> list() {
        return R.ok(resourceService.listBreakfasts());
    }
}
