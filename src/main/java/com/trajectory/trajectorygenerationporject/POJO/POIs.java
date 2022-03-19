package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class POIs {
    private String adCode;
    private String adName;
    private String cityCode;
    private String cityName;
    private String POIID;
    private String POIName;
    private String POITypeName;
    private String POITypeCode;
    private String Lng;
    private String Lat;
    private String entrLng;
    private String entrLat;
    private String exitLng;
    private String exitLat;
}
