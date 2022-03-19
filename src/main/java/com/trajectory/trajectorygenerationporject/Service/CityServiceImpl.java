package com.trajectory.trajectorygenerationporject.Service;

import com.trajectory.trajectorygenerationporject.DAO.CityDAO;
import com.trajectory.trajectorygenerationporject.POJO.City;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CityServiceImpl implements CityService {
    @Autowired
    private CityDAO cityDAO;
    @Override
    public List<City> listCity(){
        return cityDAO.listCity();
    }
    @Override
    public City findCityByAdcode(String adCode){
        return cityDAO.findCityByAdcode(adCode);
    }
    @Override
    public List<City> findCityListByAdName(String adName){
        return cityDAO.findCityListByAdName(adName);
    }
    @Override
    public List<City> findCityListByCityCode(String cityNode){
        return cityDAO.findCityListByCityCode(cityNode);
    }
    @Override
    public List<City> findCityListByCityName(String cityName){
        return cityDAO.findCityListByCityName(cityName);
    }
    @Override
    public List<City> findCityListWithPOI(){
        return cityDAO.findCityListWithPOI();
    }

    @Override
    public String findCityCodeByAdCode(String adCode) {
        return cityDAO.findCityCodeByAdCode(adCode);
    }

    @Override
    public String findCityNameByAdCode(String adCode) {
        return cityDAO.findCityNameByAdCode(adCode);
    }

    @Override
    public String findAdCodeByCityNameAndAdname(String cityName, String adName) {
        return cityDAO.findAdCodeByCityNameAndAdname(cityName, adName);
    }

    @Override
    public String findAdNameByAdCode(String adCode) {
        return cityDAO.findAdNameByAdCode(adCode);
    }

}
