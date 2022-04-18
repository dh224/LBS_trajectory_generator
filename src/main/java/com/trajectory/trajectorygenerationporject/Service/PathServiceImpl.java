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
import org.apache.ibatis.javassist.compiler.ast.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Override
    public Trajectory getTrajectory(String patternName, String cityName, String adName, String startT, String endT, List<Map<Integer, List<Map<String, Integer>>>> pattern, boolean isRestrict, int age, String job, String sex, int maskRate, String Vaccines, int drivingRate, int commutingTimeRate, String index, String key) throws IOException {
        //要求起始值和结束的时间为整小时。
        boolean isKeepStayPoint = false;
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int maxRandomStayTime = 15; //minues;
        var startTime = LocalDateTime.parse(startT, dateFormatter);
        var endTime = LocalDateTime.parse(endT, dateFormatter);
        String cityCode = cityDAO.findCityCodeByAdCode(cityDAO.findAdCodeByCityNameAndAdname(cityName, adName));
        Trajectory res = new Trajectory(patternName, pattern, age, job, sex, 8, Vaccines, drivingRate, startTime, endTime);
        Map<String, POIs> thisPathPOIs = new HashMap<>();
        var curTime = startTime; //设置当前时间，在接下去的循环中会更新它
        int startdayofweek = curTime.getDayOfWeek().getValue() - 1; //初始日期是星期几 由于是list存储，因此需要-1
        var startdayPattern = pattern.get(startdayofweek); //获得初始日的行为模式表
        var needPositions = startdayPattern.get(curTime.getHour()); // 获得当前应该去的所有位置 MAP
        var curPositionTypeMap = needPositions.get(rand.nextInt(needPositions.size()));
        var curPositionType = getTypeCodeByCurMap(curPositionTypeMap);// 设置在起始时间的位置类型.
        String startPOIID = poIsService.findRandomPOIWithCityCodeAndAdCodeAndTypeCode(cityCode, "330102", curPositionType);
        addHomeInformationAndInit(res, startPOIID);
        res.isInPoiContacted = true;
        res.lastContactTimeInPoi = curTime;
        res.lastPoiid = startPOIID;
        POIs startPOI = poIsService.findPOIByID(startPOIID);
//        this.log.info("起始点的位置是:" + startPOI.getPOIName());
        thisPathPOIs.put(curPositionType, poisDAO.findPOIByID(startPOIID)); //添加初始的POI位置,此后的位置或许可以根据此poi的周围随机选择
        while (curTime.compareTo(endTime) < 0) { // 当前时间早于生成终止时间，继续生成
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
                    curTime = pathStayAwhile(curTime, nextHourTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint);
                }
            } else { //需要进行移动.
                //进行随机的停留时间，增加真实性。
                var RTGTime = curTime.plusMinutes(rand.nextInt(maxRandomStayTime));
//                System.out.println(thisPathPOIs + curPositionType);
//                System.out.println("需要进行移动" + curDayPattern.get(curTimeOfHour) + "curpoiType:" + curPositionType);
                curTime = pathStayAwhile(curTime, RTGTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint);
                curTimeOfHour = curTime.getHour(); // 更新小时
                curDayPattern = pattern.get(curTime.getDayOfWeek().getValue() - 1); //更新日行为模式，以防等待后进入下一天.
                //curDayPattern.get(curTimeOfHour).contains(curPositionType)
                if (Util.isHaveTypeCode(curPositionType, curDayPattern.get(curTimeOfHour))) { //避免在等待后进入下一个时间段，并且下一个时间段应处位置等同于当前位置。
                    var nextHourTime = curTime.plusHours(1).minusSeconds(curTime.getMinute());
                    curTime = pathStayAwhile(curTime, nextHourTime, thisPathPOIs.get(curPositionType), res, isKeepStayPoint);//相当于又等到下个小时.
                } else { //确保会前往下一个地点
                    Map<String, Integer> targetPositionTypeMap;
                    String targetPositionType;
                    if (curDayPattern.get(curTimeOfHour).size() > 1) { // 如果当前时刻行为模式中有多个候选项。
                        targetPositionTypeMap = curDayPattern.get(curTimeOfHour).get(rand.nextInt(curDayPattern.get(curTimeOfHour).size())); // 随机选择地点
                    } else { // 只有一种可能
                        targetPositionTypeMap = curDayPattern.get(curTimeOfHour).get(0); //选第一个
                    }
                    targetPositionType = getTypeCodeByCurMap(targetPositionTypeMap);
                    if (thisPathPOIs.containsKey(targetPositionType)) { //如果此地在之前已经被访问过
                        //发送请求，拼接
                        POIs curPoi = thisPathPOIs.get(curPositionType);
                        POIs nextPoi = thisPathPOIs.get(targetPositionType);
//                        this.log.info("前往重复的目标POI,名称是："+ nextPoi.getPOIName() );
                        if (isDriving(curPoi, nextPoi, drivingRate)) {
                            LocalDateTime temp = sentGetAndCombinationTrajectoryWithDrivingmode(curPoi, nextPoi, res, curTime, key); // 出发！
                            curTime = temp;
                            curPositionType = nextPoi.getPOITypeCode();
                        } else {
                            LocalDateTime temp; // 出发！
                            if (isPublictransport(curPoi, nextPoi) == true) {
                                temp = sentGetAndCombinationTrajectoryWithPublictransportMode(curPoi, nextPoi, res, curTime, cityCode, key);
                                if (temp != null) curTime = temp;
                                else {
                                    if (Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat()) < 8000) {
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
                                if (isBycycling(curPoi, nextPoi, 4)) {
                                    temp = sentGetAndCombinationTrajectoryWithBicyclingmode(curPoi, nextPoi, res, curTime, key);
                                } else {
                                    temp = sentGetAndCombinationTrajectoryWithWalkingmode(curPoi, nextPoi, res, curTime, key);
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
                            if (isPublictransport(curPoi, nextPoi) == true) {
                                temp = sentGetAndCombinationTrajectoryWithPublictransportMode(curPoi, nextPoi, res, curTime, cityCode, key);
                                if (temp != null) curTime = temp;
                                else {
                                    if (Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat()) < 8000) {
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
                                if (isBycycling(curPoi, nextPoi, 4)) {
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

    public boolean isPublictransport(POIs curPoi, POIs nextPoi) {
        double distance = Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat());
        if (distance > 4000) {
            if (rand.nextInt(10) > 3) {
                return true;
            } else {
                return false;
            }
        } else {
            if (rand.nextInt(10) > 5) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean isDriving(POIs curPoi, POIs nextPoi, int rate) {
        double distance = Util.getDistance(curPoi.getLng(), curPoi.getLat(), nextPoi.getLng(), nextPoi.getLat());
        boolean isDriving = false;
        if (distance / 5000 > 1) {
            if (rate / (distance / 5000) > rand.nextInt(10)) {
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

    public LocalDateTime sentGetAndCombinationTrajectoryWithPublictransportMode(POIs curPOI, POIs nextPOI, Trajectory res, LocalDateTime curTime, String cityCode, String key) throws IOException {
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

    public LocalDateTime pathStayAwhile(LocalDateTime startTime, LocalDateTime endTime, POIs poi, Trajectory res, boolean isKeepStayPoint) {
        LocalDateTime pointer = startTime;
        if (isKeepStayPoint) {
            while (startTime.plusSeconds(5).compareTo(endTime) < 0) {
                var temp = new Position(poi.getLng(), poi.getLat(), poi.getPOITypeCode());
                temp.setPoiid(poi.getPOIID());
                res.addPositionWithTimeline(temp, startTime);
                startTime = startTime.plusSeconds(5);
            }
        } else {
            // 仅保留开始点和最后一点
            while (startTime.plusSeconds(5).compareTo(endTime) < 0) {
                if (startTime.isEqual(pointer) || startTime.isEqual(endTime)) {
                    var temp = new Position(poi.getLng(), poi.getLat(), poi.getPOITypeCode());
                    temp.setPoiid(poi.getPOIID());
                    res.addPositionWithTimeline(temp, startTime);
                }
                startTime = startTime.plusSeconds(5);
            }
        }
        return startTime;
    }

    public static LocalDateTime addPositionFromOriginPathWithPublictransportMode(OriginPath originPath, LocalDateTime startTime, int d, Trajectory res) {
        int all_Distance = originPath.getDistance(); // 总距离;
        //目前我们假设地铁每5分钟一班、公交车20分钟一班
        for (int i = 0; i < originPath.getSize(); i++) {
            // 对于分段中的每一点，先判断是步行的分段还是公交路线的分段
            if (originPath.getStep_mode().get(i).equals("walking")) {
                Position startPosition = new Position(originPath.getStep_polyLine().get(i).get(0).getLng(),
                        originPath.getStep_polyLine().get(i).get(0).getLat(),
                        originPath.getStep_polyLine().get(i).get(0).getTypeCode());
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
                        startTime = pathStayForBus(startTime, endTime, lastPosition, res);
                    } else {
                        int mins = startTime.getMinute();
                        int secs = startTime.getSecond();
                        LocalDateTime endTime = startTime.plusSeconds(60 - secs).plusMinutes(20 - mins % 20);
                        var lastPosition = new Position(originPath.getStep_polyLine().get(i).get(k).getLng(),
                                originPath.getStep_polyLine().get(i).get(k).getLat(),
                                originPath.getStep_polyLine().get(i).get(k).getTypeCode());
                        startTime = pathStayForBus(startTime, endTime, lastPosition, res);
                    }
                }
            } else {
                Position startPosition = new Position(originPath.getStep_polyLine().get(i).get(0).getLng(),
                        originPath.getStep_polyLine().get(i).get(0).getLat(),
                        originPath.getStep_polyLine().get(i).get(0).getTypeCode());
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
                    startTime = stepEndTime;
                    res.addPositionWithTimeline(lastPosition, startTime); // 插入最后一点
                    startTime = startTime.plusSeconds(d);
                }
            }
        }
        return startTime;
    }

    public static LocalDateTime addPositionFromOriginPath(OriginPath originPath, LocalDateTime startTime, int d, Trajectory res) { // d = 5;
        int all_Distance = originPath.getDistance(); // 总距离;
        for (int i = 0; i < originPath.getSize(); i++) { // 对每分段轨迹进行取点，建立符合gps规律的数据
            //先插入第一个点
            Position startPosition = new Position(originPath.getStep_polyLine().get(i).get(0).getLng(),
                    originPath.getStep_polyLine().get(i).get(0).getLat(),
                    originPath.getStep_polyLine().get(i).get(0).getTypeCode());
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
    public void getTrajectoryAndSimulation(List<Trajectory> trajectories, int R0, String startT, String endT) {
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
        int poiInfectedFactor = 180000;
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
        getSensiableToExposureAtfirst(trajectories.get(2), curTime);
        getSensiableToExposureAtfirst(trajectories.get(5), curTime);
        getSensiableToExposureAtfirst(trajectories.get(9), curTime);
        getSensiableToExposureAtfirst(trajectories.get(100), curTime);

        List<Integer> aroundInfectedTra;
        while (curTime.compareTo(endTime) < 0) {
            //模拟疫情
            List<Integer> unInfectedIdList = new ArrayList<>();
            List<Integer> infectedIdList = new ArrayList<>();
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
                        infectedIdList.add(tra.getId());
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
                                Double aa = (Double) InfectedInCommuteRate(temp, curTime);
                                String aaaaa = aa.toString();
                                this.log.info("进行通勤感染判断，当前的轨迹id是：" + temp.getId() + "时间是：" + curTime.toString() + "当前的接触时间:" + Duration.between(temp.lastContactTime, curTime).getSeconds() +
                                        "总病毒量是：" + temp.virusNumSumInContact + "感染的概率是：" + aaaaa + " 状态为：" + temp.path.get(latestPosition.get(temp.getId())).typeCode + "  被id:" + temp.maxVirusNumIdInContact);
                                if (isInfectedInCommute(temp, curTime)) {
                                    temp.state = Trajectory.State.E;
                                    temp.infectedBy = temp.maxVirusNumIdInContact;
                                    trajectories.get(temp.maxVirusNumIdInContact).infectedNum++;
                                    temp.exposureTime = temp.lastContactTime;
                                    temp.virusNum = Util.getNormalRand(40, 28);
                                    long hours = Util.getNormalRand(72, 48);
                                    long recoverHours = Util.getNormalRand(170, 96);
                                    temp.infectedTime = temp.exposureTime.plusHours(hours);
                                    temp.recoverTime = temp.infectedTime.plusHours(recoverHours);
                                    double rate = 1;
                                    rate = getProcessionRate(virusS, temp);
                                    if (Math.random() < rate) {
                                        temp.isAsym = false;
                                    } else temp.isAsym = true;
                                    temp.isInContacted = false;
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
                                if (trajectories.get(interationTra.get(j)).state == Trajectory.State.S) {
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
                                simulationInPOI(trajectories, curTime, unInfectedTraidInPoi, virusNumSum, maxVirusNumId, virusS, poiInfectedFactor);
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
                                if (trajectories.get(interationTra.get(j)).state == Trajectory.State.S) {
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
                                simulationInPOI(trajectories, curTime, unInfectedTraidInPoi, virusNumSum, maxVirusNumId, virusS, poiInfectedFactor);
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
                } else {
                }
            }
            // 留下记录
            if (curTime.getSecond() == 0 && curTime.getMinute() == 0) {
                List<Integer> tempres = new ArrayList<>();
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
                }
                eachHours.add(curTime);
                tempres.add(sNumber);
                tempres.add(eNumber);
                tempres.add(iNumber);
                tempres.add(aNumber);
                tempres.add(rNumber);
                eachHoursState.add(tempres);
            }
            curTime = curTime.plusSeconds(1);
        }
        try {
            Util.outputSimulationResult(eachHours, eachHoursState, "aa");
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public List<Integer> getAroundTraInCommute(List<Trajectory> list, Trajectory cur, List<Integer> infectedTraIdList, List<Integer> lastPosition) {
        String curLng = cur.getPath().get(lastPosition.get(cur.getId())).getLng();
        String curLat = cur.getPath().get(lastPosition.get(cur.getId())).getLat();
        String poiid = cur.path.get(lastPosition.get(cur.getId())).getPoiid();
        List<Integer> res = new ArrayList<>();
        int size = infectedTraIdList.size();
        for (int i = 0; i < size; i++) {
            // 判断通勤时的周围的人
            if (!list.get(infectedTraIdList.get(i)).getPath().get(lastPosition.get(infectedTraIdList.get(i))).isInPoi() && Util.getDistance(curLng, curLat, list.get(infectedTraIdList.get(i)).getPath().get(lastPosition.get(infectedTraIdList.get(i))).getLng(),
                    list.get(infectedTraIdList.get(i)).getPath().get(lastPosition.get(infectedTraIdList.get(i))).getLat()) < 30.0 &&
                    !list.get(infectedTraIdList.get(i)).getPath().get(lastPosition.get(infectedTraIdList.get(i))).typeCode.equals("driving")) {
                res.add(infectedTraIdList.get(i));
            }
        }
        return res;
    }

    public boolean isInfectedInCommute(Trajectory temp, LocalDateTime curTime) {
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
        double rate = 1 - Math.exp(-(avgVirusNUm * mask * vc) / 90000.0);
        if (Math.random() < rate) {
            return true;
        } else return false;
    }

    public double InfectedInCommuteRate(Trajectory temp, LocalDateTime curTime) {
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
        double rate = 1 - Math.exp(-(avgVirusNUm * mask * vc) / 90000.0);
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

    public void getSensiableToExposureAtfirst(Trajectory temp, LocalDateTime curTime) {
        temp.state = Trajectory.State.E;
        temp.virusNum = 60;
        temp.infectedBy = -1;
        temp.exposureTime = curTime;
        long hours = Util.getNormalRand(72, 48);
        long recoverHours = Util.getNormalRand(170, 96);
        temp.infectedTime = temp.exposureTime.plusHours(hours);
        temp.recoverTime = temp.infectedTime.plusHours(recoverHours);
        double rate = 1;
        rate = getProcessionRate(0.5, temp);
        if (Math.random() < rate) {
            temp.isAsym = false;
        } else temp.isAsym = true;
        temp.isInContacted = false;
    }

    public String simulationInPOIisInfected(Trajectory t, double rate, LocalDateTime curTime, int maxVirusNumId, double virusS) {
        // 1 == 感染且发病 2 == 感染不发病 3 == 不感染
        if (Math.random() < rate) {
            t.state = Trajectory.State.E;
            t.infectedBy = maxVirusNumId;
            t.exposureTime = curTime;
            t.isExposureInPoi = true;
            t.virusNum = Util.getNormalRand(40, 20);
            long hours = Util.getNormalRand(72, 48);
            long recoverHours = Util.getNormalRand(170, 96);
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

    public void simulationInPOI(List<Trajectory> trajectories, LocalDateTime curTime, List<Integer> unInfectedTraidInPoi, double virusNumSum, int maxVirusNumId, double virusS, int factor) {
        for (int j = 0; j < unInfectedTraidInPoi.size(); j++) {
            Trajectory t = trajectories.get(unInfectedTraidInPoi.get(j));
            var max = trajectories.get(maxVirusNumId);
            Duration duration = Duration.between(max.lastContactTimeInPoi, curTime);
            long seconds = duration.toSeconds();
            //计算感染概率
            double avgVirusNUm = virusNumSum;
            double mask = 1;
            double vc = 1;
            if (t.isVaccines.equals("A")) {
                vc = 0.5;
            }
            if (Util.rand.nextInt(10) > t.maskRate) {
                mask = 0.75;
            }
            double rate = 1 - Math.exp(-1 * avgVirusNUm / factor * seconds * mask * vc);
            String resultString = simulationInPOIisInfected(t, rate, curTime, maxVirusNumId, virusS);
            if (resultString.equals("1") || resultString.equals("2")) {
                trajectories.get(maxVirusNumId).infectedNum++;
            }
            this.log.info("进行poi感染判断，当前的轨迹id是：" + t.getId() + "  " + "当前的因素是" + factor + " 接触强度是：" + virusNumSum + " 接触时间为："
                    + seconds + "  感染概率是：" + rate);
        }
    }
}
