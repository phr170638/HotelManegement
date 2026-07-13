package com.hotel.module.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.module.resource.entity.Hotel;
import com.hotel.module.search.dto.HotelSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HotelMapper extends BaseMapper<Hotel> {

    Page<Hotel> searchHotels(Page<Hotel> page, @Param("req") HotelSearchRequest req);

    Hotel selectDetailById(@Param("id") Long id);
}
