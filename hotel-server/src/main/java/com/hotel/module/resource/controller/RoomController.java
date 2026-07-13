package com.hotel.module.resource.controller;

import com.hotel.common.result.R;
import com.hotel.module.resource.dto.RoomSaveRequest;
import com.hotel.module.resource.service.ResourceService;
import com.hotel.module.resource.vo.RoomVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "资源管理-房型")
@RestController
@RequestMapping("/api/resource/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final ResourceService resourceService;

    @Operation(summary = "某酒店下的房型列表")
    @GetMapping
    public R<List<RoomVO>> list(@RequestParam Long hotelId) {
        return R.ok(resourceService.listRoomsByHotelId(hotelId));
    }

    @Operation(summary = "新增房型")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> save(@Valid @RequestBody RoomSaveRequest request) {
        resourceService.saveRoom(request);
        return R.ok();
    }

    @Operation(summary = "更新房型")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoomSaveRequest request) {
        resourceService.updateRoom(id, request);
        return R.ok();
    }

    @Operation(summary = "删除房型")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        resourceService.deleteRoom(id);
        return R.ok();
    }
}
