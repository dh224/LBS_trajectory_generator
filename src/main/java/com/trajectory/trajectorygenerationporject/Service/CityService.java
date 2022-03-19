package com.trajectory.trajectorygenerationporject.Service;

import com.trajectory.trajectorygenerationporject.POJO.City;

import java.util.List;

public interface CityService {
    public List<City> listCity();
    public City findCityByAdcode(String adCode);
    public List<City> findCityListByAdName(String adName);
    public List<City> findCityListByCityCode(String cityNode);
    public List<City> findCityListByCityName(String cityName);
    public List<City> findCityListWithPOI();
    public String findCityCodeByAdCode(String adCode);
    public String findCityNameByAdCode(String adCode);
    public String findAdCodeByCityNameAndAdname(String cityName, String adName);
    public String findAdNameByAdCode(String adCode);
}
