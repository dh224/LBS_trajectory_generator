package com.trajectory.trajectorygenerationporject.Service;

import com.trajectory.trajectorygenerationporject.DAO.POITypeDAO;
import com.trajectory.trajectorygenerationporject.POJO.POIType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class POITypeServiceImpl implements POITypeService {
    @Autowired
    POITypeDAO poiTypeDAO;
    @Override
    public List<POIType> listPOIType() {
        return poiTypeDAO.listPOIType();
    }

    @Override
    public POIType findTypeCodeBySubCategory(String subCategory) {
        return poiTypeDAO.findTypeCodeBySubCategory(subCategory);
    }
}
