package com.trajectory.trajectorygenerationporject.Service;

import com.trajectory.trajectorygenerationporject.POJO.POIs;
import com.trajectory.trajectorygenerationporject.POJO.Position;

import java.io.IOException;
import java.util.List;

public interface POIsService {
    public List<POIs> POIsList();
    public List<POIs> findPOIListByCityName(String cityName);
    public List<POIs> findPOIListByCityCode(String cityCode);
    public List<POIs> findPOIListByAdCode(String adCode);
    public List<POIs> findPOIListByAdName(String adAdName);
    public List<POIs> findPOIListByTypeName(String typeName);
    public List<POIs> findPOIListByTypeCode(String typeCode);
    public POIs findPOIByID(String poiid);
    public void insertPOI(POIs pois);
    public void insertPOI(String poiid, String adCode, String adName, String cityCode,
                          String cityName, String POIName, String POITypeName, String POItypeCode,
                          String lng, String lat, String entrLng, String entrLat, String exitLng,
                          String exitLat);
    public void searchPOIsAllType(String cityName, String adName) throws IOException;
    public String findRandomPOIWithCityCodeAndTypeCode(String cityCode, String typeCode) throws IOException;
    public String findRandomPOIWithCityCodeAndTypeCodeAndDistance(String cityCode, String typeCode, Position basePosition,Integer radius) throws IOException;

    }
