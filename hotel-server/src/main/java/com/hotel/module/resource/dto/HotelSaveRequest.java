package com.hotel.module.resource.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HotelSaveRequest {
    @NotNull(message = "城市ID不能为空")
    private Long cityId;

    private String nameCn;

    private String nameEn;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer starLevel;
    private String brand;
    private String description;
    private List<String> imageUrls;
    private List<String> facilities;

    @AssertTrue(message = "涓枃鍚嶅拰鑻辨枃鍚嶈嚦灏戝～鍐欎竴涓?")
    public boolean isHotelNameProvided() {
        return StringUtils.hasText(nameCn) || StringUtils.hasText(nameEn);
    }
}
