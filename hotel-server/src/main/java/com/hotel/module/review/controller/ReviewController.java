package com.hotel.module.review.controller;

import com.hotel.common.result.PageResult;
import com.hotel.common.result.R;
import com.hotel.module.review.dto.ReviewCreateRequest;
import com.hotel.module.review.service.ReviewService;
import com.hotel.module.review.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "评价模块", description = "酒店评价、商家回复")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "酒店评价列表")
    @GetMapping("/hotel/{hotelId}")
    public R<PageResult<ReviewVO>> list(@PathVariable Long hotelId,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) Integer score) {
        return R.ok(reviewService.listByHotel(hotelId, page, size, score));
    }

    @Operation(summary = "发表评价")
    @PostMapping("/create")
    public R<Void> create(@AuthenticationPrincipal Long userId,
                           @Valid @RequestBody ReviewCreateRequest request) {
        reviewService.create(userId, request);
        return R.ok();
    }

    @Operation(summary = "商家回复（管理端）")
    @PutMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewService.reply(id, body.get("reply"));
        return R.ok();
    }
}
