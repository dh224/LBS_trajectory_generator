package com.trajectory.trajectorygenerationporject.Service;

import com.alibaba.fastjson.JSONArray;
import com.trajectory.trajectorygenerationporject.POJO.Trajectory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface PathService {
    public Trajectory getTrajectoriy(String cityName, String adName, String startT, String endT, List<Map<Integer, List<String>>> pattern, boolean isRestrict, int age, String job, String sex, boolean isMask, String Vaccines,int drivingRate, int commutingTimeRate) throws IOException ;
}
