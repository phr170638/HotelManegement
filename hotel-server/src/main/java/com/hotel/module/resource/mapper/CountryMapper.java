package com.hotel.module.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.module.resource.entity.Country;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CountryMapper extends BaseMapper<Country> {
}
