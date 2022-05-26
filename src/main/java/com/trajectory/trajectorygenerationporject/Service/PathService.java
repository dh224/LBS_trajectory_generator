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
    public Trajectory getTrajectory(String patternName, String cityName, String adName, String startT, String endT, List<Map<Integer, List<Map<String, Integer>>>> pattern, boolean isRestrict, int age, String job, String sex, int maskRate, String Vaccines, int drivingRate, int commutingTimeRate,String index ,String key, String blockTime1, String blockTime2, String blockTime3) throws IOException, IndexOutOfBoundsException ;
    public Integer choosePattern(Integer patternRateNum, List<Integer> patternChooser);
    public void getTrajectoryAndSimulation(List<Trajectory> trajectories, int maxR0, String startTime, String endTime, int poiInfectedFactor,
                                               int infectedInCommuteFactor,
                                           int virusNumExp, int virusNumVar, int infectedHoursExp, int infectedHoursVar,
                                           int recoverHoursExp, int recoverHoursVar);
    public void recordRadiusFromTrajectories(List<Trajectory> trajectories, String startTime, String endTime, String fileName) throws IOException;
    public void recordAPListFromTrajectories(List<Trajectory> trajectories, String startT, String endT, String fileName) throws IOException;
    public String getTypeFromTypeCode(String type);
    public void recordTripFromTrajectories(List<Trajectory> trajectories, String startT, String endT, String fileName) throws IOException;
    public void recordBusFromTrajectories(List<Trajectory> trajectories, String startT, String endT, String fileName) throws IOException;
}