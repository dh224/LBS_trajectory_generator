package com.trajectory.trajectorygenerationporject.DAO;

import com.trajectory.trajectorygenerationporject.POJO.POIType;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface POITypeDAO {
    public List<POIType> listPOIType();
    public POIType findTypeCodeBySubCategory(String subCategory);
}
