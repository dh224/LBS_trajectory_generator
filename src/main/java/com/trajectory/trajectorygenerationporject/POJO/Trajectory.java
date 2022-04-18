package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.ibatis.type.LocalDateTimeTypeHandler;

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
    public LocalDateTime startTime;
    public LocalDateTime endTime;

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
    public Integer infectedNum;

    public String homeLng;
    public String homeLat;
    public String homeName;
    public Trajectory(String patternName, int maskRate, String isVaccines, int age, String sex, LocalDateTime startTime, LocalDateTime endTime, String homeLng, String homeLat, String homeName){
        this.startTime = startTime;
        this.endTime = endTime;
        this.patternName = patternName;
        this.age = age;
        this.sex = sex;
        this.maskRate = maskRate;
        this.isVaccines = isVaccines;
        this.contactLevel = 0;
        this.isInContacted = false;
        this.virusNumSumInContact = 0;
        this.isAsym = false;
        this.homeLng = homeLng;
        this.homeLat = homeLat;
        this.homeName = homeName;
        this.infectedNum = 0;
    }
    public Trajectory(String patternName, List<Map<Integer, List<Map<String, Integer>>>> pattern, int age, String job, String sex, int maskRate, String isVaccines, int drivingRate, LocalDateTime startTime, LocalDateTime endTime){
        this.startTime = startTime;
        this.endTime = endTime;
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

    public void printTrajectoryInformation(){
        String text = "输出当前轨迹的基本数据：";
        text += "性别：" + this.sex + " 年龄：" + this.age + " 行为模式名称:" + this.patternName + "家的名字：" + this.homeName
                + " 轨迹的第一点：" + this.path.get(0).getLng() + " " + this.path.get(0).getLat() + " " + this.path.get(0).getTypeCode()  + " "+ this.timeLine.get(0) +
                "轨迹的最后一点：" + this.path.get(this.path.size() - 1).getLng() + " " + this.path.get(this.path.size() - 1).getLat() + " " + this.path.get(this.path.size() - 1).getTypeCode() +
                " " + this.timeLine.get(this.path.size() - 1);
        System.out.println(text);
    }
}
