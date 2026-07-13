package com.hotel.module.resource.controller;

import com.hotel.common.result.R;
import com.hotel.module.resource.entity.BedType;
import com.hotel.module.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "资源管理-床型")
@RestController
@RequestMapping("/api/resource/bed-types")
@RequiredArgsConstructor
public class BedTypeController {

    private final ResourceService resourceService;

    @Operation(summary = "床型列表")
    @GetMapping
    public R<List<BedType>> list() {
        return R.ok(resourceService.listBedTypes());
    }
}
