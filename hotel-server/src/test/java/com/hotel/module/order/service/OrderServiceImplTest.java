package com.hotel.module.order.service;

import com.hotel.common.exception.BusinessException;
import com.hotel.module.order.dto.OrderCreateRequest;
import com.hotel.module.order.entity.Order;
import com.hotel.module.order.entity.OrderItem;
import com.hotel.module.order.mapper.OrderItemMapper;
import com.hotel.module.order.mapper.OrderMapper;
import com.hotel.module.order.service.impl.OrderServiceImpl;
import com.hotel.module.order.vo.OrderVO;
import com.hotel.module.resource.entity.Hotel;
import com.hotel.module.resource.entity.Room;
import com.hotel.module.resource.mapper.HotelMapper;
import com.hotel.module.resource.mapper.RoomMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private HotelMapper hotelMapper;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void cancelShouldRejectOtherUsersOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(99L);
        order.setStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.cancel(100L, 1L));

        assertEquals("无权操作此订单", exception.getMessage());
        verify(orderMapper, never()).updateById(order);
    }

    @Test
    void createShouldCalculateTotalByStayNights() {
        Hotel hotel = new Hotel();
        hotel.setId(10L);
        when(hotelMapper.selectById(10L)).thenReturn(hotel);

        Room room = new Room();
        room.setId(20L);
        room.setHotelId(10L);
        room.setStatus(1);
        room.setName("Superior King Room");
        room.setPrice(new BigDecimal("500"));
        when(roomMapper.selectById(20L)).thenReturn(room);
        when(orderItemMapper.selectList(any())).thenReturn(java.util.List.of());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order inserted = invocation.getArgument(0);
            inserted.setId(100L);
            return 1;
        });

        OrderCreateRequest.OrderItemRequest item = new OrderCreateRequest.OrderItemRequest();
        item.setRoomId(20L);
        item.setQuantity(2);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setHotelId(10L);
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(4));
        request.setGuestName("tester");
        request.setGuestPhone("13800000000");
        request.setItems(java.util.List.of(item));

        OrderVO orderVO = orderService.create(1L, request);

        verify(orderMapper).insert(orderCaptor.capture());
        verify(orderItemMapper).insert(itemCaptor.capture());

        assertNotNull(orderVO);
        assertEquals(0, new BigDecimal("3000").compareTo(orderCaptor.getValue().getTotalAmount()));
        assertEquals(2, orderCaptor.getValue().getRoomCount());
        assertEquals(0, new BigDecimal("3000").compareTo(itemCaptor.getValue().getSubtotal()));
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    void createShouldRejectRoomFromAnotherHotel() {
        Hotel hotel = new Hotel();
        hotel.setId(10L);
        when(hotelMapper.selectById(10L)).thenReturn(hotel);

        Room room = new Room();
        room.setId(20L);
        room.setHotelId(11L);
        room.setStatus(1);
        room.setPrice(new BigDecimal("500"));
        when(roomMapper.selectById(20L)).thenReturn(room);

        OrderCreateRequest.OrderItemRequest item = new OrderCreateRequest.OrderItemRequest();
        item.setRoomId(20L);
        item.setQuantity(1);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setHotelId(10L);
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(2));
        request.setGuestName("tester");
        request.setGuestPhone("13800000000");
        request.setItems(java.util.List.of(item));

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.create(1L, request));

        assertEquals("所选房型不属于当前酒店", exception.getMessage());
    }
}
