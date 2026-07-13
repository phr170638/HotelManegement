package com.hotel.module.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.module.resource.entity.City;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CityMapper extends BaseMapper<City> {

    @Select("SELECT * FROM t_city WHERE hot = 1")
    List<City> selectHotCities();
}
