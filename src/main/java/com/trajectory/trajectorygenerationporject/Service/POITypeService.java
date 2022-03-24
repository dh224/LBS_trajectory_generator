package com.trajectory.trajectorygenerationporject.Service;

import com.trajectory.trajectorygenerationporject.POJO.POIType;

import java.util.List;

public interface POITypeService {
    public List<POIType> listPOIType();
    public POIType findTypeCodeBySubCategory(String subCategory);
}
