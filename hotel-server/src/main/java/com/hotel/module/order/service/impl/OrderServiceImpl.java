package com.hotel.module.order.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.result.PageResult;
import com.hotel.module.order.dto.OrderCreateRequest;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.entity.OrderItem;
import com.hotel.module.order.mapper.OrderItemMapper;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.order.service.OrderService;
import com.hotel.module.order.vo.OrderVO;
import com.hotel.module.resource.entity.Hotel;
import com.hotel.module.resource.entity.Room;
import com.hotel.module.resource.mapper.HotelMapper;
import com.hotel.module.resource.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final HotelMapper hotelMapper;
    private final RoomMapper roomMapper;

    @Override
    @Transactional
    public OrderVO create(Long userId, OrderCreateRequest req) {
        Hotel hotel = hotelMapper.selectById(req.getHotelId());
        if (hotel == null) throw new BusinessException("酒店不存在");
        if (req.getCheckInDate() == null || req.getCheckOutDate() == null) {
            throw new BusinessException("入住和退房日期不能为空");
        }
        if (req.getCheckInDate().isBefore(LocalDate.now())) {
            throw new BusinessException("入住日期不能早于今天");
        }
        if (!req.getCheckOutDate().isAfter(req.getCheckInDate())) {
            throw new BusinessException("退房日期必须晚于入住日期");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException("订单明细不能为空");
        }

        // 生成订单号
        String orderNo = DateUtil.format(new Date(), "yyyyMMddHHmmss") + String.format("%06d", new Random().nextInt(999999));
        long stayNights = ChronoUnit.DAYS.between(req.getCheckInDate(), req.getCheckOutDate());

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalRoomCount = 0;
        List<PendingOrderItem> pendingItems = new ArrayList<>();

        for (OrderCreateRequest.OrderItemRequest itemReq : req.getItems()) {
            Room room = roomMapper.selectById(itemReq.getRoomId());
            if (room == null) throw new BusinessException("房型不存在");
            if (!room.getHotelId().equals(req.getHotelId())) throw new BusinessException("所选房型不属于当前酒店");
            if (!Objects.equals(room.getStatus(), 1)) throw new BusinessException("所选房型当前不可预订");

            int quantity = itemReq.getQuantity() == null ? 1 : itemReq.getQuantity();
            if (quantity <= 0) throw new BusinessException("房间数量必须大于 0");

            BigDecimal subtotal = room.getPrice()
                    .multiply(BigDecimal.valueOf(quantity))
                    .multiply(BigDecimal.valueOf(stayNights));
            totalAmount = totalAmount.add(subtotal);
            totalRoomCount += quantity;
            pendingItems.add(new PendingOrderItem(room.getId(), room.getName(), room.getPrice(), quantity, subtotal));
        }

        if (totalRoomCount <= 0) {
            throw new BusinessException("订单房间数量不能为空");
        }

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setHotelId(req.getHotelId());
        order.setCheckInDate(req.getCheckInDate());
        order.setCheckOutDate(req.getCheckOutDate());
        order.setRoomCount(totalRoomCount);
        order.setGuestName(req.getGuestName());
        order.setGuestPhone(req.getGuestPhone());
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        for (PendingOrderItem pendingItem : pendingItems) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setRoomId(pendingItem.roomId());
            item.setRoomName(pendingItem.roomName());
            item.setPrice(pendingItem.price());
            item.setQuantity(pendingItem.quantity());
            item.setSubtotal(pendingItem.subtotal());
            orderItemMapper.insert(item);
        }

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
    public OrderVO detailByUser(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException("无权查看此订单");
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
        // TODO: 实际计算退房手续费
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BusinessException("无权操作此订单");

        String cancelId = "CANCEL-" + UUID.randomUUID().toString().substring(0, 8);
        order.setCancelConfirmId(cancelId);
        order.setStatus(4); // 退房申请中
        orderMapper.updateById(order);

        Map<String, Object> result = new HashMap<>();
        result.put("cancelConfirmId", cancelId);
        result.put("originalAmount", order.getTotalAmount());
        result.put("cancelPenalty", BigDecimal.ZERO);
        result.put("refundAmount", order.getTotalAmount());
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

    private record PendingOrderItem(Long roomId, String roomName, BigDecimal price, Integer quantity, BigDecimal subtotal) {
    }
}
