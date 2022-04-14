package com.trajectory.trajectorygenerationporject.POJO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class OriginPath {
    private int distance;
//    private int duration;
    private int size; // 轨迹的段数
    private String mode; //轨迹使用的出行工具
    private List<String> step_mode; // 轨迹中每一段的出行工具
    private List<Integer> step_distance;
    private List<Integer> step_duration;
    private List<Double> step_velocity;
    private List<List<Position>> step_polyLine;
    public OriginPath(){
        this.step_distance = new ArrayList<>();
        this.step_duration = new ArrayList<>();
        this.step_velocity = new ArrayList<>();
        this.step_polyLine = new ArrayList<>();
        this.step_mode = new ArrayList<>();
        this.size = 0;
    }
}
