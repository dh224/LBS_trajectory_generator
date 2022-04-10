package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trajectory {
    public enum State { S, E, A, I, R };
    public int age;
    public String job;
    public String sex;
    public int drivingRate;
    public List<Position> path;
    public List<LocalDateTime> timeLine;
    public String patternName;
    public List<Map<Integer, List<Map<String, Integer>>>> pattern;


    //有关疫情模拟相关的属性（不得不添加的)
    public int id;
    public State state;
    public int virusNum;
    public int maskRate;
    public String isVaccines;
    public int infectedBy;
    public LocalDateTime infectedTime;
    public LocalDateTime exposureTime;
    public LocalDateTime lastContactTime;
    public LocalDateTime lastContactTimeInPoi;
    public LocalDateTime recoverTime;
    public double maxVirusNumInContact;
    public String lastPoiid;
    public double virusNumSumInContact;
    public int maxVirusNumIdInContact;
    public boolean isInContacted;
    public boolean isInPoiContacted;
    public boolean isAsym;
    public int incubationTimeOfHours;
    public int contactLevel;
    public boolean isExposureInPoi;

    public Trajectory(String patternName, List<Map<Integer, List<Map<String, Integer>>>> pattern, int age, String job, String sex, int maskRate, String isVaccines, int drivingRate){
        this.patternName = patternName;
        this.pattern = pattern;
        this.age = age;
        this.job = job;
        this.sex = sex;
        this.maskRate = maskRate;
        this.isVaccines = isVaccines;
        this.path = new ArrayList<Position>();
        this.timeLine = new ArrayList<>();
        this.drivingRate = drivingRate;
        this.contactLevel = 0;
        this.isInContacted = false;
        this.virusNumSumInContact = 0;
        this.isAsym = false;
    }
    public void addPositionWithTimeline(Position position, LocalDateTime localDateTime){
        this.path.add(position);
        this.timeLine.add(localDateTime);
    }
}
