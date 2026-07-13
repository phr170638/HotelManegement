package com.hotel.module.order.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import com.hotel.common.result.PageResult;
import com.hotel.module.order.dto.OrderCreateRequest;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.entity.OrderItem;
import com.hotel.module.order.mapper.OrderItemMapper;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.order.service.OrderService;
import com.hotel.module.order.vo.OrderVO;
import com.hotel.module.payment.entity.Payment;
import com.hotel.module.payment.mapper.PaymentMapper;
import com.hotel.module.payment.service.AlipayService;
import com.hotel.module.resource.entity.Hotel;
import com.hotel.module.resource.entity.Room;
import com.hotel.module.resource.mapper.HotelMapper;
import com.hotel.module.resource.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final HotelMapper hotelMapper;
    private final RoomMapper roomMapper;
    private final AlipayService alipayService;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public OrderVO create(Long userId, OrderCreateRequest req) {
        Hotel hotel = hotelMapper.selectById(req.getHotelId());
        if (hotel == null) throw new BusinessException("酒店不存在");

        // 生成订单号
        String orderNo = DateUtil.format(new Date(), "yyyyMMddHHmmss") + String.format("%06d", new Random().nextInt(999999));

        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setHotelId(req.getHotelId());
        order.setCheckInDate(req.getCheckInDate());
        order.setCheckOutDate(req.getCheckOutDate());
        order.setRoomCount(req.getRoomCount());
        order.setGuestName(req.getGuestName());
        order.setGuestPhone(req.getGuestPhone());
        order.setStatus(0); // 待支付
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 保存订单明细
        for (OrderCreateRequest.OrderItemRequest itemReq : req.getItems()) {
            Room room = roomMapper.selectById(itemReq.getRoomId());
            if (room == null) throw new BusinessException("房型不存在");

            BigDecimal subtotal = room.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setRoomId(itemReq.getRoomId());
            item.setRoomName(room.getName());
            item.setPrice(room.getPrice());
            item.setQuantity(itemReq.getQuantity());
            item.setSubtotal(subtotal);
            orderItemMapper.insert(item);
        }

        order.setTotalAmount(totalAmount);
        orderMapper.updateById(order);

        return buildOrderVO(order);
    }

    @Override
    public PageResult<OrderVO> listByUser(Long userId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) wrapper.eq(Order::getStatus, status);
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        List<OrderVO> records = result.getRecords().stream().map(this::buildOrderVO).toList();
        return new PageResult<>(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public OrderVO detail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        return buildOrderVO(order);
    }

    @Override
    public void cancel(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException("无权操作此订单");
        if (order.getStatus() != 0) throw new BusinessException("仅可取消待支付订单");
        order.setStatus(2); // 已取消
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public Map<String, Object> preCancel(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException("无权操作此订单");

        // 计算退房手续费
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInTime = order.getCheckInDate().atTime(14, 0); // 入住日14:00
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(now, checkInTime);

        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal penaltyRate;
        String penaltyDesc;

        if (hoursUntilCheckIn <= 0) {
            // 已过入住时间
            penaltyRate = BigDecimal.ONE;
            penaltyDesc = "已超过入住时间，不退款";
        } else if (hoursUntilCheckIn <= 24) {
            penaltyRate = BigDecimal.ONE;
            penaltyDesc = "距入住不足24小时，不退款";
        } else if (hoursUntilCheckIn <= 72) {
            penaltyRate = new BigDecimal("0.8");
            penaltyDesc = "距入住24小时~3天，扣除80%手续费";
        } else if (hoursUntilCheckIn <= 168) {
            penaltyRate = new BigDecimal("0.5");
            penaltyDesc = "距入住3~7天，扣除50%手续费";
        } else {
            penaltyRate = BigDecimal.ZERO;
            penaltyDesc = "距入住7天以上，免手续费";
        }

        BigDecimal cancelPenalty = totalAmount.multiply(penaltyRate).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal refundAmount = totalAmount.subtract(cancelPenalty);

        String cancelId = "CANCEL-" + UUID.randomUUID().toString().substring(0, 8);
        order.setCancelConfirmId(cancelId);
        order.setStatus(4); // 退房申请中
        order.setCancelAmount(cancelPenalty);
        orderMapper.updateById(order);

        Map<String, Object> result = new HashMap<>();
        result.put("cancelConfirmId", cancelId);
        result.put("originalAmount", totalAmount);
        result.put("hoursUntilCheckIn", hoursUntilCheckIn);
        result.put("penaltyRate", penaltyRate);
        result.put("penaltyDesc", penaltyDesc);
        result.put("cancelPenalty", cancelPenalty);
        result.put("refundAmount", refundAmount);
        return result;
    }

    @Override
    public void confirmCancel(Long userId, Long orderId, String cancelConfirmId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException("无权操作此订单");
        if (!cancelConfirmId.equals(order.getCancelConfirmId())) throw new BusinessException("取消确认ID不匹配");
        order.setStatus(5); // 已退房
        orderMapper.updateById(order);
    }

    private OrderVO buildOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setHotelId(order.getHotelId());
        vo.setCheckInDate(order.getCheckInDate());
        vo.setCheckOutDate(order.getCheckOutDate());
        vo.setRoomCount(order.getRoomCount());
        vo.setGuestName(order.getGuestName());
        vo.setGuestPhone(order.getGuestPhone());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(getStatusText(order.getStatus()));
        vo.setPayTime(order.getPayTime());
        vo.setCreateTime(order.getCreateTime());

        Hotel hotel = hotelMapper.selectById(order.getHotelId());
        if (hotel != null) {
            vo.setHotelName(hotel.getNameCn());
            vo.setHotelAddress(hotel.getAddress());
            vo.setHotelLongitude(hotel.getLongitude());
            vo.setHotelLatitude(hotel.getLatitude());
            vo.setHotelStarLevel(hotel.getStarLevel());
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        if (items != null) {
            vo.setItems(items.stream().map(i -> {
                OrderVO.OrderItemVO iv = new OrderVO.OrderItemVO();
                iv.setId(i.getId());
                iv.setRoomName(i.getRoomName());
                iv.setPrice(i.getPrice());
                iv.setQuantity(i.getQuantity());
                iv.setSubtotal(i.getSubtotal());
                return iv;
            }).toList());
        }

        return vo;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已入住";
            case 4 -> "退房申请中";
            case 5 -> "已退房";
            case 6 -> "已完成";
            default -> "未知";
        };
    }

    @Override
    public String getPayForm(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException("无权操作此订单");
        if (order.getStatus() != 0) throw new BusinessException("仅待支付订单可发起支付");

        Hotel hotel = hotelMapper.selectById(order.getHotelId());
        String subject = hotel != null ? hotel.getNameCn() + " — 酒店预订" : "酒店预订";

        return alipayService.pagePay(order.getOrderNo(),
                order.getTotalAmount().toString(), subject);
    }

    @Override
    @Transactional
    public void handlePayNotify(Map<String, String> params) {
        // 1. 验证签名
        if (!alipayService.verifySignature(params)) {
            log.error("支付宝回调签名验证失败");
            throw new BusinessException("签名验证失败");
        }

        // 2. 只处理交易成功
        if (!alipayService.isTradeSuccess(params)) {
            log.info("支付宝回调非交易成功状态: {}", params.get("trade_status"));
            return;
        }

        String orderNo = alipayService.extractOrderNo(params);
        String tradeNo = alipayService.extractTradeNo(params);
        String amount = alipayService.extractAmount(params);

        // 3. 查找订单
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            log.error("支付宝回调订单不存在: {}", orderNo);
            throw new BusinessException("订单不存在");
        }

        // 4. 防止重复通知
        if (order.getStatus() != 0) {
            log.info("订单已处理，忽略重复回调: {}", orderNo);
            return;
        }

        // 5. 更新订单状态
        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 6. 记录支付流水
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setTradeNo(tradeNo);
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(1); // 成功
        payment.setPayMethod("ALIPAY");
        payment.setPayTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        log.info("支付成功: orderNo={}, tradeNo={}, amount={}", orderNo, tradeNo, amount);
    }
}
