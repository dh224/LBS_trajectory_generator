package com.trajectory.trajectorygenerationporject.Service;

import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.trajectory.trajectorygenerationporject.Common.Util;
import com.trajectory.trajectorygenerationporject.DAO.CityDAO;
import com.trajectory.trajectorygenerationporject.DAO.POIsDAO;
import com.trajectory.trajectorygenerationporject.POJO.OriginPath;
import com.trajectory.trajectorygenerationporject.POJO.POIs;
import com.trajectory.trajectorygenerationporject.POJO.Position;
import com.trajectory.trajectorygenerationporject.POJO.Trajectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
@Service
public class PathServiceImpl implements PathService {
    private final Log log = LogFactory.getLog(PathServiceImpl.class);

    @Autowired
    CityDAO cityDAO;
    @Autowired
    POIsDAO poisDAO;
    @Autowired
    POIsService poIsService;
    Random rand = new Random();

    @Override
    public List<Trajectory> getTrajectoriesFromPost(LocalDateTime startTime, LocalDateTime endTime, int trajectoryNum, JSONArray patterns){
        List<Trajectory> res;
        for (Object p : patterns) {
            JSONObject tt = (JSONObject)p;

        }
        return null;
    }





    @Override
    public Trajectory getTrajectory(String cityName, String adName, String startT, String endT, List<Map<Integer, List<Map<String, Integer>>>> pattern, boolean isRestrict, int age, String job, String sex, boolean isMask, String Vaccines, int drivingRate, int commutingTimeRate,String index) throws IOException {
        //要求起始值和结束的时间为整小时。
        System.out.println("list::" + pattern);
        System.out.println(startT);
        System.out.println(endT);
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int maxRandomStayTime = 15; //minues;
        var startTime = LocalDateTime.parse(startT,dateFormatter);
        var endTime = LocalDateTime.parse(endT, dateFormatter);
        String cityCode = cityDAO.findCityCodeByAdCode(cityDAO.findAdCodeByCityNameAndAdname(cityName, adName));
        Trajectory res = new Trajectory(age, job, sex, isMask, Vaccines);
        Map<String, POIs> thisPathPOIs = new HashMap<>();
        var curTime = startTime; //设置当前时间，在接下去的循环中会更新它
        int startdayofweek = curTime.getDayOfWeek().getValue() - 1; //初始日期是星期几 由于是list存储，因此需要-1
        var startdayPattern = pattern.get(startdayofweek); //获得初始日的行为模式表
        var needPositions = startdayPattern.get(curTime.getHour()); // 获得当前应该去的所有位置 MAP
        var curPositionTypeMap = needPositions.get(rand.nextInt(needPositions.size()));
        var curPositionType = getTypeCodeByCurMap(curPositionTypeMap);// 设置在起始时间的位置类型.
        String startPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCode(cityCode, curPositionType);
        POIs startPOI = poIsService.findPOIByID(startPOIID);
        this.log.info("起始点的位置是:" + startPOI.getPOIName());
        thisPathPOIs.put(curPositionType, poisDAO.findPOIByID(startPOIID)); //添加初始的POI位置,此后的位置或许可以根据此poi的周围随机选择
        while(curTime.compareTo(endTime) < 0){ // 当前时间早于生成终止时间，继续生成
            var curDayPattern = pattern.get(curTime.getDayOfWeek().getValue() - 1); //得到当天的行为模式表
            var curTimeOfHour = curTime.getHour(); //获得当前是第几个小时
                    //curDayPattern.get(curTimeOfHour).contains(curPositionType)
            System.out.println(curPositionType + "  " + curDayPattern.get(curTimeOfHour) + Integer.toString(curTimeOfHour) + curDayPattern);
            if(Util.isHaveTypeCode(curPositionType,curDayPattern.get(curTimeOfHour))){ // 当前位置类型已经在应该在的位置了，因此需要创建停留点
                System.out.println("创建停留点");
                if(!thisPathPOIs.containsKey(curPositionType)){
                    //搜索POI只考虑城市，而不考虑当前距离
                    this.log.info("不考虑距离获取POI" );
                    String targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCode(cityCode, curPositionType);
                    thisPathPOIs.put(curPositionType, poisDAO.findPOIByID(targetPOIID));
                }else{
                    System.out.println("当前列表中已经有了typecode了");
                    var nextHourTime = curTime.plusHours(1).minusSeconds(curTime.getMinute());
                    curTime = pathStayAwhile(curTime, nextHourTime, thisPathPOIs.get(curPositionType),res);
                }
            }else{ //需要进行移动.
                //进行随机的停留时间，增加真实性。
                var RTGTime = curTime.plusMinutes(rand.nextInt(maxRandomStayTime));
                System.out.println(thisPathPOIs + curPositionType);
                System.out.println("需要进行移动" + curDayPattern.get(curTimeOfHour) + "curpoiType:" + curPositionType);
                curTime = pathStayAwhile(curTime, RTGTime, thisPathPOIs.get(curPositionType),res);
                curTimeOfHour = curTime.getHour(); // 更新小时
                curDayPattern = pattern.get(curTime.getDayOfWeek().getValue() - 1); //更新日行为模式，以防等待后进入下一天.
                //curDayPattern.get(curTimeOfHour).contains(curPositionType)
                if(Util.isHaveTypeCode(curPositionType,curDayPattern.get(curTimeOfHour))){ //避免在等待后进入下一个时间段，并且下一个时间段应处位置等同于当前位置。
                    var nextHourTime = curTime.plusHours(1).minusSeconds(curTime.getMinute());
                    curTime = pathStayAwhile(curTime, nextHourTime,thisPathPOIs.get(curPositionType),res);//相当于又等到下个小时.
                }else{ //确保会前往下一个地点
                    Map<String, Integer> targetPositionTypeMap;
                    String targetPositionType;
                    if(curDayPattern.get(curTimeOfHour).size() > 1){ // 如果当前时刻行为模式中有多个候选项。
                        targetPositionTypeMap = curDayPattern.get(curTimeOfHour).get(rand.nextInt(curDayPattern.get(curTimeOfHour).size())) ; // 随机选择地点
                    }else{ // 只有一种可能
                        targetPositionTypeMap = curDayPattern.get(curTimeOfHour).get(0); //选第一个
                    }
                    targetPositionType = getTypeCodeByCurMap(targetPositionTypeMap);
                    System.out.println("下一点的type" + targetPositionType);
                    if(thisPathPOIs.containsKey(targetPositionType)){ //如果此地在之前已经被访问过
                        //发送请求，拼接
                        POIs curPoi = thisPathPOIs.get(curPositionType);
                        POIs nextPoi = thisPathPOIs.get(targetPositionType);
                        this.log.info("前往重复的目标POI,名称是："+ nextPoi.getPOIName() );
                        if(isDriving(curPoi, nextPoi, drivingRate)){
                            LocalDateTime temp = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime); // 出发！
                            curTime = temp;
                            System.out.println("设置当前点为：" + nextPoi.getPOITypeCode());
                            curPositionType = nextPoi.getPOITypeCode();
                        }else{
                            LocalDateTime temp = sentGetAndCombinationTrajectoryWithWalkingmode(curPoi, nextPoi, res, curTime); // 出发！
                            if(temp != null) curTime = temp;
                            else curTime = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime);
                            System.out.println("设置当前点为：" + nextPoi.getPOITypeCode());
                            curPositionType = nextPoi.getPOITypeCode();
                        }
                    }else{ //若没有访问过此类地点，则先随机获得一个该地点的POI。随后再申请轨迹
                        String targetPOIID;
                        POIs curPoi = thisPathPOIs.get(curPositionType);
                        Position curPoiPosition = new Position(curPoi.getLng(), curPoi.getLat(), curPoi.getPOITypeCode());
                        if(commutingTimeRate < 5){
                            //只考虑类别和城市，可能会导致大范围通勤
                            System.out.println("直接申请的POI");
                            targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCode(cityCode, targetPositionType);
                            if (targetPOIID==null){
                                targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCode(cityCode, targetPositionType);
                            }
                        }else{
                            //搜索POI考虑城市和当前所在的位置，减少大范围通勤情况.
                            this.log.info("限定范围的申请POIType:" + targetPositionType + "!\n当前的中心点是:" + curPoiPosition.getLng() + "," + curPoiPosition.getLat());
                            targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCodeAndDistance(cityCode, targetPositionType, curPoiPosition,targetPositionTypeMap.get(targetPositionType));
                            if (targetPOIID==null){
                                targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCode(cityCode, targetPositionType);
                            }
                        }
                        POIs nextPoi = poisDAO.findPOIByID(targetPOIID);
                        this.log.info("一个新的目标POI名称是:" + nextPoi.getPOIName() + "当前预期的最大通勤距离是：" + targetPositionTypeMap.get(targetPositionType));
                        thisPathPOIs.put(nextPoi.getPOITypeCode(), nextPoi); // 将新地点插入map中
                        if(isDriving(curPoi, nextPoi, drivingRate)){
                            curTime = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime); // 出发！
                            System.out.println("新设置当前点为：" + nextPoi.getPOITypeCode());
                            curPositionType = nextPoi.getPOITypeCode();
                        }else{
                            LocalDateTime temp = sentGetAndCombinationTrajectoryWithWalkingmode(curPoi, nextPoi, res, curTime); // 出发！
                            if(temp != null) curTime = temp;
                            else curTime = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime); // 出发！
                            System.out.println("新设置当前点为：" + nextPoi.getPOITypeCode());
                            curPositionType = nextPoi.getPOITypeCode();
                        }
                        //curTime = sentGetAndCombinationTrajectory(curPoi, nextPoi, res, drivingRate, curTime);// 出发！
                    }
                }
            }
        }
        RemoveRepeatNum(res);
        Util.outputtheTrajectoryPOIS(thisPathPOIs,index);
        return res;
    }

    @Override
    public Integer choosePattern(Integer patternRateNum, List<Integer> patternChooser) {
        return patternChooser.get(rand.nextInt(patternRateNum));
    }

    public String getTypeCodeByCurMap(Map<String, Integer> curMap){
        return curMap.keySet().toArray()[0].toString();
    }
    public boolean isDriving(POIs curPoi, POIs nextPoi, int rate){
        double distance = Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat());
        boolean isDriving = false;
        if(distance / 2000.0 > 1){
            if(rate/(distance / 2000.0) > rand.nextInt(10)){
                isDriving = true;
            }
        }else{
            double a = 10 - distance / 2000.0 * 10;
            if(rate > a +  rand.nextInt(10)){
                isDriving = true;
            }
        }
        return  isDriving;
    }
    public LocalDateTime sentGetAndCombinationTrajectoryWithDrivingmode(POIs curPOI, POIs nextPOI, Trajectory res, LocalDateTime curTime) throws IOException {
            double distance = Util.getDistance(curPOI.getLng(), curPOI.getLat(), nextPOI.getLng(), nextPOI.getLat());
            String startLL = curPOI.getLng() + "," + curPOI.getLat();
            String endLL = nextPOI.getLng() + "," + nextPOI.getLat();
            String url = "https://restapi.amap.com/v5/direction/" + "driving" + "?parameters&key=192b951ff8bc56e05cb476f8740a760c&origin="
                    + startLL + "&destination=" + endLL + "&origin_id=" + curPOI.getPOIID() + "&destination_id" + nextPOI.getPOIID() +
                    "&show_fields=cost,polyline";
            System.out.println("sentGetAndCombinationTrajectory   " + url);
            JSONObject singlePath =  Util.sentGet(url);
            OriginPath originPath = getOriginPathFromJSON(singlePath, "driving");
            //Util.outputtheOriginPath(originPath, curTime.getDayOfWeek().toString() + "D" + curTime.getHour() + "H" + curTime.getMinute() + "M" + curTime.getSecond());
            curTime = addPositionFromOriginPath(originPath,curTime,  5, res); //此时curTime为最后一点的时间，即到达时间.
            return curTime;
    }

    public LocalDateTime sentGetAndCombinationTrajectoryWithWalkingmode(POIs curPOI, POIs nextPOI, Trajectory res, LocalDateTime curTime) throws IOException {
        double distance = Util.getDistance(curPOI.getLng(), curPOI.getLat(), nextPOI.getLng(), nextPOI.getLat());
        String startLL = curPOI.getLng() + "," + curPOI.getLat();
        String endLL = nextPOI.getLng() + "," + nextPOI.getLat();
        String url = "https://restapi.amap.com/v5/direction/" + "walking" + "?parameters&key=192b951ff8bc56e05cb476f8740a760c&origin="
                + startLL + "&destination=" + endLL + "&origin_id=" + curPOI.getPOIID() + "&destination_id" + nextPOI.getPOIID() +
                "&show_fields=cost,polyline";
        this.log.info("sentGetAndCombinationTrajectory " + url);
        JSONObject singlePath =  Util.sentGet(url);

        if(singlePath.get("status").toString().equals("0")){
            return null;
        }
        OriginPath originPath = getOriginPathFromJSON(singlePath, "walking");
        //Util.outputtheOriginPath(originPath, curTime.getDayOfWeek().toString() + "D" + curTime.getHour() + "H" + curTime.getMinute() + "M" + curTime.getSecond());
        curTime = addPositionFromOriginPath(originPath,curTime,  5, res); //此时curTime为最后一点的时间，即到达时间.
        return curTime;
    }
    public LocalDateTime sentGetAndCombinationTrajectory(POIs curPOI, POIs nextPOI, Trajectory res, int drivingRate, LocalDateTime curTime) throws IOException {
        double distance = Util.getDistance(curPOI.getLng(), curPOI.getLat(), nextPOI.getLng(), nextPOI.getLat());
        boolean isDriving = false;
        System.out.println("距离：" + distance);
        if(distance / 2000.0 > 1){
            if(drivingRate > rand.nextInt(10)){
                isDriving = true;
            }
        }else{
            double a = 10 - distance / 2000.0 * 10;
            if(drivingRate > a +  rand.nextInt(10)){
                isDriving = true;
            }
        }
        String startLL = curPOI.getLng() + "," + curPOI.getLat();
        String endLL = nextPOI.getLng() + "," + nextPOI.getLat();
        String mode = isDriving? "driving":"walking";
        String url = "https://restapi.amap.com/v5/direction/" + mode + "?parameters&key=192b951ff8bc56e05cb476f8740a760c&origin="
                + startLL + "&destination=" + endLL + "&origin_id=" + curPOI.getPOIID() + "&destination_id" + nextPOI.getPOIID() +
                "&show_fields=cost,polyline";
        this.log.info("sentGetAndCombinationTrajectory " + url);
        JSONObject singlePath =  Util.sentGet(url);
        OriginPath originPath = getOriginPathFromJSON(singlePath, mode);
        //Util.outputtheOriginPath(originPath, curTime.getDayOfWeek().toString() + "D" + curTime.getHour() + "H" + curTime.getMinute() + "M" + curTime.getSecond());
        curTime = addPositionFromOriginPath(originPath,curTime,  5, res); //此时curTime为最后一点的时间，即到达时间.
        return curTime;
    }
    public static void RemoveRepeatNum(Trajectory trajectory){
        for(int i = 0;i < trajectory.path.size() - 1; i++){
            LocalDateTime t = trajectory.timeLine.get(i);
            for(int j = i + 1; j < trajectory.path.size(); j++){
                if(trajectory.timeLine.get(j).compareTo(t) == 0){
                    trajectory.timeLine.remove(j);
                    trajectory.path.remove(j);
                }else{
                    break;
                }
            }
        }
    }

    public LocalDateTime pathStayAwhile(LocalDateTime startTime, LocalDateTime endTime, POIs poi,Trajectory res){
        System.out.println("pathStay" + poi.getPOIName());

        while(startTime.plusSeconds(5).compareTo(endTime) < 0){
            var temp = new Position(poi.getLng(), poi.getLat(),poi.getPOITypeCode());
            res.addPathWithTimeline(temp,startTime);
            startTime = startTime.plusSeconds(5);
        }
        return startTime;
    }

    public static LocalDateTime addPositionFromOriginPath(OriginPath originPath, LocalDateTime startTime, int d, Trajectory res){ // d = 5;
        int all_Distance = originPath.getDistance(); // 总距离;
        for(int i = 0;i < originPath.getSize(); i++){ // 对每分段轨迹进行取点，建立符合gps规律的数据
            //先插入第一个点
            Position startPosition = new Position(originPath.getStep_polyLine().get(i).get(0).getLng(),
                    originPath.getStep_polyLine().get(i).get(0).getLat(),
                    originPath.getStep_polyLine().get(i).get(0).getTypeCode());
            res.addPathWithTimeline(startPosition, startTime);
            int stepDur = originPath.getStep_duration().get(i); //获得当前分段的总耗时
            LocalDateTime stepEndTime = startTime.plusSeconds(stepDur);
            startTime = startTime.plusSeconds(d);
            int pointNumber = stepDur / d; // 获取当前分段理应插几个点
            double temp_velocity = originPath.getStep_velocity().get(i); // 得到分段的平均速度 m / s
            double stepForward = temp_velocity * d; // 得到每d秒理应前进的距离,即步长
            if(originPath.getStep_polyLine().get(i).size() - 2 == pointNumber){ // 说明当前分段有的点和理应插入的点个数相同，那么直接插到底
                for(int j = 1; j < originPath.getStep_polyLine().get(i).size(); j++){ // 跳过已经插入的第一个点
                    var tempPosition = new Position(originPath.getStep_polyLine().get(i).get(j).getLng(),
                            originPath.getStep_polyLine().get(i).get(j).getLat(),
                            originPath.getMode());
                    if(originPath.getStep_polyLine().get(i).size() - 1 == j){ // 到了分段的最后一点
                        res.addPathWithTimeline(tempPosition,stepEndTime);
                        startTime = stepEndTime.plusSeconds(d);
                        break;
                    }else{
                        res.addPathWithTimeline(tempPosition, startTime);
                        startTime = startTime.plusSeconds(d);
                    }
                }
            }else{ // 如果不相同，则需要进行插值计算
                var last = startPosition;
                var stepLong = stepForward;
                int k = 1;
                int j = 0;
                boolean isInsertLast = false;
                while(j < pointNumber){
                    if(k == originPath.getStep_polyLine().get(i).size() - 1){ // 到了最后一点了
                        double dis = Util.getDistance(last.getLng(), last.getLat(),
                                originPath.getStep_polyLine().get(i).get(k).getLng(),
                                originPath.getStep_polyLine().get(i).get(k).getLat());
                        if(dis < stepLong){
                            var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat(),
                                    originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                            startTime = stepEndTime;
                            res.addPathWithTimeline(lastPosition,startTime); // 插入最后一点
                            startTime = startTime.plusSeconds(d);
                            isInsertLast = true;
                            break;
                        }else{
                            double rate = stepLong / dis;
                            j++;
                            var tempLL = Util.getMidLL(last.getLng(), last.getLat(), originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat(), rate);
                            Position insertPosition = new Position(tempLL.get(0).toString(), tempLL.get(1).toString(), originPath.getMode());
                            res.addPathWithTimeline(insertPosition, startTime);
                            startTime = startTime.plusSeconds(d);
                            stepLong = stepForward; // 恢复步长
                            last = insertPosition; // 更新当前位置
                        }
                    }else{
                        double dis = Util.getDistance(last.getLng(), last.getLat(),
                                originPath.getStep_polyLine().get(i).get(k).getLng(),
                                originPath.getStep_polyLine().get(i).get(k).getLat());
                        if(dis < stepLong){ // 要移动到下一点
                            stepLong -= dis;
                            last = originPath.getStep_polyLine().get(i).get(k);
                            k++;
                        }else{ // 说明此时应在两点间插值
                            double rate = stepLong / dis;
                            j++;
                            var tempLL = Util.getMidLL(last.getLng(), last.getLat(), originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat(), rate);
                            Position insertPosition = new Position(tempLL.get(0).toString(), tempLL.get(1).toString(), originPath.getMode());
                            res.addPathWithTimeline(insertPosition, startTime);
                            startTime = startTime.plusSeconds(d);
                            stepLong = stepForward; // 恢复步长
                            last = insertPosition; // 更新当前位置
                        }
                    }
                }
                if(!isInsertLast){
                    var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                            originPath.getStep_polyLine().get(i).get(k).getLat(),
                            originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                    startTime = stepEndTime;
                    res.addPathWithTimeline(lastPosition,startTime); // 插入最后一点
                    startTime = startTime.plusSeconds(d);
                }
            }
        }

        return startTime;
    }

    public static OriginPath getOriginPathFromJSON(JSONObject jsonObject, String pathMode){
        OriginPath originPath = new OriginPath();
        //System.out.println(jsonObject.toString());
        JSONObject path = jsonObject.getJSONObject("route").getJSONArray("paths").getJSONObject(0); //取第一个轨迹
        originPath.setDistance(path.getInteger("distance"));
        originPath.setMode(pathMode);
        //System.out.println("getOriginPathFromJSON  " + path);
        //originPath.setDuration(path.getJSONObject("cost").getInteger("duration"));
        JSONArray steps = path.getJSONArray("steps");
        for(int i = 0; i < steps.size(); i++){
            var step = steps.getJSONObject(i);
            originPath.getStep_distance().add(step.getInteger("step_distance")); //添加每step的路程
            originPath.getStep_duration().add(step.getJSONObject("cost").getInteger("duration")); //添加每step的耗时
            originPath.getStep_velocity().add((double) step.getInteger("step_distance") / (double)step.getJSONObject("cost").getInteger("duration")); //添加每step的均速
            String tempPolyline = step.getString("polyline");
            String[] tempEachPosition = tempPolyline.split(";");
            List<Position> step_Polyline = new ArrayList<>();
            for (var position : tempEachPosition){
                String t_Lng = position.split(",")[0];
                String t_Lat = position.split(",")[1];
                step_Polyline.add(new Position(t_Lng, t_Lat, pathMode));
            }
            originPath.getStep_polyLine().add(step_Polyline); //添加轨迹线
            originPath.setSize(originPath.getSize() + 1);
        }
        return originPath;
    }
}
