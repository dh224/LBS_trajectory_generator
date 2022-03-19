package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class POIType {
    private String typeCode;
    private String bigCategory;
    private String midCategory;
    private String subCategory;
}
