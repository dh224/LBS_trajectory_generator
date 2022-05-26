package com.trajectory.trajectorygenerationporject.Service;

import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mysql.cj.conf.ConnectionUrlParser;
import com.trajectory.trajectorygenerationporject.Common.Util;
import com.trajectory.trajectorygenerationporject.DAO.CityDAO;
import com.trajectory.trajectorygenerationporject.DAO.POIsDAO;
import com.trajectory.trajectorygenerationporject.POJO.*;
import javassist.compiler.ast.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
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
    public List<Trajectory> getTrajectoriesFromPost(LocalDateTime startTime, LocalDateTime endTime, int trajectoryNum, JSONArray patterns) {
        List<Trajectory> res;
        for (Object p : patterns) {
            JSONObject tt = (JSONObject) p;

        }
        return null;
    }

    public String getTypeFromTypeCode(String typeCode){
        if("050".equals(typeCode.substring(0,3))){ //吃饭的地方
            return "dining";
        }else if("0601".equals(typeCode.substring(0,4)) || "0604".equals(typeCode.substring(0,4)) || "0610".equals(typeCode.substring(0,4))){
            // 商场、超市、步行街
            return "publicShopping";
        }else if("06".equals(typeCode.substring(0,2))){ //除了以上三个地点的消费场所
            return "normalShopping";
        } else if ("0801".equals(typeCode.substring(0, 4)) || "0802".equals(typeCode.substring(0, 4)) || "0804".equals(typeCode.substring(0, 4)) || "0805".equals(typeCode.substring(0, 4)) ||
                "110".equals(typeCode.substring(0, 3))
        ) {
            return "daytimeRecreation";
        } else if("0806".equals(typeCode.substring(0,4)) || "0803".equals(typeCode.substring(0,4))){ // 影剧院, 娱乐场所
            return "nighttimeRecreation";
        } else if("1203".equals(typeCode.substring(0,4))){ //商务住宅、住宅区
            return "home";
        } else if("140".equals(typeCode.substring(0,3)) || "1402".equals(typeCode.substring(0,4))){ // 科教文化服务 学校们
            return "publicScienceAndEducation";
        } else if("170".equals(typeCode.substring(0,3)) || "160".equals(typeCode.substring(0,3)) ){ //公司 工厂
            return "work";
        } else if("090".equals(typeCode.substring(0,3))){ //医院 药店等
            return "medical";
        }else return "other";
    }

    public int isStayInPosition(int rank, String type){
        if(rank == 1){
            if("nighttimeRecreation".equals(getTypeFromTypeCode(type)) || "dining".equals(getTypeFromTypeCode(type)) || "publicShopping".equals(getTypeFromTypeCode(type))){
                if("nighttimeRecreation".equals(getTypeFromTypeCode(type))){
                    return 10;
                }else return 5;
            }else return 0;
        }else if(rank == 2){
            if("nighttimeRecreation".equals(getTypeFromTypeCode(type)) ||  "dining".equals(getTypeFromTypeCode(type)) ||"daytimeRecreation".equals(getTypeFromTypeCode(type)) || "publicScienceAndEducation".equals(getTypeFromTypeCode(type)) || "publicShopping".equals(getTypeFromTypeCode(type)) || "normalShopping".equals(getTypeFromTypeCode(type)) || "medical".equals(getTypeFromTypeCode(type)) || "work".equals(getTypeFromTypeCode(type))){
                if("medical".equals(getTypeFromTypeCode(type))){
                    return 5;
                }else if("normalShopping".equals(getTypeFromTypeCode(type))){
                    return 5;
                }else if("work".equals(getTypeFromTypeCode(type))){
                    return 3;
                }else{
                    return 10;
                }
            }else return 0;
        }else if(rank == 3){
            if("nighttimeRecreation".equals(getTypeFromTypeCode(type)) ||  "dining".equals(getTypeFromTypeCode(type)) ||"daytimeRecreation".equals(getTypeFromTypeCode(type)) || "publicScienceAndEducation".equals(getTypeFromTypeCode(type)) || "publicShopping".equals(getTypeFromTypeCode(type)) || "normalShopping".equals(getTypeFromTypeCode(type)) ||
            "work".equals(getTypeFromTypeCode(type)) || "other".equals(getTypeFromTypeCode(type))){
                //System.out.println("当前是封锁等级3，无法去工作" + getTypeFromTypeCode(type));
                return 10;
            }else return 0;
        }
        return 0;
    }

    @Override
    public Trajectory getTrajectory(String patternName, String cityName, String adName, String startT, String endT,
                                    List<Map<Integer, List<Map<String, Integer>>>> pattern, boolean isRestrict, int age, String job,
                                    String sex, int maskRate, String Vaccines, int drivingRate, int commutingTimeRate, String index, String key,
                                    String blockT1, String blockT2, String blockT3) throws IOException, IndexOutOfBoundsException, NullPointerException {
        //要求起始值和结束的时间为整小时。
        //System.out.println("开始创建新轨迹");
        boolean isKeepStayPoint = false;
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int maxRandomStayTime = 30; //minues;
        var startTime = LocalDateTime.parse(startT, dateFormatter);
        var endTime = LocalDateTime.parse(endT, dateFormatter);
        int blockRank = 0;
        LocalDateTime blockTime1, blockTime2 = null, blockTime3 = null;
        blockTime1 = LocalDateTime.parse(blockT1, dateFormatter);
        boolean isAnotherBlockRank = true;
        if(blockT2 != null){
            blockTime2 = LocalDateTime.parse(blockT2, dateFormatter);
            blockTime3 = LocalDateTime.parse(blockT3, dateFormatter);
        }else{
            isAnotherBlockRank = false;
        }
        List<LocalDateTime> blockTimes = new ArrayList<>();
        blockTimes.add(blockTime1);
        if(isAnotherBlockRank){
            blockTimes.add(blockTime2);
            blockTimes.add(blockTime3);
        }
        int blockIndex = 0;
        boolean isAddedMskRate = false;
        String cityCode = cityDAO.findCityCodeByAdCode(cityDAO.findAdCodeByCityNameAndAdname(cityName, adName));
        String vacRank = "A";
        if(Util.rand.nextInt(10) > 3){
            vacRank = "B";
        }
        Trajectory res = new Trajectory(patternName, pattern, age, job, sex, maskRate, vacRank, drivingRate, startTime, endTime);
        Map<String, POIs> thisPathPOIs = new HashMap<>();
        var curTime = startTime; //设置当前时间，在接下去的循环中会更新它
        int startdayofweek = curTime.getDayOfWeek().getValue() - 1; //初始日期是星期几 由于是list存储，因此需要-1
        var startdayPattern = pattern.get(startdayofweek); //获得初始日的行为模式表
        var needPositions = startdayPattern.get(curTime.getHour()); // 获得当前应该去的所有位置 MAP
        var curPositionTypeMap = needPositions.get(rand.nextInt(needPositions.size()));
        var curPositionType = getTypeCodeByCurMap(curPositionTypeMap);// 设置在起始时间的位置类型.
        String startPOIID = poIsService.findRandomPOIWithCityCodeAndAdCodeAndTypeCode(cityCode, "330105", curPositionType, key);
        addHomeInformationAndInit(res, startPOIID);
        res.isInPoiContacted = true;
        res.lastContactTimeInPoi = curTime;
        res.lastPoiid = startPOIID;
        POIs startPOI = poIsService.findPOIByID(startPOIID);
//        this.log.info("起始点的位置是:" + startPOI.getPOIName());
        thisPathPOIs.put(curPositionType, poisDAO.findPOIByID(startPOIID)); //添加初始的POI位置,此后的位置或许可以根据此poi的周围随机选择
        while (curTime.compareTo(endTime) < 0) { // 当前时间早于生成终止时间，继续生成
            if(blockIndex < 3 && curTime.compareTo(blockTimes.get(blockIndex)) > 0){
                blockIndex++;
                blockRank = blockIndex;
            }
            if(!isAddedMskRate && blockRank > 0){
               int mkr = res.maskRate;
               if(mkr < 8){
                   mkr += 2;
               }
            }
            var curDayPattern = pattern.get(curTime.getDayOfWeek().getValue() - 1); //得到当天的行为模式表
            var curTimeOfHour = curTime.getHour(); //获得当前是第几个小时
            //curDayPattern.get(curTimeOfHour).contains(curPositionType)
//            System.out.println(curPositionType + "  " + curDayPattern.get(curTimeOfHour) + Integer.toString(curTimeOfHour) + curDayPattern);
            if (Util.isHaveTypeCode(curPositionType, curDayPattern.get(curTimeOfHour))) { // 当前位置类型已经在应该在的位置了，因此需要创建停留点
                if (!thisPathPOIs.containsKey(curPositionType)) {
                    //搜索POI只考虑城市，而不考虑当前距离
                    String targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCode(cityCode, curPositionType);
                    thisPathPOIs.put(curPositionType, poisDAO.findPOIByID(targetPOIID));
                } else {
                    var nextHourTime = curTime.plusHours(1).minusSeconds(curTime.getMinute());
                    if(isMask(res)){
                        curTime = pathStayAwhile(curTime, nextHourTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint, 1);
                    }else{
                        curTime = pathStayAwhile(curTime, nextHourTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint, 0);
                    }
                }
           } else { //需要进行移动.
                //进行随机的停留时间，增加真实性。
                var RTGTime = curTime.plusMinutes(rand.nextInt(maxRandomStayTime));
//                System.out.println(thisPathPOIs + curPositionType);
//                System.out.println("需要进行移动" + curDayPattern.get(curTimeOfHour) + "curpoiType:" + curPositionType);
                if(isMask(res)){
                    curTime = pathStayAwhile(curTime, RTGTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint, 1);
                }else{
                    curTime = pathStayAwhile(curTime, RTGTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint, 0);
                }
                curTimeOfHour = curTime.getHour(); // 更新小时
                curDayPattern = pattern.get(curTime.getDayOfWeek().getValue() - 1); //更新日行为模式，以防等待后进入下一天.
                //curDayPattern.get(curTimeOfHour).contains(curPositionType)
                if (Util.isHaveTypeCode(curPositionType, curDayPattern.get(curTimeOfHour))) { //避免在等待后进入下一个时间段，并且下一个时间段应处位置等同于当前位置。
                    var nextHourTime = curTime.plusHours(1).minusSeconds(curTime.getMinute());
                    if(isMask(res)){
                        curTime = pathStayAwhile(curTime, nextHourTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint, 1);//相当于又等到下个小时.
                    }else{
                        curTime = pathStayAwhile(curTime, nextHourTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint, 0);
                    }
                } else { //确保会前往下一个地点
                    Map<String, Integer> targetPositionTypeMap;
                    String targetPositionType;
                    if (curDayPattern.get(curTimeOfHour).size() > 1) { // 如果当前时刻行为模式中有多个候选项。
                        targetPositionTypeMap = curDayPattern.get(curTimeOfHour).get(rand.nextInt(curDayPattern.get(curTimeOfHour).size())); // 随机选择地点
                    } else { // 只有一种可能
                        targetPositionTypeMap = curDayPattern.get(curTimeOfHour).get(0); //选第一个
                    }
                    targetPositionType = getTypeCodeByCurMap(targetPositionTypeMap);
                    //System.out.println("要出发了，目前的所在地是:" + getTypeFromTypeCode(curPositionType));
                    if(Util.rand.nextInt(10) < isStayInPosition(blockRank,targetPositionType)){
                        //System.out.println("由于政策，停止出行, 要去的位置类型是：" + targetPositionType + "  当前的时间是：" + curTime);
                        boolean istimetonextlocation = false;
                        var nextHourTime = curTime.plusHours(1).minusMinutes(curTime.getMinute());
                        while (!istimetonextlocation){
                            curDayPattern = pattern.get(nextHourTime.getDayOfWeek().getValue() - 1); //更新日行为模式，以防等待后进入下一天.
                            if(curDayPattern.get(nextHourTime.getHour()).contains(targetPositionType)){
                                nextHourTime = nextHourTime.plusHours(1).minusMinutes(nextHourTime.getMinute());
                            }else{
                                istimetonextlocation = true;
                            }
                        }
                        if(isMask(res)){
                            curTime = pathStayAwhile(curTime, nextHourTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint, 1);
                        }else{
                            curTime = pathStayAwhile(curTime, nextHourTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint, 0);
                        }
                    }else{
                        if (thisPathPOIs.containsKey(targetPositionType)) { //如果此地在之前已经被访问过
                            //发送请求，拼接
                            POIs curPoi = thisPathPOIs.get(curPositionType);
                            POIs nextPoi = thisPathPOIs.get(targetPositionType);
                            if (isDriving(curPoi, nextPoi, drivingRate)) {
                                LocalDateTime temp = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime, key); // 出发！
                                curTime = temp;
                                curPositionType = nextPoi.getPOITypeCode();
                            } else {
                                LocalDateTime temp; // 出发！
                                if (isPublictransport(curPoi, nextPoi, blockRank) == true) {
                                    temp = sentGetAndCombinationTrajectoryWithPublictransportMode(curPoi, nextPoi, res, curTime, cityCode, key);
                                    if (temp != null) curTime = temp;
                                    else {
                                        if (Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat()) < 6000) {
                                            curTime = sentGetAndCombinationTrajectoryWithWalkingmode(curPoi, nextPoi, res, curTime, key); // 出发！
                                            this.log.info("公共出行无法获得方案, 选择了步行出行.当前目的地与出发地之间的距离是：" + Util.getDistance(curPoi.getLng(), curPoi.getLat(),
                                                    nextPoi.getLng(), nextPoi.getLat()));
                                        } else {
                                            curTime = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime, key); // 出发！
                                            this.log.info("公共出行无法获得方案, 选择了驾车出行.当前目的地与出发地之间的距离是：" + Util.getDistance(curPoi.getLng(), curPoi.getLat(),
                                                    nextPoi.getLng(), nextPoi.getLat()));
                                        }
                                    }
                                } else {
                                    if(blockRank > 1 && isDriving(curPoi, nextPoi, drivingRate)){
                                        temp = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime, key); // 出发！
                                    }else{
                                        if (isBycycling(curPoi, nextPoi, 3)) {
                                            temp = sentGetAndCombinationTrajectoryWithBicyclingmode(curPoi, nextPoi, res, curTime, key);
                                        } else {
                                            temp = sentGetAndCombinationTrajectoryWithWalkingmode(curPoi, nextPoi, res, curTime, key);
                                        }
                                    }
                                }
                                if (temp != null) curTime = temp;
                                else
                                    curTime = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime, key);
                                curPositionType = nextPoi.getPOITypeCode();
                            }
                        } else { //若没有访问过此类地点，则先随机获得一个该地点的POI。随后再申请轨迹
                            String targetPOIID;
                            POIs curPoi = thisPathPOIs.get(curPositionType);
                            Position curPoiPosition = new Position(curPoi.getLng(), curPoi.getLat(), curPoi.getPOITypeCode());
                            curPoiPosition.setPoiid(curPoi.getPOIID());
                            if (commutingTimeRate < 5) {
                                //只考虑类别和城市，可能会导致大范围通勤
                                targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCode(cityCode, targetPositionType);
                                if (targetPOIID == null) {
                                    targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCode(cityCode, targetPositionType);
                                }
                            } else {
                                //搜索POI考虑城市和当前所在的位置，减少大范围通勤情况.
//                            this.log.info("限定范围的申请POIType:" + targetPositionType + "!\n当前的中心点是:" + curPoiPosition.getLng() + "," + curPoiPosition.getLat());
                                targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCodeAndDistance_v3(cityCode,
                                        targetPositionType, curPoiPosition, targetPositionTypeMap.get(targetPositionType), key);
                                int rad = targetPositionTypeMap.get(targetPositionType) * 2;
                                while (targetPOIID == null) {
                                    targetPOIID = poIsService.findRandomPOIWithCityCodeAndTypeCodeAndDistance_v3(cityCode,
                                            targetPositionType, curPoiPosition, rad, key);
                                    rad *= 2;
                                }
                            }
                            POIs nextPoi = poisDAO.findPOIByID(targetPOIID);
//                        this.log.info("一个新的目标POI名称是:" + nextPoi.getPOIName() + "当前预期的最大通勤距离是：" + targetPositionTypeMap.get(targetPositionType));
                            thisPathPOIs.put(nextPoi.getPOITypeCode(), nextPoi); // 将新地点插入map中
                            if (isDriving(curPoi, nextPoi, drivingRate)) {
                                curTime = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime, key); // 出发！
//                            System.out.println("新设置当前点为：" + nextPoi.getPOITypeCode());
                                curPositionType = nextPoi.getPOITypeCode();
                            } else {
                                LocalDateTime temp;
                                if (isPublictransport(curPoi, nextPoi, blockRank) == true) {
                                    temp = sentGetAndCombinationTrajectoryWithPublictransportMode(curPoi, nextPoi, res, curTime, cityCode, key);
                                    if (temp != null) curTime = temp;
                                    else {
                                        if (Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat()) < 5000) {
                                            curTime = sentGetAndCombinationTrajectoryWithWalkingmode(curPoi, nextPoi, res, curTime, key); // 出发！
                                            this.log.info("公共出行无法获得方案, 选择了步行出行.当前目的地与出发地之间的距离是：" + Util.getDistance(curPoi.getLng(), curPoi.getLat(),
                                                    nextPoi.getLng(), nextPoi.getLat()));
                                        } else {
                                            curTime = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime, key); // 出发！
                                            this.log.info("公共出行无法获得方案, 选择了驾车出行.当前目的地与出发地之间的距离是：" + Util.getDistance(curPoi.getLng(), curPoi.getLat(),
                                                    nextPoi.getLng(), nextPoi.getLat()));
                                        }
                                    }
                                } else {
                                    if (isBycycling(curPoi, nextPoi, 2)) {
                                        temp = sentGetAndCombinationTrajectoryWithBicyclingmode(curPoi, nextPoi, res, curTime, key);
                                    } else
                                        temp = sentGetAndCombinationTrajectoryWithWalkingmode(curPoi, nextPoi, res, curTime, key); // 出发！
                                    if (temp != null) curTime = temp;
                                    else
                                        curTime = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime, key); // 出发！
                                }
//                            System.out.println("新设置当前点为：" + nextPoi.getPOITypeCode());
                                curPositionType = nextPoi.getPOITypeCode();
                            }
                        }
                    }
                }
            }
        }
        RemoveRepeatNum(res);
//        Util.outputtheTrajectoryPOIS(thisPathPOIs,index);
        return res;
    }

    @Override
    public Integer choosePattern(Integer patternRateNum, List<Integer> patternChooser) {
        return patternChooser.get(rand.nextInt(patternRateNum));
    }

    private boolean isBycycling(POIs curPoi, POIs nextPoi, int rate) {
        double distance = Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat());
        boolean isbycycling = false;
        if (distance / 2000.0 > 1) {
            if ((double) rate / (distance / 2000.0) > rand.nextInt(10)) {
                isbycycling = true;
            }
        } else {
            if (rate > rand.nextInt(10)) {
                isbycycling = true;
            }
        }
        return isbycycling;
    }

    public String getTypeCodeByCurMap(Map<String, Integer> curMap) {
        return curMap.keySet().toArray()[0].toString();
    }

    public boolean isPublictransport(POIs curPoi, POIs nextPoi, int blockRank) {
        double distance = Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat());
        if(distance < 1500) return false;
        if(blockRank > 1) return false;
        if (distance > 4000) {
            if (rand.nextInt(10) > 3) {
                return true;
            } else {
                return false;
            }
        } else {
            if (rand.nextInt(10) > 7) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean isDriving(POIs curPoi, POIs nextPoi, int rate) {
        double distance = Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat());
        boolean isDriving = false;
        if (distance / 5000.0 > 1) {
            double t = distance / 5000.0;
            if((distance / 5000) > 1.5){
                t = distance / 7500;
            }
            if (rate * t > rand.nextInt(10)) {
                isDriving = true;
            }
        } else {
            double a = 10 - distance / 5000 * 10;
            if (rate > a + rand.nextInt(10)) {
                isDriving = true;
            }
        }
        return isDriving;
    }

    public LocalDateTime sentGetAndCombinationTrajectoryWithPublictransportMode(POIs curPOI, POIs nextPOI, Trajectory res, LocalDateTime curTime, String cityCode, String key) throws IOException, IndexOutOfBoundsException {
        double distance = Util.getDistance(curPOI.getLng(), curPOI.getLat(), nextPOI.getLng(), nextPOI.getLat());
        DateTimeFormatter dtfm1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dtfm2 = DateTimeFormatter.ofPattern("HH-mm");
        String departureDate = dtfm1.format(curTime);
        String departureTime = dtfm2.format(curTime);
//        System.out.println("当前的时间是: " + departureDate + departureTime);
        String startLL = curPOI.getLng() + "," + curPOI.getLat();
        String endLL = nextPOI.getLng() + "," + nextPOI.getLat();
        String url =
                "https://restapi.amap.com/v5/direction/transit/integrated?parameters" + "&key=" + key + "&origin="
                        + startLL + "&destination=" + endLL + "&origin_id=" + curPOI.getPOIID() + "&destination_id" + nextPOI.getPOIID() + "&city1=" + cityCode + "&city2=" + cityCode +
                        "&AlternativeRoute=1&nightflag=1&data=" + departureDate + "&time=" + departureTime + "&show_fields=cost,polyline";
//        System.out.println("出行url:" + url);
        JSONObject singlePath = Util.sentGet(url);
        if (singlePath.get("count").toString().equals("0")) {
            return null;
        }
        OriginPath originPath = getOriginPathWithPublictransportModeFromJSON(singlePath, "Publictransport");
        curTime = addPositionFromOriginPathWithPublictransportMode(originPath, curTime, 5, res);
        return curTime;
    }

    public LocalDateTime sentGetAndCombinationTrajectoryWithDrivingmode(POIs curPOI, POIs nextPOI, Trajectory res, LocalDateTime curTime, String key) throws IOException {
        double distance = Util.getDistance(curPOI.getLng(), curPOI.getLat(), nextPOI.getLng(), nextPOI.getLat());
        String startLL = curPOI.getLng() + "," + curPOI.getLat();
        String endLL = nextPOI.getLng() + "," + nextPOI.getLat();
        String url = "https://restapi.amap.com/v5/direction/" + "driving" + "?parameters&key=" + key + "&origin="
                + startLL + "&destination=" + endLL + "&origin_id=" + curPOI.getPOIID() + "&destination_id" + nextPOI.getPOIID() +
                "&show_fields=cost,polyline";
//            System.out.println("sentGetAndCombinationTrajectory   " + url);
        JSONObject singlePath = Util.sentGet(url);
        OriginPath originPath = getOriginPathFromJSON(singlePath, "driving");
        //Util.outputtheOriginPath(originPath, curTime.getDayOfWeek().toString() + "D" + curTime.getHour() + "H" + curTime.getMinute() + "M" + curTime.getSecond());
        curTime = addPositionFromOriginPath(originPath, curTime, 5, res); //此时curTime为最后一点的时间，即到达时间.
        return curTime;
    }

    public LocalDateTime sentGetAndCombinationTrajectoryWithWalkingmode(POIs curPOI, POIs nextPOI, Trajectory res, LocalDateTime curTime, String key) throws IOException {
        double distance = Util.getDistance(curPOI.getLng(), curPOI.getLat(), nextPOI.getLng(), nextPOI.getLat());
        String startLL = curPOI.getLng() + "," + curPOI.getLat();
        String endLL = nextPOI.getLng() + "," + nextPOI.getLat();
        String url = "https://restapi.amap.com/v5/direction/" + "walking" + "?parameters&key=" + key + "&origin="
                + startLL + "&destination=" + endLL + "&origin_id=" + curPOI.getPOIID() + "&destination_id" + nextPOI.getPOIID() +
                "&show_fields=cost,polyline";
//        this.log.info("sentGetAndCombinationTrajectory " + url);
        JSONObject singlePath = Util.sentGet(url);
        if (singlePath.get("status").toString().equals("0")) {
            return null;
        }
        OriginPath originPath = getOriginPathFromJSON(singlePath, "walking");
        //Util.outputtheOriginPath(originPath, curTime.getDayOfWeek().toString() + "D" + curTime.getHour() + "H" + curTime.getMinute() + "M" + curTime.getSecond());
        curTime = addPositionFromOriginPath(originPath, curTime, 5, res); //此时curTime为最后一点的时间，即到达时间.
        return curTime;
    }

    public LocalDateTime sentGetAndCombinationTrajectoryWithBicyclingmode(POIs curPOI, POIs nextPOI, Trajectory res, LocalDateTime curTime, String key) throws IOException {
        double distance = Util.getDistance(curPOI.getLng(), curPOI.getLat(), nextPOI.getLng(), nextPOI.getLat());
        String startLL = curPOI.getLng() + "," + curPOI.getLat();
        String endLL = nextPOI.getLng() + "," + nextPOI.getLat();
        String url = "https://restapi.amap.com/v5/direction/" + "bicycling" + "?parameters&key=" + key + "&origin="
                + startLL + "&destination=" + endLL + "&origin_id=" + curPOI.getPOIID() + "&destination_id" + nextPOI.getPOIID() +
                "&show_fields=cost,polyline";
//        this.log.info("sentGetAndCombinationTrajectory " + url);
        JSONObject singlePath = Util.sentGet(url);
        if (singlePath.get("status").toString().equals("0")) {
            return null;
        }
        OriginPath originPath = getOriginPathFromJSON(singlePath, "bicycling");
        //Util.outputtheOriginPath(originPath, curTime.getDayOfWeek().toString() + "D" + curTime.getHour() + "H" + curTime.getMinute() + "M" + curTime.getSecond());
        curTime = addPositionFromOriginPath(originPath, curTime, 5, res); //此时curTime为最后一点的时间，即到达时间.
        return curTime;
    }

    public LocalDateTime sentGetAndCombinationTrajectory(POIs curPOI, POIs nextPOI, Trajectory res, int drivingRate, LocalDateTime curTime) throws IOException {
        double distance = Util.getDistance(curPOI.getLng(), curPOI.getLat(), nextPOI.getLng(), nextPOI.getLat());
        boolean isDriving = false;
//        System.out.println("距离：" + distance);
        if (distance / 2000.0 > 1) {
            if (drivingRate > rand.nextInt(10)) {
                isDriving = true;
            }
        } else {
            double a = 10 - distance / 2000.0 * 10;
            if (drivingRate > a + rand.nextInt(10)) {
                isDriving = true;
            }
        }
        String startLL = curPOI.getLng() + "," + curPOI.getLat();
        String endLL = nextPOI.getLng() + "," + nextPOI.getLat();
        String mode = isDriving ? "driving" : "walking";
        String url = "https://restapi.amap.com/v5/direction/" + mode + "?parameters&key=192b951ff8bc56e05cb476f8740a760c&origin="
                + startLL + "&destination=" + endLL + "&origin_id=" + curPOI.getPOIID() + "&destination_id" + nextPOI.getPOIID() +
                "&show_fields=cost,polyline";
//        this.log.info("sentGetAndCombinationTrajectory " + url);
        JSONObject singlePath = Util.sentGet(url);
        OriginPath originPath = getOriginPathFromJSON(singlePath, mode);
        //Util.outputtheOriginPath(originPath, curTime.getDayOfWeek().toString() + "D" + curTime.getHour() + "H" + curTime.getMinute() + "M" + curTime.getSecond());
        curTime = addPositionFromOriginPath(originPath, curTime, 5, res); //此时curTime为最后一点的时间，即到达时间.
        return curTime;
    }



    public static void RemoveRepeatNum(Trajectory trajectory) {
        for (int i = 0; i < trajectory.path.size() - 1; i++) {
            LocalDateTime t = trajectory.timeLine.get(i);
            for (int j = i + 1; j < trajectory.path.size(); j++) {
                if (trajectory.timeLine.get(j).compareTo(t) == 0) {
                    trajectory.timeLine.remove(j);
                    trajectory.path.remove(j);
                } else {
                    break;
                }
            }
        }
    }
    public static LocalDateTime pathStayForBus(LocalDateTime startTime, LocalDateTime endTime, Position waitPoint, Trajectory res) {
        LocalDateTime pointer = startTime;
        while (startTime.plusSeconds(5).compareTo(endTime.minusSeconds(5)) < 0) {
            res.addPositionWithTimeline(waitPoint, startTime);
            startTime = startTime.plusSeconds(5);
        }
        res.addPositionWithTimeline(waitPoint, endTime);
        return endTime;
    }

    public LocalDateTime pathStayAwhile(LocalDateTime startTime, LocalDateTime endTime, POIs poi, Trajectory res, boolean isKeepStayPoint, int mask) {
        LocalDateTime pointer = startTime;
        if (isKeepStayPoint) {
            while (startTime.plusSeconds(5).compareTo(endTime) < 0) {
                var temp = new Position(poi.getLng(), poi.getLat(), poi.getPOITypeCode());
                temp.setMask(mask);
                temp.setPoiid(poi.getPOIID());
                res.addPositionWithTimeline(temp, startTime);
                startTime = startTime.plusSeconds(5);
            }
        } else {
            // 仅保留开始点和最后一点
            while (startTime.plusSeconds(5).compareTo(endTime) < 0) {
                if (startTime.isEqual(pointer) || startTime.isEqual(endTime)) {
                    var temp = new Position(poi.getLng(), poi.getLat(), poi.getPOITypeCode());
                    temp.setMask(mask);
                    temp.setPoiid(poi.getPOIID());
                    res.addPositionWithTimeline(temp, startTime);
                }
                startTime = startTime.plusSeconds(5);
            }
        }
        return startTime;
    }

    public static LocalDateTime addPositionFromOriginPathWithPublictransportMode(OriginPath originPath, LocalDateTime startTime, int d, Trajectory res) throws IndexOutOfBoundsException{
        int all_Distance = originPath.getDistance(); // 总距离;
        boolean mask = isMask(res);
        int maskRank= 0;
        if(mask){
            maskRank = 1;
        }
        //目前我们假设地铁每5分钟一班、公交车20分钟一班
            for (int i = 0; i < originPath.getSize(); i++) {
                // 对于分段中的每一点，先判断是步行的分段还是公交路线的分段
                if (originPath.getStep_mode().get(i).equals("walking")) {
                    Position startPosition = new Position(originPath.getStep_polyLine().get(i).get(0).getLng(),
                            originPath.getStep_polyLine().get(i).get(0).getLat(),
                            originPath.getStep_polyLine().get(i).get(0).getTypeCode());
                    startPosition.setMask(maskRank);
                    res.addPositionWithTimeline(startPosition, startTime);

                    int stepDur = originPath.getStep_duration().get(i);
                    LocalDateTime stepEndTime = startTime.plusSeconds(stepDur);
                    startTime = startTime.plusSeconds(d);
                    int pointNum = stepDur / d; // 应该插入几个点
                    double step_velocity = originPath.getStep_velocity().get(i);
                    double stepForward = step_velocity * d;
                    var last = startPosition;
                    var stepLong = stepForward;
                    int k = 1; // 第二个点
                    int j = 0;
                    boolean isInsertLast = false;
                    while (j < pointNum) {
                        if (k == originPath.getStep_polyLine().get(i).size() - 1) { // 到了最后一点了
                            double dis = Util.getDistance(last.getLng(), last.getLat(),
                                    originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat());
                            if (dis < stepLong) {
                                var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                                        originPath.getStep_polyLine().get(i).get(k).getLat(),
                                        originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                                lastPosition.setMask(maskRank);
                                startTime = stepEndTime;
                                res.addPositionWithTimeline(lastPosition, startTime); // 插入最后一点
                                startTime = startTime.plusSeconds(d);
                                isInsertLast = true;
                                break;
                            } else {
                                double rate = stepLong / dis;
                                j++;
                                var tempLL = Util.getMidLL(last.getLng(), last.getLat(), originPath.getStep_polyLine().get(i).get(k).getLng(),
                                        originPath.getStep_polyLine().get(i).get(k).getLat(), rate);
                                Position insertPosition = new Position(tempLL.get(0).toString(), tempLL.get(1).toString(), originPath.getStep_mode().get(i));
                                insertPosition.setMask(maskRank);
                                res.addPositionWithTimeline(insertPosition, startTime);
                                startTime = startTime.plusSeconds(d);
                                stepLong = stepForward; // 恢复步长
                                last = insertPosition; // 更新当前位置
                            }
                        } else {
                            double dis = Util.getDistance(last.getLng(), last.getLat(),
                                    originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat());
                            if (dis < stepLong) { // 要移动到下一点
                                stepLong -= dis;
                                last = originPath.getStep_polyLine().get(i).get(k);
                                k++;
                            } else { // 说明此时应在两点间插值
                                double rate = stepLong / dis;
                                j++;
                                var tempLL = Util.getMidLL(last.getLng(), last.getLat(), originPath.getStep_polyLine().get(i).get(k).getLng(),
                                        originPath.getStep_polyLine().get(i).get(k).getLat(), rate);
                                Position insertPosition = new Position(tempLL.get(0).toString(), tempLL.get(1).toString(), originPath.getStep_mode().get(i));
                                insertPosition.setMask(maskRank);
                                res.addPositionWithTimeline(insertPosition, startTime);
                                startTime = startTime.plusSeconds(d);
                                stepLong = stepForward; // 恢复步长
                                last = insertPosition; // 更新当前位置
                            }
                        }
                    }
                    if (!isInsertLast) {
                        var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                                originPath.getStep_polyLine().get(i).get(k).getLat(),
                                originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                        lastPosition.setMask(maskRank);
                        startTime = stepEndTime;
                        res.addPositionWithTimeline(lastPosition, startTime); // 插入最后一点
                        startTime = startTime.plusSeconds(d);
                    }
                    if (i != originPath.getSize() - 1) {
                        String waitType = originPath.getStep_mode().get(i + 1);
                        if (waitType.equals("motro")) {
                            int mins = startTime.getMinute();
                            int secs = startTime.getSecond();
                            LocalDateTime endTime = startTime.plusSeconds(60 - secs).plusMinutes(5 - mins % 5);
                            var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat(),
                                    originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                            lastPosition.setMask(maskRank);
                            startTime = pathStayForBus(startTime, endTime, lastPosition, res);
                        } else {
                            int mins = startTime.getMinute();
                            int secs = startTime.getSecond();
                            LocalDateTime endTime = startTime.plusSeconds(60 - secs).plusMinutes(20 - mins % 20);
                            var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat(),
                                    originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                            lastPosition.setMask(maskRank);
                            startTime = pathStayForBus(startTime, endTime, lastPosition, res);
                        }
                    }
                } else {
                    Position startPosition = new Position(originPath.getStep_polyLine().get(i).get(0).getLng(),
                            originPath.getStep_polyLine().get(i).get(0).getLat(),
                            originPath.getStep_polyLine().get(i).get(0).getTypeCode());
                    startPosition.setMask(maskRank);
                    res.addPositionWithTimeline(startPosition, startTime);
                    int stepDur = originPath.getStep_duration().get(i);
                    LocalDateTime stepEndTime = startTime.plusSeconds(stepDur);
                    startTime = startTime.plusSeconds(d);
                    int pointNum = stepDur / d; // 应该插入几个点
                    double step_velocity = originPath.getStep_velocity().get(i);
                    double stepForward = step_velocity * d;

                    var last = startPosition;
                    var stepLong = stepForward;
                    int k = 1; // 第二个点
                    int j = 0;
                    boolean isInsertLast = false;
                    while (j < pointNum) {
                        if (k == originPath.getStep_polyLine().get(i).size() - 1) { // 到了最后一点了
                            double dis = Util.getDistance(last.getLng(), last.getLat(),
                                    originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat());
                            if (dis < stepLong) {
                                var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                                        originPath.getStep_polyLine().get(i).get(k).getLat(),
                                        originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                                lastPosition.setMask(maskRank);
                                startTime = stepEndTime;
                                res.addPositionWithTimeline(lastPosition, startTime); // 插入最后一点
                                startTime = startTime.plusSeconds(d);
                                isInsertLast = true;
                                break;
                            } else {
                                double rate = stepLong / dis;
                                j++;
                                var tempLL = Util.getMidLL(last.getLng(), last.getLat(), originPath.getStep_polyLine().get(i).get(k).getLng(),
                                        originPath.getStep_polyLine().get(i).get(k).getLat(), rate);
                                Position insertPosition = new Position(tempLL.get(0).toString(), tempLL.get(1).toString(), originPath.getStep_mode().get(i));
                                insertPosition.setMask(maskRank);
                                res.addPositionWithTimeline(insertPosition, startTime);
                                startTime = startTime.plusSeconds(d);
                                stepLong = stepForward; // 恢复步长
                                last = insertPosition; // 更新当前位置
                            }
                        } else {
                            double dis = Util.getDistance(last.getLng(), last.getLat(),
                                    originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat());
                            if (dis < stepLong) { // 要移动到下一点
                                stepLong -= dis;
                                last = originPath.getStep_polyLine().get(i).get(k);
                                k++;
                            } else { // 说明此时应在两点间插值
                                double rate = stepLong / dis;
                                j++;
                                var tempLL = Util.getMidLL(last.getLng(), last.getLat(), originPath.getStep_polyLine().get(i).get(k).getLng(),
                                        originPath.getStep_polyLine().get(i).get(k).getLat(), rate);
                                Position insertPosition = new Position(tempLL.get(0).toString(), tempLL.get(1).toString(), originPath.getStep_mode().get(i));
                                insertPosition.setMask(maskRank);
                                res.addPositionWithTimeline(insertPosition, startTime);
                                startTime = startTime.plusSeconds(d);
                                stepLong = stepForward; // 恢复步长
                                last = insertPosition; // 更新当前位置
                            }
                        }
                    }
                    if (!isInsertLast) {
                        var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                                originPath.getStep_polyLine().get(i).get(k).getLat(),
                                originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                        lastPosition.setMask(maskRank);
                        startTime = stepEndTime;
                        res.addPositionWithTimeline(lastPosition, startTime); // 插入最后一点
                        startTime = startTime.plusSeconds(d);
                    }
                }
            }
        return startTime;
    }

    public static boolean isMask(Trajectory tra){
        if(Util.rand.nextInt(10) > tra.getMaskRate()){
            return false;
        }
        return true;
    }

    public static LocalDateTime addPositionFromOriginPath(OriginPath originPath, LocalDateTime startTime, int d, Trajectory res) { // d = 5;
        int all_Distance = originPath.getDistance(); // 总距离;
        boolean mask = isMask(res);
        int maskRank= 0;
        if(mask){
            maskRank = 1;
        }
        for (int i = 0; i < originPath.getSize(); i++) { // 对每分段轨迹进行取点，建立符合gps规律的数据
            //先插入第一个点
            Position startPosition = new Position(originPath.getStep_polyLine().get(i).get(0).getLng(),
                    originPath.getStep_polyLine().get(i).get(0).getLat(),
                    originPath.getStep_polyLine().get(i).get(0).getTypeCode());
            startPosition.setMask(maskRank);
            res.addPositionWithTimeline(startPosition, startTime);
            int stepDur = originPath.getStep_duration().get(i); //获得当前分段的总耗时
            LocalDateTime stepEndTime = startTime.plusSeconds(stepDur);
            startTime = startTime.plusSeconds(d);
            int pointNumber = stepDur / d; // 获取当前分段理应插几个点
            double temp_velocity = originPath.getStep_velocity().get(i); // 得到分段的平均速度 m / s
            double stepForward = temp_velocity * d; // 得到每d秒理应前进的距离,即步长
            if (originPath.getStep_polyLine().get(i).size() - 2 == pointNumber) { // 说明当前分段有的点和理应插入的点个数相同，那么直接插到底
                for (int j = 1; j < originPath.getStep_polyLine().get(i).size(); j++) { // 跳过已经插入的第一个点
                    var tempPosition = new Position(originPath.getStep_polyLine().get(i).get(j).getLng(),
                            originPath.getStep_polyLine().get(i).get(j).getLat(),
                            originPath.getMode());
                    tempPosition.setMask(maskRank);
                    if (originPath.getStep_polyLine().get(i).size() - 1 == j) { // 到了分段的最后一点
                        res.addPositionWithTimeline(tempPosition, stepEndTime);
                        startTime = stepEndTime.plusSeconds(d);
                        break;
                    } else {
                        res.addPositionWithTimeline(tempPosition, startTime);
                        startTime = startTime.plusSeconds(d);
                    }
                }
            } else { // 如果不相同，则需要进行插值计算
                var last = startPosition;
                var stepLong = stepForward;
                int k = 1;
                int j = 0;
                boolean isInsertLast = false;
                while (j < pointNumber) {
                    if (k == originPath.getStep_polyLine().get(i).size() - 1) { // 到了最后一点了
                        double dis = Util.getDistance(last.getLng(), last.getLat(),
                                originPath.getStep_polyLine().get(i).get(k).getLng(),
                                originPath.getStep_polyLine().get(i).get(k).getLat());
                        if (dis < stepLong) {
                            var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat(),
                                    originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                            lastPosition.setMask(maskRank);
                            startTime = stepEndTime;
                            res.addPositionWithTimeline(lastPosition, startTime); // 插入最后一点
                            startTime = startTime.plusSeconds(d);
                            isInsertLast = true;
                            break;
                        } else {
                            double rate = stepLong / dis;
                            j++;
                            var tempLL = Util.getMidLL(last.getLng(), last.getLat(), originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat(), rate);
                            Position insertPosition = new Position(tempLL.get(0).toString(), tempLL.get(1).toString(), originPath.getMode());
                            insertPosition.setMask(maskRank);
                            res.addPositionWithTimeline(insertPosition, startTime);
                            startTime = startTime.plusSeconds(d);
                            stepLong = stepForward; // 恢复步长
                            last = insertPosition; // 更新当前位置
                        }
                    } else {
                        double dis = Util.getDistance(last.getLng(), last.getLat(),
                                originPath.getStep_polyLine().get(i).get(k).getLng(),
                                originPath.getStep_polyLine().get(i).get(k).getLat());
                        if (dis < stepLong) { // 要移动到下一点
                            stepLong -= dis;
                            last = originPath.getStep_polyLine().get(i).get(k);
                            k++;
                        } else { // 说明此时应在两点间插值
                            double rate = stepLong / dis;
                            j++;
                            var tempLL = Util.getMidLL(last.getLng(), last.getLat(), originPath.getStep_polyLine().get(i).get(k).getLng(),
                                    originPath.getStep_polyLine().get(i).get(k).getLat(), rate);
                            Position insertPosition = new Position(tempLL.get(0).toString(), tempLL.get(1).toString(), originPath.getMode());
                            insertPosition.setMask(maskRank);
                            res.addPositionWithTimeline(insertPosition, startTime);
                            startTime = startTime.plusSeconds(d);
                            stepLong = stepForward; // 恢复步长
                            last = insertPosition; // 更新当前位置
                        }
                    }
                }
                if (!isInsertLast) {
                    var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                            originPath.getStep_polyLine().get(i).get(k).getLat(),
                            originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                    lastPosition.setMask(maskRank);
                    startTime = stepEndTime;
                    res.addPositionWithTimeline(lastPosition, startTime); // 插入最后一点
                    startTime = startTime.plusSeconds(d);
                }
            }
        }
        return startTime;
    }

    public static OriginPath getOriginPathWithPublictransportModeFromJSON(JSONObject jsonObject, String pathMode) {
        OriginPath originPath = new OriginPath();
        originPath.setMode(pathMode);
        JSONObject path = jsonObject.getJSONObject("route").getJSONArray("transits").getJSONObject(0); //取第一种方案
        originPath.setDistance(path.getInteger("distance"));
        JSONArray steps = path.getJSONArray("segments");
        for (int i = 0; i < steps.size(); i++) {
            var step = steps.getJSONObject(i);
            if (step.getJSONObject("walking") != null) {
                var walkingSteps = step.getJSONObject("walking");
                originPath.getStep_distance().add(walkingSteps.getInteger("distance"));
                originPath.getStep_duration().add(walkingSteps.getJSONObject("cost").getInteger("duration"));
                originPath.getStep_velocity().add((double) walkingSteps.getInteger("distance") / (double) walkingSteps.getJSONObject("cost").getInteger("duration")); //添加每step的均速
                originPath.getStep_mode().add("walking");
                var eachSteps = walkingSteps.getJSONArray("steps");
                List<Position> step_polyline = new ArrayList<>();
                for (int j = 0; j < eachSteps.size(); j++) {
                    var oneStep = eachSteps.getJSONObject(j);
                    var tempPolyline = oneStep.getJSONObject("polyline").getString("polyline");
                    String[] tempEachPosition = tempPolyline.split(";");
                    if (j != 0) {
                        for (int k = 0; k < tempEachPosition.length; k++) {
                            if (k == 0) continue;
                            String t_Lng = tempEachPosition[k].split(",")[0];
                            String t_Lat = tempEachPosition[k].split(",")[1];
                            String mode = "walking";
                            step_polyline.add(new Position(t_Lng, t_Lat, mode));
                        }
                    } else {
                        for (int k = 0; k < tempEachPosition.length; k++) {
                            String t_Lng = tempEachPosition[k].split(",")[0];
                            String t_Lat = tempEachPosition[k].split(",")[1];
                            String mode = "walking";
                            step_polyline.add(new Position(t_Lng, t_Lat, mode));
                        }
                    }
                }
                originPath.getStep_polyLine().add(step_polyline);
                originPath.setSize(originPath.getSize() + 1);
            }
            if (step.getJSONObject("bus") != null) {
                var busSteps = step.getJSONObject("bus").getJSONArray("buslines").getJSONObject(0);
                String step_type = busSteps.getString("type");
                String step_mode = "sub";
                if (step_type.equals("普通公交线路")) {
                    step_mode = "bus";
                } else {
                    step_mode = "motro";
                }
                originPath.getStep_distance().add(busSteps.getInteger("distance"));
                originPath.getStep_duration().add(busSteps.getJSONObject("cost").getInteger("duration"));
                originPath.getStep_velocity().add((double) busSteps.getInteger("distance") / (double) busSteps.getJSONObject("cost").getInteger("duration")); //添加每step的均速
                originPath.getStep_mode().add(step_mode);
                List<Position> step_Polyline = new ArrayList<>();
                String[] eachPositionString = busSteps.getJSONObject("polyline").getString("polyline").split(";");
                for (var position : eachPositionString) {
                    String t_Lng = position.split(",")[0];
                    String t_Lat = position.split(",")[1];
                    step_Polyline.add(new Position(t_Lng, t_Lat, step_mode));
                }
                originPath.getStep_polyLine().add(step_Polyline);
                originPath.setSize(originPath.getSize() + 1);
            }
        }
        return originPath;
    }

    public static OriginPath getOriginPathFromJSON(JSONObject jsonObject, String pathMode) {
        OriginPath originPath = new OriginPath();
        //System.out.println(jsonObject.toString());
        JSONObject path = jsonObject.getJSONObject("route").getJSONArray("paths").getJSONObject(0); //取第一个轨迹
        originPath.setDistance(path.getInteger("distance"));
        originPath.setMode(pathMode);
        //System.out.println("getOriginPathFromJSON  " + path);
        //originPath.setDuration(path.getJSONObject("cost").getInteger("duration"));
        JSONArray steps = path.getJSONArray("steps");
        for (int i = 0; i < steps.size(); i++) {
            var step = steps.getJSONObject(i);
            originPath.getStep_distance().add(step.getInteger("step_distance")); //添加每step的路程
            originPath.getStep_duration().add(step.getJSONObject("cost").getInteger("duration")); //添加每step的耗时
            originPath.getStep_velocity().add((double) step.getInteger("step_distance") / (double) step.getJSONObject("cost").getInteger("duration")); //添加每step的均速
            String tempPolyline = step.getString("polyline");
            String[] tempEachPosition = tempPolyline.split(";");
            List<Position> step_Polyline = new ArrayList<>();
            for (var position : tempEachPosition) {
                String t_Lng = position.split(",")[0];
                String t_Lat = position.split(",")[1];
                step_Polyline.add(new Position(t_Lng, t_Lat, pathMode));
            }
            originPath.getStep_polyLine().add(step_Polyline); //添加轨迹线
            originPath.setSize(originPath.getSize() + 1);
        }
        return originPath;
    }


    @Override
    public void getTrajectoryAndSimulation(List<Trajectory> trajectories, int R0, String startT, String endT, int poiInfectedFactor,
                                           int infectedInCommuteFactor,
                                           int virusNumExp, int virusNumVar, int infectedHoursExp, int infectedHoursVar,
                                           int recoverHoursExp, int recoverHoursVar) {
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        double virusS = 0.5;
        var startTime = LocalDateTime.parse(startT, dateFormatter);
        var endTime = LocalDateTime.parse(endT, dateFormatter);
        var trajectoriesSize = trajectories.size();
        List<LocalDateTime> eachHours = new ArrayList<>();
        List<List<Integer>> eachHoursState = new ArrayList<>();
        List<Integer> latestPosition = new ArrayList<>();
        for (int i = 0; i < trajectoriesSize; i++) {
            latestPosition.add(0);
        }
        for (int i = 0; i < trajectoriesSize; i++) { //init
//            int virusNum = Util.getNormalRand(5,3);
//            if(rand.nextInt(100) > 98){
//                virusNum = Util.getNormalRand(50,30);
//            }
            trajectories.get(i).setVirusNum(0);
            trajectories.get(i).setId(i);
            trajectories.get(i).setState(Trajectory.State.S);
            latestPosition.add(0);
        }
        LocalDateTime curTime = startTime;
        getSensiableToExposureAtfirst(trajectories.get(2), curTime, virusNumExp, virusNumVar, infectedHoursExp, infectedHoursVar, recoverHoursExp, recoverHoursVar);
        getSensiableToExposureAtfirst(trajectories.get(3), curTime, virusNumExp, virusNumVar, infectedHoursExp, infectedHoursVar, recoverHoursExp, recoverHoursVar);
        getSensiableToExposureAtfirst(trajectories.get(10), curTime, virusNumExp, virusNumVar, infectedHoursExp, infectedHoursVar, recoverHoursExp, recoverHoursVar);
        getSensiableToExposureAtfirst(trajectories.get(20), curTime, virusNumExp, virusNumVar, infectedHoursExp, infectedHoursVar, recoverHoursExp, recoverHoursVar);
        getSensiableToExposureAtfirst(trajectories.get(100), curTime, virusNumExp, virusNumVar, infectedHoursExp, infectedHoursVar, recoverHoursExp, recoverHoursVar);

        List<Integer> aroundInfectedTra;
        List<Integer> unInfectedIdList;
        List<Integer> infectedIdList;
        while (curTime.compareTo(endTime) < 0) {
            //模拟
            unInfectedIdList = new ArrayList<>();
            infectedIdList = new ArrayList<>();
            // 更新所有感染状态
            for (int i = 0; i < trajectoriesSize; i++) {
                var tra = trajectories.get(i);
                if (trajectories.get(i).state == Trajectory.State.S) {
                    unInfectedIdList.add((trajectories.get(i).getId()));
                } else if (tra.state == Trajectory.State.E) {
                    if (tra.infectedTime.compareTo(curTime) < 0) {
                        //说明到达发病时间了
                        if (tra.isAsym == true) {
                            tra.state = Trajectory.State.A;
                        } else {
                            tra.state = Trajectory.State.I;
                        }
                        infectedIdList.add(tra.getId());
                    } else {
                        infectedIdList.add(tra.getId());
                    }
                } else if (tra.state == Trajectory.State.A || tra.state == Trajectory.State.I) {
                    if (tra.recoverTime.compareTo(curTime) < 0) {
                        tra.state = Trajectory.State.R;
                        tra.isVaccines = "B";
                        unInfectedIdList.add(tra.getId());
                    } else {
                        infectedIdList.add(tra.getId());
                    }
                } else {
                    // 说明是康复者
                    unInfectedIdList.add(tra.getId());
                }
            }
            int unInfectedidListSize = unInfectedIdList.size();
            int infectedidListSize = infectedIdList.size();
            // 模拟通勤传染
            for (int i = 0; i < unInfectedidListSize; i++) {
                Trajectory temp = trajectories.get(unInfectedIdList.get(i));
                if (temp.path.get(latestPosition.get(temp.id)).isInPoi()) {
                } else {
                    if (temp.path.get(latestPosition.get(temp.id)).typeCode.equals("driving")) {
                        //驾车的人不考虑传染，因为我们假设车内只有一人.
                    } else {
                        aroundInfectedTra = getAroundTraInCommute(trajectories, temp, infectedIdList, latestPosition);
                        int aroundInfectedTraSize = aroundInfectedTra.size();
                        if (aroundInfectedTra.isEmpty()) {
                            //如果没有，说明周围已经没有感染者了，需要进行判定。
                            if (temp.isInContacted == true) {
                                if (isInfectedInCommute(temp, curTime, latestPosition, infectedInCommuteFactor)) {
                                    infectedInCommute(trajectories, temp, virusS, virusNumExp, virusNumVar,
                                            infectedHoursExp, infectedHoursVar, recoverHoursExp, recoverHoursVar);
                                    temp.infectedPositionType = temp.path.get(latestPosition.get(temp.id)).typeCode;
                                } else {
                                    temp.isInContacted = false;
                                    temp.virusNumSumInContact = 0;
                                }
                            } else {
                                continue;
                            }
                        } else { // 如果有，则需要计算当前时间的周围的接触强度
                            if (temp.isInContacted == false) {
                                //说明是新的一次接触
                                temp.isInContacted = true;
                                temp.lastContactTime = curTime;
                                double virusNumSum = 0;
                                double maxVirusNum = temp.maxVirusNumInContact;
                                for (int j = 0; j < aroundInfectedTraSize; j++) {
                                    Trajectory infectedTra = trajectories.get(aroundInfectedTra.get(j));
                                    double tempVirusNum = infectedTra.virusNum;
                                    if (Util.rand.nextInt(10) > infectedTra.maskRate) {
                                        tempVirusNum *= (double) Util.getNormalRand(30, 15) / 100.0;
                                    }
                                    if (maxVirusNum < tempVirusNum) {
                                        maxVirusNum = tempVirusNum;
                                        temp.maxVirusNumInContact = tempVirusNum;
                                        temp.maxVirusNumIdInContact = aroundInfectedTra.get(j);
                                    }
                                    virusNumSum += tempVirusNum;
                                }
                                temp.virusNumSumInContact += virusNumSum;
                            } else {
                                double virusNumSum = 0;
                                double maxVirusNum = temp.maxVirusNumInContact;
                                for (int j = 0; j < aroundInfectedTraSize; j++) {
                                    Trajectory infectedTra = trajectories.get(aroundInfectedTra.get(j));
                                    double tempVirusNum = infectedTra.virusNum;
                                    if (Util.rand.nextInt(10) > infectedTra.maskRate) {
                                        tempVirusNum *= (double) Util.getNormalRand(30, 15) / 100.0;
                                    }
                                    if (maxVirusNum < tempVirusNum) {
                                        maxVirusNum = tempVirusNum;
                                        temp.maxVirusNumInContact = tempVirusNum;
                                        temp.maxVirusNumIdInContact = aroundInfectedTra.get(j);
                                    }
                                    virusNumSum += tempVirusNum;
                                }
                                temp.virusNumSumInContact += virusNumSum;
                            }

                        }
                    }
                }
            }
            // 模拟poi传染
            for (int i = 0; i < infectedidListSize; i++) {
                Trajectory temp = trajectories.get(infectedIdList.get(i));
                if (temp.path.get(latestPosition.get(infectedIdList.get(i))).isInPoi()) {
                    if (temp.isInPoiContacted) {
                        continue;
                    } else {
                        temp.isInPoiContacted = true;
                        temp.lastContactTimeInPoi = curTime;
                        temp.lastPoiid = temp.path.get(latestPosition.get(infectedIdList.get(i))).poiid;
                    }
                } else {
                    if (temp.isInPoiContacted) {
                        //说明刚刚离开poi，需要进行判断
                        temp.isInPoiContacted = false;
                        String poiid = temp.lastPoiid;
                        temp.lastPoiid = null;
                        List<Integer> aroundTraInPoi = new ArrayList<>();
                        for (int j = 0; j < trajectoriesSize; j++) {
                            if (trajectories.get(j).getPath().get(latestPosition.get(j)).isInPoi() &&
                                    poiid.equals(trajectories.get(j).getPath().get(latestPosition.get(j)).getPoiid())) {
                                aroundTraInPoi.add(j);
                            }
                        }
                        if (aroundTraInPoi.size() > 6) { // 如果大于6人，则要从中选择6个人作为交互对象
                            this.log.info("大于6人，选择6人进行判断");
                            Set<Integer> choosenIndex = Util.getRandomsNoRepeat(0, aroundTraInPoi.size() - 1, 6);
                            List<Integer> interationTra = new ArrayList<>();
                            for (Integer s : choosenIndex) {
                                interationTra.add(aroundTraInPoi.get(s));
                            }
                            List<Integer> unInfectedTraidInPoi = new ArrayList<>();
                            List<Integer> infectedTraidInPoi = new ArrayList<>();
                            infectedTraidInPoi.add(temp.getId());
                            for (int j = 0; j < interationTra.size(); j++) {
                                if (trajectories.get(interationTra.get(j)).state == Trajectory.State.S || trajectories.get(interationTra.get(j)).state == Trajectory.State.R) {
                                    unInfectedTraidInPoi.add(interationTra.get(j));
                                } else {
                                    infectedTraidInPoi.add(interationTra.get(j));
                                }
                            }
                            if (unInfectedTraidInPoi.size() > 0) {
                                double maxVirusNum = -1;
                                double virusNumSum = 0;
                                int maxVirusNumId = -1;
                                for (int j = 0; j < infectedTraidInPoi.size(); j++) {
                                    Trajectory infectedTra = trajectories.get(infectedTraidInPoi.get(j));
                                    double tempVirusNum = infectedTra.virusNum;
                                    if (Util.rand.nextInt(10) > infectedTra.maskRate) {
                                        tempVirusNum *= (double) Util.getNormalRand(30, 15) / 100.0;
                                    }
                                    if (maxVirusNum < tempVirusNum) {
                                        maxVirusNum = tempVirusNum;
                                        maxVirusNumId = infectedTraidInPoi.get(j);
                                    }
                                    virusNumSum += tempVirusNum;
                                }
                                if (maxVirusNumId != -1) {
                                    simulationInPOI(trajectories, curTime, latestPosition, unInfectedTraidInPoi, virusNumSum, maxVirusNumId, virusS, poiInfectedFactor,
                                            virusNumExp, virusNumVar, infectedHoursExp, infectedHoursVar, recoverHoursExp, recoverHoursVar);
                                }
                            }
                        } else {
                            List<Integer> interationTra = new ArrayList<>();
                            for (int j = 0; j < aroundTraInPoi.size(); j++) {
                                interationTra.add(aroundTraInPoi.get(j));
                            }
                            List<Integer> unInfectedTraidInPoi = new ArrayList<>();
                            List<Integer> infectedTraidInPoi = new ArrayList<>();
                            infectedTraidInPoi.add(temp.getId());
                            for (int j = 0; j < interationTra.size(); j++) {
                                if (trajectories.get(interationTra.get(j)).state == Trajectory.State.S || trajectories.get(interationTra.get(j)).state == Trajectory.State.R) {
                                    unInfectedTraidInPoi.add(interationTra.get(j));
                                } else {
                                    infectedTraidInPoi.add(interationTra.get(j));
                                }
                            }
                            if (unInfectedTraidInPoi.size() > 0) {
                                double maxVirusNum = -1;
                                double virusNumSum = 0;
                                int maxVirusNumId = -1;
                                for (int j = 0; j < infectedTraidInPoi.size(); j++) {
                                    Trajectory infectedTra = trajectories.get(infectedTraidInPoi.get(j));
                                    double tempVirusNum = infectedTra.virusNum;
                                    if (Util.rand.nextInt(10) > infectedTra.maskRate) {
                                        tempVirusNum *= (double) Util.getNormalRand(30, 15) / 100.0;
                                    }
                                    if (maxVirusNum < tempVirusNum) {
                                        maxVirusNum = tempVirusNum;
                                        maxVirusNumId = infectedTraidInPoi.get(j);
                                    }
                                    virusNumSum += tempVirusNum;
                                }
                                if (maxVirusNumId != -1) {
                                    simulationInPOI(trajectories, curTime, latestPosition, unInfectedTraidInPoi, virusNumSum, maxVirusNumId, virusS, poiInfectedFactor,
                                            virusNumExp, virusNumVar, infectedHoursExp, infectedHoursVar, recoverHoursExp, recoverHoursVar);
                                }
                            }
                        }
                    }
                }
            }
            // 更新位置
            for (int i = 0; i < trajectoriesSize; i++) {
                int timelineSize = trajectories.get(i).timeLine.size();
                if (latestPosition.get(i) + 1 < timelineSize) {
                    var nextPoistionTime = trajectories.get(i).timeLine.get(latestPosition.get(i) + 1);
                    if (nextPoistionTime != null && nextPoistionTime.compareTo(curTime) < 0) {
                        latestPosition.set(i, latestPosition.get(i) + 1); // 更新当前时间的位置.
                    }
                }
            }
            // 留下记录
            if (curTime.getSecond() == 0 && curTime.getMinute() == 0) {
                List<Integer> tempres = new ArrayList<>();
                int walkingNum = 0;
                int motroNum = 0;
                int busNum = 0;
                int bicyclingNum = 0;
                int poiNum = 0;
                int sNumber = 0;
                int eNumber = 0;
                int iNumber = 0;
                int aNumber = 0;
                int rNumber = 0;
                for (int i = 0; i < trajectoriesSize; i++) {
                    var tra = trajectories.get(i);
                    if (tra.getState() == Trajectory.State.S) {
                        sNumber++;
                    } else if (tra.getState() == Trajectory.State.E) {
                        eNumber++;
                    } else if (tra.getState() == Trajectory.State.I) {
                        iNumber++;
                    } else if (tra.getState() == Trajectory.State.A) {
                        aNumber++;
                    } else {
                        rNumber++;
                    }
                    if (tra.infectedPositionType != null) {
                        if (tra.infectedPositionType.equals("walking")) {
                            walkingNum++;
                        } else if (tra.infectedPositionType.equals("motro")) {
                            motroNum++;
                        } else if (tra.infectedPositionType.equals("bus")) {
                            busNum++;
                        } else if (tra.infectedPositionType.equals("bicycling")) {
                            bicyclingNum++;
                        } else {
                            poiNum++;
                        }
                    }
                }
                eachHours.add(curTime);
                tempres.add(sNumber);
                tempres.add(eNumber);
                tempres.add(iNumber);
                tempres.add(aNumber);
                tempres.add(rNumber);
                tempres.add(walkingNum);
                tempres.add(motroNum);
                tempres.add(busNum);
                tempres.add(bicyclingNum);
                tempres.add(poiNum);
                eachHoursState.add(tempres);
            }
            curTime = curTime.plusSeconds(1);
        }
        try {
            int r0Sum = 0;
            int infectedNum = 0;
            for (int i = 0; i < trajectories.size(); i++) {
                var tt = trajectories.get(i);
                if (tt.infectedNum > 0) {
                    r0Sum += tt.infectedNum;
                    infectedNum++;
                }
            }
            double r0 = (double) r0Sum * 1.0 / infectedNum;
            Util.outputSimulationResult(eachHours, eachHoursState, r0);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public List<Integer> getAroundTraInCommute(List<Trajectory> list, Trajectory cur, List<Integer> infectedTraIdList, List<Integer> lastPosition) {
        String curLng = cur.getPath().get(lastPosition.get(cur.getId())).getLng();
        String curLat = cur.getPath().get(lastPosition.get(cur.getId())).getLat();
        List<Integer> res = new ArrayList<>();
        int size = infectedTraIdList.size();
        for (int i = 0; i < size; i++) {
            // 判断通勤时的周围的人
            if (!list.get(infectedTraIdList.get(i)).getPath().get(lastPosition.get(infectedTraIdList.get(i))).isInPoi() && !list.get(infectedTraIdList.get(i)).getPath().get(lastPosition.get(infectedTraIdList.get(i))).typeCode.equals("driving") &&
                    Util.getDistance(curLng, curLat, list.get(infectedTraIdList.get(i)).getPath().get(lastPosition.get(infectedTraIdList.get(i))).getLng(),
                            list.get(infectedTraIdList.get(i)).getPath().get(lastPosition.get(infectedTraIdList.get(i))).getLat()) < 20.0
            ) {
                res.add(infectedTraIdList.get(i));
            }
        }
        return res;
    }

    public boolean isInfectedInCommute(Trajectory temp, LocalDateTime curTime, List<Integer> latestPosition, int infectedInCommuteFactor) {
        Duration duration = Duration.between(temp.lastContactTime, curTime);
        long seconds = duration.toSeconds();
        //计算感染概率
        double avgVirusNUm = temp.virusNumSumInContact;
        double mask = 1;
        double vc = 1;
        if (temp.isVaccines.equals("A")) {
            vc = 0.5;
        }
        if (temp.isVaccines.equals("B")) {
            vc = 0.25;
        }
        if (Util.rand.nextInt(10) < temp.maskRate) {
            mask = 0.75;
        }
        double rate = 1 - Math.exp(-(avgVirusNUm * mask * vc) / infectedInCommuteFactor);
        this.log.info("进行通勤感染判断，当前的轨迹id是：" + temp.getId() + "时间是：" + curTime.toString() + "当前的接触时间:" + Duration.between(temp.lastContactTime, curTime).getSeconds() +
                "总病毒量是：" + temp.virusNumSumInContact + "感染的概率是：" + rate + " 状态为：" + temp.path.get(latestPosition.get(temp.getId())).typeCode + "  被id:" + temp.maxVirusNumIdInContact);
        if (Math.random() < rate) {
            return true;
        } else return false;
    }

    public double InfectedInCommuteRate(Trajectory temp, LocalDateTime curTime, int infectedInCommuteFactor) {
        Duration duration = Duration.between(temp.lastContactTime, curTime);
        long seconds = duration.toSeconds();
        //计算感染概率
        double avgVirusNUm = temp.virusNumSumInContact;
        double mask = 1;
        double vc = 1;
        if (temp.isVaccines.equals("A")) {
            vc = 0.5;
        }
        if (Util.rand.nextInt(10) < temp.maskRate) {
            mask = 0.75;
        }
        double rate = 1 - Math.exp(-(avgVirusNUm * mask * vc) / infectedInCommuteFactor);
        return rate;
    }

    public double getProcessionRate(double virus, Trajectory temp) {
        double res = 1;
        if (temp.age < 20) {
            res *= 0.5;
        } else if (temp.age < 40) {
            res *= 0.6;
        } else if (temp.age < 60) {
            res *= 0.7;
        } else if (temp.age < 80) {
            res *= 0.9;
        }
        if (temp.isVaccines.equals("A")) {
            res *= 0.35;
        }
        if (temp.isVaccines.equals("B")) {
            res *= 0.25;
        }
        return res * virus;
    }

    public void addHomeInformationAndInit(Trajectory res, String poiid) {
        POIs home = poIsService.findPOIByID(poiid);
        res.homeLng = home.getLng();
        res.homeLat = home.getLat();
        res.homeName = home.getPOIName();
    }

    public void getSensiableToExposureAtfirst(Trajectory temp, LocalDateTime curTime,
                                              int virusNumExp, int virusNumVar, int infectedHoursExp, int infectedHoursVar,
                                              int recoverHoursExp, int recoverHoursVar) {
        temp.state = Trajectory.State.E;
        temp.virusNum = 30;
        temp.infectedBy = -1;
        temp.exposureTime = curTime;
        long hours = Util.getNormalRand(infectedHoursExp, infectedHoursVar);
        long recoverHours = Util.getNormalRand(recoverHoursExp, recoverHoursVar); // 170 96
        recoverHours = Math.round(recoverHours * getRateWithAgeForRecoverTime(temp.age));
        temp.infectedTime = temp.exposureTime.plusHours(hours);
        temp.recoverTime = temp.infectedTime.plusHours(recoverHours);
        double rate = 1;
        rate = getProcessionRate(0.5, temp);
        if (Math.random() < rate) {
            temp.isAsym = false;
        } else temp.isAsym = true;
        temp.isInContacted = false;
    }

    public double getRateWithAgeForRecoverTime(int age) {
        double rate = 1;
        int t = Util.getNormalRand(50, 30);
        if (age > 80) {
            if (t > 60) {
                rate = rate + rand.nextDouble() / 1.5;
            }
        } else if (age > 50) {
            if (t > 75) {
                rate = rate + rand.nextDouble() / 3.0;
            }
        } else {
            return 1;
        }
        return rate;
    }

    public String simulationInPOIisInfected(Trajectory t, double rate, LocalDateTime curTime, int maxVirusNumId, double virusS,
                                            int virusNumExp, int virusNumVar, int infectedHoursExp, int infectedHoursVar,
                                            int recoverHoursExp, int recoverHoursVar) {
        // 1 == 感染且发病 2 == 感染不发病 3 == 不感染
        if (Math.random() < rate) {
            t.state = Trajectory.State.E;
            t.infectedBy = maxVirusNumId;
            t.exposureTime = curTime;
            t.isExposureInPoi = true;
            t.virusNum = Util.getNormalRand(virusNumExp, virusNumVar);
            long hours = Util.getNormalRand(infectedHoursExp, infectedHoursVar);
            long recoverHours = Util.getNormalRand(recoverHoursExp, recoverHoursVar);
            recoverHours = Math.round(recoverHours * getRateWithAgeForRecoverTime(t.age));
            t.infectedTime = t.exposureTime.plusHours(hours);
            t.recoverTime = t.infectedTime.plusHours(recoverHours);
            double r = 1;
            r = getProcessionRate(virusS, t);
            if (Math.random() < r) {
                t.isAsym = false;
                return "1";
            } else {
                t.isAsym = true;
                return "2";
            }
        }
        return "3";
    }

    public void infectedInCommute(List<Trajectory> trajectories, Trajectory temp, double virusS,
                                  int virusNumExp, int virusNumVar, int infectedHoursExp, int infectedHoursVar,
                                  int recoverHoursExp, int recoverHoursVar) {
        temp.state = Trajectory.State.E;
        temp.infectedBy = temp.maxVirusNumIdInContact;
        trajectories.get(temp.maxVirusNumIdInContact).infectedNum++;
        temp.exposureTime = temp.lastContactTime;
        temp.virusNum = Util.getNormalRand(virusNumExp, virusNumVar);
        long hours = Util.getNormalRand(infectedHoursExp, infectedHoursVar);
        long recoverHours = Util.getNormalRand(recoverHoursExp, recoverHoursVar);
        recoverHours = Math.round(recoverHours * getRateWithAgeForRecoverTime(temp.age));
        temp.infectedTime = temp.exposureTime.plusHours(hours);
        temp.recoverTime = temp.infectedTime.plusHours(recoverHours);
        double rate = 1;
        rate = getProcessionRate(virusS, temp);
        if (Math.random() < rate) {
            temp.isAsym = false;
        } else temp.isAsym = true;
        temp.isInContacted = false;
    }

    public void simulationInPOI(List<Trajectory> trajectories, LocalDateTime curTime, List<Integer> latestPosition, List<Integer> unInfectedTraidInPoi, double virusNumSum, int maxVirusNumId, double virusS, int factor,
                                int virusNumExp, int virusNumVar, int infectedHoursExp, int infectedHoursVar,
                                int recoverHoursExp, int recoverHoursVar) {
        for (int j = 0; j < unInfectedTraidInPoi.size(); j++) {
            Trajectory t = trajectories.get(unInfectedTraidInPoi.get(j));
            var max = trajectories.get(maxVirusNumId);
            if (max.lastContactTimeInPoi == null) {
                max.lastContactTimeInPoi = t.lastContactTimeInPoi;
            }
            Duration duration = Duration.between(max.lastContactTimeInPoi, curTime);
            long seconds = duration.toSeconds();
            //计算感染概率
            double avgVirusNUm = virusNumSum;
            double mask = 1;
            double vc = 1;
            if (t.isVaccines.equals("A")) {
                vc = 0.5;
            } else if (t.isVaccines.equals("B")) {
                vc = 0.25;
            }
            if (Util.rand.nextInt(10) > t.maskRate) {
                mask = 0.75;
            }
            double rate = 1 - Math.exp(-1 * avgVirusNUm / factor * seconds * mask * vc);
            String resultString = simulationInPOIisInfected(t, rate, curTime, maxVirusNumId, virusS, virusNumExp,
                    virusNumVar, infectedHoursExp, infectedHoursVar, recoverHoursExp, recoverHoursVar);
            if (resultString.equals("1") || resultString.equals("2")) {
                trajectories.get(maxVirusNumId).infectedNum++;
                trajectories.get(t.id).infectedPositionType = trajectories.get(t.id).path.get(latestPosition.get(t.id)).typeCode;
            }
            this.log.info("进行poi感染判断，当前的轨迹id是：" + t.getId() + "  " + "当前的因素是" + factor + " 接触强度是：" + virusNumSum + " 接触时间为："
                    + seconds + "  感染概率是：" + rate);
        }
    }

    @Override
    public void recordBusFromTrajectories(List<Trajectory> trajectories, String startT, String endT, String fileName) throws IOException {
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var startTime = LocalDateTime.parse(startT, dateFormatter);
        var endTime = LocalDateTime.parse(endT, dateFormatter);
        var curTime = startTime;

        List<List<Integer>> recordBusTra = new ArrayList<>();
        List<LocalDateTime> busTimelines = new ArrayList<>();
        List<List<Integer>> recordMetroTra = new ArrayList<>();
        List<LocalDateTime> metroTimelines = new ArrayList<>();
        List<Integer> latestPositionIndex = new ArrayList<>();
        List<Position> lastPosition = new ArrayList<>();
        List<Position> lastPOI = new ArrayList<>();
        List<LocalDateTime> lastInPOITime = new ArrayList<>();
        for(int i = 0; i < trajectories.size(); i ++){
            latestPositionIndex.add(0);
            lastPosition.add(trajectories.get(i).getPath().get(0));
            lastPOI.add(trajectories.get(i).getPath().get(0));
            lastInPOITime.add(trajectories.get(i).getTimeLine().get(0));
        }
        int size = trajectories.size();
        List<Trajectory> inBusTra = new ArrayList<>();
        List<Trajectory> inMetroTra = new ArrayList<>();
        while(curTime.compareTo(endTime) < 0){
            for(int i =0; i < size; i ++){
                var tra = trajectories.get(i);
                if(tra.getPath().get(latestPositionIndex.get(i)).typeCode.equals("bus")){
                    inBusTra.add(tra);
                }else if(tra.getPath().get(latestPositionIndex.get(i)).typeCode.equals("motro")){
                    inMetroTra.add(tra);
                }
            }
            HashSet<Integer> visitedList = new HashSet<>();
            for(int i =0; i < inMetroTra.size(); i++){
                var tra = inMetroTra.get(i);
                if(visitedList.contains(tra.id)){
                    continue;
                }
                visitedList.add(tra.id);
                List<Integer> sameBus = new ArrayList<>();
                sameBus.add(tra.id);
                for(int j =0; j < inMetroTra.size();j ++){
                    var jtra = inMetroTra.get(j);
                    if(visitedList.contains(jtra.id)){
                        continue;
                    }
                    if(Util.getDistance(tra.path.get(latestPositionIndex.get(tra.id)).lng, tra.path.get(latestPositionIndex.get(tra.id)).lat,
                            jtra.path.get(latestPositionIndex.get(jtra.id)).lng,jtra.path.get(latestPositionIndex.get(jtra.id)).lat) < 30){
                        //说明在同一辆
                        visitedList.add(jtra.id);
                        sameBus.add(jtra.id);
                    }
                }
                if(sameBus.size() > 1){
                    recordMetroTra.add(sameBus);
                    metroTimelines.add(curTime);
                }
            }
            visitedList = new HashSet<>();
            for(int i =0; i < inBusTra.size(); i++){
                var tra = inBusTra.get(i);
                if(visitedList.contains(tra.id)){
                    continue;
                }
                visitedList.add(tra.id);
                List<Integer> sameBus = new ArrayList<>();
                sameBus.add(tra.id);
                for(int j =0; j < inBusTra.size();j ++){
                    var jtra = inBusTra.get(j);
                    if(visitedList.contains(jtra.id)){
                        continue;
                    }
                    if(Util.getDistance(tra.path.get(latestPositionIndex.get(tra.id)).lng, tra.path.get(latestPositionIndex.get(tra.id)).lat,
                            jtra.path.get(latestPositionIndex.get(jtra.id)).lng,jtra.path.get(latestPositionIndex.get(jtra.id)).lat) < 30){
                        //说明在同一辆
                        visitedList.add(jtra.id);
                        sameBus.add(jtra.id);
                    }
                }
                if(sameBus.size() > 1){
                    recordBusTra.add(sameBus);
                    busTimelines.add(curTime);
                }
            }
            curTime = curTime.plusMinutes(10);
            //更新最新位置
            for (int i = 0; i < size; i++) {
                int timelineSize = trajectories.get(i).timeLine.size();
                for(int j = latestPositionIndex.get(i) + 1; j < timelineSize; j++){
                    var nextPositionTime = trajectories.get(i).getTimeLine().get(j);
                    if(nextPositionTime.compareTo(curTime) > 0){
                        break;
                    }
                    if (nextPositionTime != null && nextPositionTime.compareTo(curTime) < 0) {
                        latestPositionIndex.set(i, j); // 更新当前时间的位置.
                    }
                }
            }
            inBusTra.clear();
            inMetroTra.clear();
        }
        List<List<String>> newrecordBusTra = new ArrayList<>();
        List<List<String>> newrecordMetroTra = new ArrayList<>();
        for(int i =0; i < recordBusTra.size();i++){
            var tlist = recordBusTra.get(i);
            List<String> tt = new ArrayList<>();
            for(int j = 0; j < tlist.size();j++){
                tt.add(trajectories.get(tlist.get(j)).getFileName());
            }
            newrecordBusTra.add(tt);
        }
        for(int i =0; i < recordMetroTra.size();i++){
            var tlist = recordMetroTra.get(i);
            List<String> tt = new ArrayList<>();
            for(int j = 0; j < tlist.size();j++){
                tt.add(trajectories.get(tlist.get(j)).getFileName());
            }
            newrecordMetroTra.add(tt);
        }
        Util.outputSameBus(newrecordBusTra, busTimelines,"同一公交车");
        Util.outputSameBus(newrecordMetroTra, metroTimelines,"同一地铁");
    }
    @Override
    public void recordRadiusFromTrajectories(List<Trajectory> trajectories, String startT, String endT, String fileName) throws IOException {
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var startTime = LocalDateTime.parse(startT, dateFormatter);
        var endTime = LocalDateTime.parse(endT, dateFormatter);
        List<Double> radiusList = new ArrayList<>();
        List<List<POIRecord>> poiRecordList = new ArrayList<>();

        for (int i = 0; i < trajectories.size(); i++) {
            var tra = trajectories.get(i);
            List<POIRecord> singleRecord = new ArrayList<>();
            var curTime = startTime;
            Position lastPosition = tra.getPath().get(0);
            int lastTimeIndex = 0;
            LocalDateTime lastInPoiTime = startTime;
            while (curTime.compareTo(endTime) < 0) {
                if (tra.path.get(lastTimeIndex).isInPoi()) {
                    if (lastPosition.isInPoi() == true) {

                    } else {
                        lastPosition.setPoiid(tra.path.get(lastTimeIndex).getPoiid());
                        lastInPoiTime = curTime;
                    }
                } else {
                    if (lastPosition.isInPoi() == true) {
                        // 说明此时刚刚离开poi
                        var departureTime = curTime;
                        var between = Duration.between(lastInPoiTime, departureTime);
                        long stayTime = between.getSeconds();
                        int hourOfDay = lastInPoiTime.getHour();
                        POIRecord record = new POIRecord(lastPosition, lastInPoiTime, departureTime, hourOfDay, stayTime);
                        singleRecord.add(record);
                        lastPosition = tra.path.get(lastTimeIndex);
                    } else {
                        // 说明上一位置是在赶路，忽视
                    }
                }
                int timelineSize = tra.timeLine.size();
                if (lastTimeIndex + 1 < timelineSize) {
                    var nextPoistionTime = tra.timeLine.get(lastTimeIndex + 1);
                    if (nextPoistionTime != null && nextPoistionTime.compareTo(curTime) < 0) {
                        lastTimeIndex = lastTimeIndex + 1;
                    }
                }
                curTime = curTime.plusSeconds(1);
            }
            // 记录回转半径
            poiRecordList.add(singleRecord);

            Position center = Util.getCenterPointFromListOfCoordinates(singleRecord);
            double distanceSum = 0;
            for(int j =0; j < singleRecord.size(); j++){
                var record = singleRecord.get(j);
                distanceSum += Math.pow(Util.getDistance(center.getLng(), center.getLat(),record.getPosition().getLng(), record.getPosition().getLat()), 2);
            }
            distanceSum /= singleRecord.size();
            Double radius = Math.sqrt(distanceSum);
            radiusList.add(radius);
        }
        Util.outputRadiusOfGyration(trajectories, radiusList, fileName);
    }

    public void recordTripFromTrajectories(List<Trajectory> trajectories, String startT, String endT, String fileName) throws IOException{
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var startTime = LocalDateTime.parse(startT, dateFormatter);
        var endTime = LocalDateTime.parse(endT, dateFormatter);

        List<List<Integer>> arrList = new ArrayList<>();
        List<List<Integer>> depList = new ArrayList<>();
        List<LocalDateTime> localDateTimeList = new ArrayList<>();
        var curTime = startTime;
        List<Integer> latestPositionIndex = new ArrayList<>();
        List<Position> lastPosition = new ArrayList<>();
        List<Position> lastPOI = new ArrayList<>();
        List<LocalDateTime> lastInPOITime = new ArrayList<>();
        for(int i = 0; i < trajectories.size(); i ++){
            latestPositionIndex.add(0);
            lastPosition.add(trajectories.get(i).getPath().get(0));
            lastPOI.add(trajectories.get(i).getPath().get(0));
            lastInPOITime.add(trajectories.get(i).getTimeLine().get(0));
        }
        List<Trip> ans = new ArrayList<>();
        int size = trajectories.size();
        while(curTime.compareTo(endTime) < 0){
            // 对于整个时间段，统计数据
            // 先统计数据，再更新位置。
            for(int i = 0; i < size; i ++){
                var tra = trajectories.get(i);
                if (tra.path.get(latestPositionIndex.get(i)).isInPoi()) {
                    if(lastPosition.get(i).isInPoi()){
                        //说明之前已经在poi内部了
                        // 更新位置
                    }else{
                        // 说明是新到达 记录旅程
                        String startType = lastPOI.get(i).getTypeCode();
                        String endType = tra.path.get(latestPositionIndex.get(i)).getTypeCode();
                        Double dis = Util.getDistance(lastPOI.get(i).getLng(), lastPOI.get(i).getLat(),
                                tra.path.get(latestPositionIndex.get(i)).getLng(), tra.path.get(latestPositionIndex.get(i)).getLat());
                        var betweenS = Duration.between(lastInPOITime.get(i), curTime).getSeconds();
                        long mins = betweenS / 60;
                        String patternName = tra.getPatternName();
                        var trip = new Trip(startType, endType, lastInPOITime.get(i),curTime, mins, betweenS,patternName, dis);
                        ans.add(trip);
                    }
                } else {
                    if(lastPosition.get(i).isInPoi()){
                        //说明刚刚离开，需要记录
                        lastInPOITime.set(i, curTime);
                        lastPOI.set(i,lastPosition.get(i));
                    }else {
                        //说明上个时间点仍在赶路，不需要考虑
                    }
                }
                lastPosition.set(i,tra.getPath().get(latestPositionIndex.get(i)));
            }
            //记录
            curTime = curTime.plusMinutes(1);
            //更新最新位置
            for (int i = 0; i < size; i++) {
                int timelineSize = trajectories.get(i).timeLine.size();
                for(int j = latestPositionIndex.get(i) + 1; j < timelineSize; j++){
                    var nextPositionTime = trajectories.get(i).getTimeLine().get(j);
                    if(nextPositionTime.compareTo(curTime) > 0){
                        break;
                    }
                    if (nextPositionTime != null && nextPositionTime.compareTo(curTime) < 0) {
                        latestPositionIndex.set(i, j); // 更新当前时间的位置.
                    }
                }
            }
        }
        Util.outputTrip(ans,"行程");
    }
    public void recordAPListFromTrajectories(List<Trajectory> trajectories, String startT, String endT, String fileName) throws IOException{
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var startTime = LocalDateTime.parse(startT, dateFormatter);
        var endTime = LocalDateTime.parse(endT, dateFormatter);

        List<List<Integer>> arrList = new ArrayList<>();
        List<List<Integer>> depList = new ArrayList<>();
        List<LocalDateTime> localDateTimeList = new ArrayList<>();
        var curTime = startTime;
        List<Integer> latestPositionIndex = new ArrayList<>();
        List<Position> lastPosition = new ArrayList<>();

        for(int i = 0; i < trajectories.size(); i ++){
            latestPositionIndex.add(0);
            lastPosition.add(trajectories.get(i).getPath().get(0));
        }
        int size = trajectories.size();
        while(curTime.compareTo(endTime) < 0){
            // 对于整个时间段，统计数据
            // 先统计数据，再更新位置。
            int ordinaryworkerArr = 0;
            int ordinarworkerDep = 0;
            int overtimeworkerArr = 0;
            int overtimeworkerDep = 0;
            int retireeArr = 0;
            int retireeDep = 0;
            int pupilArr = 0;
            int pupilDep = 0;
            int studentArr = 0;
            int studentDep = 0;
            int freelancerArr = 0;
            int freelancerDep = 0;
            for(int i = 0; i < size; i ++){
                var tra = trajectories.get(i);
                if (tra.path.get(latestPositionIndex.get(i)).isInPoi()) {
                    if(lastPosition.get(i).isInPoi()){
                        //说明之前已经在poi内部了
                        // 更新位置
                    }else{
                        // 说明是新到达
                        if("ordinary worker".equals(tra.getPatternName())){
                            ordinaryworkerArr++;
                        }else if("overtime worker".equals(tra.getPatternName())){
                            overtimeworkerArr++;
                        }else if("retiree".equals(tra.getPatternName())){
                            retireeArr++;
                        }else if("middleschool student".equals(tra.getPatternName())){
                            studentArr++;
                        }else if("pupil".equals(tra.getPatternName())){
                            pupilArr++;
                        }else{
                            freelancerArr++;
                        }
                    }
                } else {
                    if(lastPosition.get(i).isInPoi()){
                        //说明刚刚离开，需要记录
                        if("ordinary worker".equals(tra.getPatternName())){
                            ordinarworkerDep++;
                        }else if("overtime worker".equals(tra.getPatternName())){
                            overtimeworkerDep++;
                        }else if("retiree".equals(tra.getPatternName())){
                            retireeDep++;
                        }else if("middleschoolstudent".equals(tra.getPatternName())){
                            studentDep++;
                        }else if("pupil".equals(tra.getPatternName())){
                            pupilDep++;
                        }else{
                            freelancerDep++;
                        }
                    }else {
                        //说明上个时间点仍在赶路，不需要考虑
                    }
                }
                lastPosition.set(i,tra.getPath().get(latestPositionIndex.get(i)));
            }

            //记录
            List<Integer> tempArrList = new ArrayList<>();
            tempArrList.add(ordinaryworkerArr);
            tempArrList.add(overtimeworkerArr);
            tempArrList.add(retireeArr);
            tempArrList.add(pupilArr);
            tempArrList.add(studentArr);
            tempArrList.add(freelancerArr);
            List<Integer> tempDepList = new ArrayList<>();
            tempDepList.add(ordinarworkerDep);
            tempDepList.add(overtimeworkerDep);
            tempDepList.add(retireeDep);
            tempDepList.add(pupilDep);
            tempDepList.add(studentDep);
            tempDepList.add(freelancerDep);
            arrList.add(tempArrList);
            depList.add(tempDepList);
            localDateTimeList.add(curTime);
            curTime = curTime.plusMinutes(1);
            //更新最新位置
            for (int i = 0; i < size; i++) {
                int timelineSize = trajectories.get(i).timeLine.size();
                for(int j = latestPositionIndex.get(i) + 1; j < timelineSize; j++){
                    var nextPositionTime = trajectories.get(i).getTimeLine().get(j);
                    if(nextPositionTime.compareTo(curTime) > 0){
                        break;
                    }
                    if (nextPositionTime != null && nextPositionTime.compareTo(curTime) < 0) {
                        latestPositionIndex.set(i, j); // 更新当前时间的位置.
                    }
                }
            }
        }
        Util.outputAPInformation(arrList, localDateTimeList, "arr");
        Util.outputAPInformation(depList, localDateTimeList, "dep");
    }
}
