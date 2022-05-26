package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Position {
    public String lng;
    public String lat;
    public String typeCode; // 当前点的属性 若在POI，则认为是该地点的类型。若不是，则为道路。
    public String poiid;
    public int mask;
    public Position(String lng, String lat, String typeCode){
        this.lng = lng;
        this.lat = lat;
        this.typeCode = typeCode;
    }
    public boolean isInPoi(){
        return !(poiid == null);
    }
}
