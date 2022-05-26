package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@AllArgsConstructor
public class POIRecord {
    Position position;
    LocalDateTime startTime;
    LocalDateTime departureTime;
    int theHourOfDay;
    long stayTime;

}
