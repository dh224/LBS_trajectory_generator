package com.trajectory.trajectorygenerationporject.Service;

import com.alibaba.fastjson.JSONArray;
import com.trajectory.trajectorygenerationporject.POJO.Trajectory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface PathService {
    public List<Trajectory> getTrajectoriesFromPost(LocalDateTime startTime, LocalDateTime endTime, int trajectoryNum, JSONArray patterns);
    public Trajectory getTrajectory(String cityName, String adName, String startT, String endT, List<Map<Integer, List<Map<String, Integer>>>> pattern, boolean isRestrict, int age, String job, String sex, boolean isMask, String Vaccines, int drivingRate, int commutingTimeRate,String index) throws IOException ;
    public Integer choosePattern(Integer patternRateNum, List<Integer> patternChooser);

}
