package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
@AllArgsConstructor
public class Trip {
    public String startType;
    public String endType;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public long durationM;
    public long durationS;
    public String patternName;
    public Double dis;
}
