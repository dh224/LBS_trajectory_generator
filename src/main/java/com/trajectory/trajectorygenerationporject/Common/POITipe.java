package com.trajectory.trajectorygenerationporject.Common;

import java.util.HashMap;
import java.util.Map;

public class POITipe {
    public static final Map<String, String> POITypeMap;
    static{
        POITypeMap = new HashMap<String, String>();
        POITypeMap.put("加油站","010100");
        POITypeMap.put("汽车维修","030000");
        POITypeMap.put("餐饮服务大类","050000");
        POITypeMap.put("中餐厅","050100");
        POITypeMap.put("外国餐厅","050200");
        POITypeMap.put("快餐厅","050300");
        POITypeMap.put("咖啡厅","050500");
        POITypeMap.put("购物服务大类","060000");
        POITypeMap.put("商场","060100");
        POITypeMap.put("便利店","060200");
        POITypeMap.put("超级市场","060400");
        POITypeMap.put("综合市场","060700");
        POITypeMap.put("体育用品店","060900");
        POITypeMap.put("步行街","061001");
        POITypeMap.put("专卖店","061200");
        POITypeMap.put("书店","061205");
        POITypeMap.put("邮局","070400");
        POITypeMap.put("事务所","070700");
        POITypeMap.put("人才市场","070800");
        POITypeMap.put("洗浴推拿场所","071400");
        POITypeMap.put("运动场馆","080100");
    }
}
