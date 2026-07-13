package com.hotel.module.order.controller;

import com.hotel.common.result.PageResult;
import com.hotel.common.result.R;
import com.hotel.module.order.dto.OrderCreateRequest;
import com.hotel.module.order.service.OrderService;
import com.hotel.module.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单模块", description = "订单创建、支付、取消、退房")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public R<OrderVO> create(@AuthenticationPrincipal Long userId,
                              @Valid @RequestBody OrderCreateRequest request) {
        return R.ok(orderService.create(userId, request));
    }

    @Operation(summary = "发起支付（返回支付宝收银台HTML表单）")
    @PostMapping("/{id}/pay")
    public R<String> pay(@AuthenticationPrincipal Long userId,
                           @PathVariable Long id) {
        String form = orderService.getPayForm(userId, id);
        return R.ok(form);
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        orderService.cancel(userId, id);
        return R.okMsg("订单已取消");
    }

    @Operation(summary = "订单预取消（退房申请第一步）")
    @PostMapping("/{id}/pre-cancel")
    public R<Map<String, Object>> preCancel(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return R.ok(orderService.preCancel(userId, id));
    }

    @Operation(summary = "确认取消（退房申请第二步）")
    @PostMapping("/{id}/confirm-cancel")
    public R<Void> confirmCancel(@AuthenticationPrincipal Long userId,
                                  @PathVariable Long id,
                                  @RequestParam String cancelConfirmId) {
        orderService.confirmCancel(userId, id, cancelConfirmId);
        return R.okMsg("退房申请已确认");
    }

    @Operation(summary = "支付回调（支付宝异步通知）")
    @PostMapping("/pay-notify")
    public String payNotify(@RequestParam Map<String, String> params) {
        orderService.handlePayNotify(params);
        return "success";
    }
}
