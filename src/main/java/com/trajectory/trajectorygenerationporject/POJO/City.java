package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class City {
    private String adCode;
    private String adName;
    private String cityCode;
    private String cityName;
    private int POINumber;
}
