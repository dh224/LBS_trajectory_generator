package com.trajectory.trajectorygenerationporject.Controllor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.trajectory.trajectorygenerationporject.POJO.POIType;
import com.trajectory.trajectorygenerationporject.Service.POITypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TestController {
    @Autowired
    POITypeService poiTypeService;
    @CrossOrigin(originPatterns = "http://localhost:3000")
    @GetMapping("/listAllPoiType")
    public String list(){
        List<POIType> temp =  poiTypeService.listPOIType();
        JSONObject res = new JSONObject();
        JSONArray types = new JSONArray();
        for(int i = 0; i < temp.size(); i++){
            JSONObject singleRecord = new JSONObject();
            singleRecord.put("typecode", temp.get(i).getTypeCode());
            singleRecord.put("bigcategory", temp.get(i).getBigCategory());
            singleRecord.put("midcategory", temp.get(i).getMidCategory());
            singleRecord.put("subcategory", temp.get(i).getSubCategory());
            types.add(singleRecord);
        }
        res.put("types", types);
        return res.toJSONString();
    }
}
