package com.hotel.module.resource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hotel.common.result.PageResult;
import com.hotel.module.resource.dto.HotelSaveRequest;
import com.hotel.module.resource.dto.RoomSaveRequest;
import com.hotel.module.resource.entity.BedType;
import com.hotel.module.resource.entity.Breakfast;
import com.hotel.module.resource.entity.City;
import com.hotel.module.resource.entity.Country;
import com.hotel.module.resource.vo.HotelVO;
import com.hotel.module.resource.vo.RoomVO;

import java.util.List;

public interface ResourceService {

    // 国家
    PageResult<Country> listCountries(Integer page, Integer size, String keyword);
    Country getCountry(Long id);
    void saveCountry(Country country);
    void updateCountry(Country country);
    void deleteCountry(Long id);

    // 城市
    PageResult<City> listCities(Integer page, Integer size, Long countryId, String keyword);
    List<City> listHotCities();
    void saveCity(City city);
    void updateCity(City city);
    void deleteCity(Long id);

    // 酒店
    PageResult<HotelVO> listHotels(Integer page, Integer size, Long cityId, String keyword, Integer starLevel);
    HotelVO getHotelDetail(Long id);
    void saveHotel(HotelSaveRequest request);
    void updateHotel(Long id, HotelSaveRequest request);
    void deleteHotel(Long id);

    // 房型
    List<RoomVO> listRoomsByHotelId(Long hotelId);
    void saveRoom(RoomSaveRequest request);
    void updateRoom(Long id, RoomSaveRequest request);
    void deleteRoom(Long id);

    // 早餐
    List<Breakfast> listBreakfasts();

    // 床型
    List<BedType> listBedTypes();
}
