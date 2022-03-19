package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
public class Trajectory {
    public int age;
    public String job;
    public String sex;
    public boolean isMask;
    public String isVaccines;
    public List<Position> path;
    public List<LocalDateTime> timeLine;
    public Trajectory(int age, String job, String sex, boolean isMask, String isVaccines){
        this.age = age;
        this.job = job;
        this.sex = sex;
        this.isMask = isMask;
        this.isVaccines = isVaccines;
        this.path = new ArrayList<Position>();
        this.timeLine = new ArrayList<>();
    }
    public void addPathWithTimeline(Position position, LocalDateTime localDateTime){
        this.path.add(position);
        this.timeLine.add(localDateTime);
    }
}
