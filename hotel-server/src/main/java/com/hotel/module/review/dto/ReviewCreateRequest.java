package com.hotel.module.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReviewCreateRequest {
    @NotNull(message = "酒店ID不能为空")
    private Long hotelId;

    private Long orderId;

    @Min(1) @Max(5)
    @NotNull(message = "评分不能为空")
    private Integer score;

    @NotNull(message = "评价内容不能为空")
    private String content;

    private List<String> images;
}
