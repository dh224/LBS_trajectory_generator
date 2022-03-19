package com.trajectory.trajectorygenerationporject.DAO;

import com.trajectory.trajectorygenerationporject.POJO.POIs;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface POIsDAO {
    public List<POIs> POIsList();
    public List<POIs> findPOIListByCityName(String cityName);
    public List<POIs> findPOIListByCityCode(String cityCode);
    public List<POIs> findPOIListByAdCode(String adCode);
    public List<POIs> findPOIListByAdName(String adAdName);
    public List<POIs> findPOIListByTypeName(String typeName);
    public List<POIs> findPOIListByTypeCode(String typeCode);
    public List<POIs> findPOIListByTypeCodeAndCityCode(String typeCode, String cityCode);
    public POIs findPOIByID(String poiid);
    public void insertPOI(POIs pois);

}
