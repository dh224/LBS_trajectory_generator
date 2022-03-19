package com.trajectory.trajectorygenerationporject.DAO;

import com.trajectory.trajectorygenerationporject.POJO.City;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface CityDAO {
    public List<City> listCity();

    public City findCityByAdcode(String adCode);
    public List<City> findCityListByAdName(String adName);
    public List<City> findCityListByCityCode(String cityNode);
    public List<City> findCityListByCityName(String cityName);
    public List<City> findCityListWithPOI();

    public void AddOnePOINumberByAdCode(String adCode);
    public String findCityCodeByAdCode(String adCode);
    public String findCityNameByAdCode(String adCode);

    public String findAdCodeByCityNameAndAdname(String cityName, String adName);

    public String findAdNameByAdCode(String adCode);
}
