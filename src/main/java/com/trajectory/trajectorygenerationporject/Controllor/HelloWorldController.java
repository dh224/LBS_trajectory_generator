package com.trajectory.trajectorygenerationporject.Controllor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.trajectory.trajectorygenerationporject.Common.Util;
import com.trajectory.trajectorygenerationporject.POJO.*;
import com.trajectory.trajectorygenerationporject.Service.CityService;
import com.trajectory.trajectorygenerationporject.Service.POITypeService;
import com.trajectory.trajectorygenerationporject.Service.POIsService;
import com.trajectory.trajectorygenerationporject.Service.PathService;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class HelloWorldController {
    @Autowired
    private CityService cityService;

    @Autowired
    private PathService pathService;

    @Autowired
    private POIsService poisService;

    @Autowired
    private POITypeService poiTypeService;

    @Autowired
    private MapperFacade mapperFacade;

    @GetMapping("/hello")
    public String hello() throws IOException {
//        LocalDateTime time = LocalDateTime.now();
//        System.out.println(time.plusSeconds(990000));
        //System.out.println(HttpUtil.sentGet("https://restapi.amap.com/v5/place/text?key=051b74eefaa0e56e8d7ad11b11b96d2b&types=120300|120301|120302|120303&region=" + cityService.findAdCodeByAdName("北京市") + "&city_limit=true&page_size=25"));
        return "1";
//        return cityService.listCity().toString();
//        return HttpUtil.sentGet("https://restapi.amap.com/v5/place/text?key=051b74eefaa0e56e8d7ad11b11b96d2b&types=120300|120301|120302|120303&region=110000&city_limit=true&page_size=25").toJSONString();
    }

    @GetMapping("/searchPOI")
    public String searchPOI(String cityName, String adName) throws IOException {
        var adCode = cityService.findAdCodeByCityNameAndAdname(cityName, adName);
        poisService.searchPOIsAllType(cityName, adName);
        return "1";
    }
    @CrossOrigin(originPatterns = "http://localhost:3000")
    @ResponseBody()
    @PostMapping("/generate")
    public String generate(@RequestBody JSONObject post) throws IOException {
        int longCommuteRange = 20000;
        int midCommuteRange = 10000;
        int shortCommuteRange = 3000;
        List<String> keys = new ArrayList<>();

        keys.add("5afce314617c908b801e1b9fc346340a");
        keys.add("6484f91689533722a25559cb26205c9d");
        keys.add("be9f2178968b4e82d7b345ef37516323");

        keys.add("051b74eefaa0e56e8d7ad11b11b96d2b");
        keys.add("408fc7071b2631e7922ed61faaed8ee5");
        keys.add("6554a57ac07efe625fa4c05711f2e62e");
        keys.add("192b951ff8bc56e05cb476f8740a760c");

        keys.add("02c4c5fb61548a2b4020e4dcc0960258");
        int keyPointer = 0;
        String pName = post.get("pname").toString();
        String cityName = post.get("cityname").toString();
        if(pName.equals("北京市") || pName.equals("上海市") || pName.equals("重庆市")|| pName.equals("天津市")){
            cityName = pName;
        }else{
        }
        int trajectoryNum = Integer.parseInt(post.get("trajectorysize").toString()) ;
        System.out.println("本次生成" + trajectoryNum);
        System.out.println(post);
        String startTime = post.get("starttime").toString();
        String endTime = post.get("endtime").toString();
        longCommuteRange =Integer.valueOf( post.get("longcommuterange").toString());
        midCommuteRange =Integer.valueOf( post.get("midcommuterange").toString());
        shortCommuteRange =Integer.valueOf( post.get("shortcommuterange").toString());
        String temp =JSONArray.toJSONString(post.get("pattern"));
        JSONArray patterns = JSON.parseArray(temp);
        List<List<Map<Integer, List<Map<String, Integer>>>>> patternsList = new ArrayList<>();
        List<JSONObject> patternsAttributeJSONList = new ArrayList<>();
        for(int i = 0; i < patterns.size(); i++){
            var onePatternInfo = (JSONObject)patterns.get(i);
            JSONArray onePattern = (JSONArray)onePatternInfo.get("pattern");
            JSONObject onePatternAttributeInfo = (JSONObject)onePattern.get(0);
            JSONObject onePatternWorkdayPatternInfo = (JSONObject)onePattern.get(1);
            JSONObject onePatternNoworkdayPatternInfo = (JSONObject)onePattern.get(2);
            JSONObject onePatternAttribute  = (JSONObject)onePatternAttributeInfo.get("attribute");
            JSONArray onePatternWorkdayPattern =  (JSONArray)onePatternWorkdayPatternInfo.get("workdayPattern");
            JSONArray onePatternNoworkdayPattern =  (JSONArray)onePatternNoworkdayPatternInfo.get("noworkdayPattern");
            patternsAttributeJSONList.add(onePatternAttribute);
            int workdayday = 5;
            int noworkdayday = 2;
            List<Map<Integer, List<Map<String, Integer>>>> pattern = new ArrayList<>();
            for(int j = 0; j < workdayday; j++){
                Map<Integer, List<Map<String, Integer>>> singleDay = new HashMap<>();
                int workdayPatternTimeSize = onePatternWorkdayPattern.size();
                for(int k = 0; k < workdayPatternTimeSize; k ++){
                    JSONObject timeQuantum = (JSONObject) onePatternWorkdayPattern.get(k);
                    JSONArray tempTypeCode = ((JSONObject) onePatternWorkdayPattern.get(k)).getJSONArray("value");
                    String commuteString = ((JSONObject) onePatternWorkdayPattern.get(k)).get("commute").toString();
                    int radius = 50000;
                    if(commuteString.equals("不限")){
                    }else if(commuteString.equals("长距离")) radius = longCommuteRange;
                    else if(commuteString.equals("中距离")) radius = midCommuteRange;
                    else if(commuteString.equals("短距离")) radius = shortCommuteRange;

                    int stTime = Integer.valueOf(timeQuantum.getString("startTime").substring(0,2));
                    int edTime = Integer.valueOf(timeQuantum.getString("endTime").substring(0,2));
                    if(edTime == 0) edTime = 24;
                    List<Map<String, Integer>> tempList = new ArrayList<>();
                    for(int l = 0; l < tempTypeCode.size(); l++){
                        String tt = tempTypeCode.get(l).toString();
                        Map<String, Integer> ttttt = new HashMap<>();
                        POIType poiType = poiTypeService.findTypeCodeBySubCategory(tt);
                        ttttt.put(poiType.getTypeCode(), radius);
                        tempList.add(ttttt);
                    }
                    for(;stTime<edTime;stTime++){
                        singleDay.put(stTime,tempList);
                    }
                    singleDay.remove(24);
                }
                pattern.add(singleDay);
            }
            for(int j = 0; j < noworkdayday; j++){
                Map<Integer, List<Map<String, Integer>>> singleDay = new HashMap<>();
                int noworkdayPatternSize = onePatternNoworkdayPattern.size();
                for(int k = 0; k < noworkdayPatternSize; k ++){
                    JSONObject timeQuantum = (JSONObject) onePatternNoworkdayPattern.get(k);
                    JSONArray tempTypeCode = ((JSONObject) onePatternNoworkdayPattern.get(k)).getJSONArray("value");
                    String commuteString = ((JSONObject) onePatternNoworkdayPattern.get(k)).get("commute").toString();
                    int radius = 50001;
                    if(commuteString.equals("不限")){
                    }else if(commuteString.equals("长距离")) radius = longCommuteRange;
                    else if(commuteString.equals("中距离")) radius = midCommuteRange;
                    else if(commuteString.equals("短距离")) radius = shortCommuteRange;

                    int stTime = Integer.valueOf(timeQuantum.getString("startTime").substring(0,2));
                    int edTime = Integer.valueOf(timeQuantum.getString("endTime").substring(0,2));
                    if(edTime == 0) edTime = 24;
                    List<Map<String, Integer>> tempList = new ArrayList<>();
                    for(int l = 0; l < tempTypeCode.size(); l++){
                        String tt = tempTypeCode.get(l).toString();
                        Map<String, Integer> ttttt = new HashMap<>();
                        POIType poiType = poiTypeService.findTypeCodeBySubCategory(tt);
                        ttttt.put(poiType.getTypeCode(), radius);
                        tempList.add(ttttt);
                    }
                    for(;stTime<edTime;stTime++){
                        singleDay.put(stTime,tempList);
                    }
                }
                pattern.add(singleDay);
            }
            patternsList.add(pattern);
        }
        int TrajectoryRateNum = 0;
        List<Integer> patternChooser = new ArrayList<>();
        for(int i =0; i < patternsAttributeJSONList.size(); i++){
            int singlePatternRate =  Integer.parseInt(patternsAttributeJSONList.get(i).get("patternrate").toString());
            for(int j = 0; j < singlePatternRate; j++){
                patternChooser.add(i);
            }
            System.out.println("当前的patternrate为："+ singlePatternRate);
            TrajectoryRateNum += singlePatternRate;
        }
        System.out.println("pattern:" + patternsList);
        List<Integer> timmer  = new ArrayList<>();
        List<Trajectory> res = new ArrayList<>();
        for(int i = 0; i < trajectoryNum; i ++){
            if(i != 0 && i % 900 == 0){
                keyPointer++;
                keyPointer %= 7;
            }
            int num  = pathService.choosePattern(TrajectoryRateNum, patternChooser);
            int maxage = Integer.parseInt(patternsAttributeJSONList.get(num).get("maxage").toString());
            int minAge = Integer.parseInt(patternsAttributeJSONList.get(num).get("minage").toString());
            int drivingRate = Integer.parseInt(patternsAttributeJSONList.get(num).get("drivingrate").toString());
            int genderRate = Integer.parseInt(patternsAttributeJSONList.get(num).get("genderrate").toString());
            int vaccinerate = Integer.parseInt(patternsAttributeJSONList.get(num).get("vaccinerate").toString());
            int maskrate = Integer.parseInt(patternsAttributeJSONList.get(num).get("maskrate").toString());
            String gender = "FM";
            if(Util.rand.nextInt(10) + 1 > genderRate){
                gender = "M";
            }
            String vaccine = "O";
            if(Util.rand.nextInt(10) + 1 > vaccinerate){
                vaccine = "A";
            }
            var pattern = patternsList.get(num);
            String patternName = patternsAttributeJSONList.get(num).get("patternname").toString();
            if(i % 200 == 0){
                System.out.println("当前生成到第" + i + "个轨迹。其类型是：" + patternName);
            }
            int randomage = Util.rand.nextInt(maxage - minAge + 1) + minAge;
            if(Util.rand.nextInt(10) > 8){
                randomage = Util.getNormalRand((maxage +  minAge) / 2, (maxage - minAge) / 2);
            }
//            int randomage = Util.getNormalRand((maxage +  minAge) / 2, (maxage - minAge) / 2);
            LocalTime a = LocalTime.now();
            Trajectory trajectory = null;
            try{
                trajectory = pathService.getTrajectory(patternName, cityName,cityName,startTime, endTime,pattern, false, randomage, "学生",gender, maskrate, vaccine,drivingRate,9,patternName+ i+"POIs", keys.get(keyPointer), "2022-03-07 00:00:00", "2022-03-08 00:00:00", "2022-03-09 00:00:00");
            }catch (Exception e){
                i--;
                System.out.println("遇到了问题"+ e+  "，回退一个轨迹，下一个i为:" + i);
                continue;
            }
            LocalTime end = LocalTime.now();
            Duration between = Duration.between(a, end);
            timmer.add(((int)between.getSeconds()));
            res.add(trajectory);
            Util.outputReuseTrajectory(trajectory, patternName + i);
        }
        long timeGapSum = 0;
        for(int j = 0;j < timmer.size();j++){
            timeGapSum += timmer.get(j);
        }
        double timeGap = (double)timeGapSum / timmer.size();
        System.out.println("生成每条轨迹的平均耗时为：" + timeGap);
        Util.outputHomeInformation(res, "aaa");
        pathService.recordAPListFromTrajectories(res,startTime, endTime, "AP");
        pathService.recordRadiusFromTrajectories(res,"2022-03-01 00:00:00", "2022-03-10 00:00:00", "全部");
        pathService.recordRadiusFromTrajectories(res,"2022-03-01 00:00:00", "2022-03-05 00:00:00", "工作日");
        pathService.recordRadiusFromTrajectories(res,"2022-03-05 00:00:00", "2022-03-07 00:00:00", "周末");
        pathService.recordRadiusFromTrajectories(res,"2022-03-07 00:00:00", "2022-03-08 00:00:00", "隔离A");
        pathService.recordRadiusFromTrajectories(res,"2022-03-08 00:00:00", "2022-03-09 00:00:00", "隔离B");
        pathService.recordRadiusFromTrajectories(res,"2022-03-09 00:00:00", "2022-03-10 00:00:00", "隔离C");
        List<Position> homeList = new ArrayList<>();
        for(int i = 0; i < res.size(); i ++){
            var tt = res.get(i);
            var pp = new Position(tt.homeLng, tt.homeLat, tt.homeName);
            homeList.add(pp);
        }
        Util.outputHomeInformation(homeList);
        Util.recordTraInformation(res   ,"2022-03-01 00:00:00", "2022-03-10 00:00:00", "所有日期的通勤情况");
        Util.recordTraInformation(res,"2022-03-01 00:00:00", "2022-03-05 00:00:00", "工作日的通勤");
        Util.recordTraInformation(res,"2022-03-05 00:00:00", "2022-03-07 00:00:00", "非工作日的通勤");
        Util.recordTraInformation(res,"2022-03-07 00:00:00", "2022-03-08 00:00:00", "隔离A的通勤情况");
        Util.recordTraInformation(res,"2022-03-08 00:00:00", "2022-03-09 00:00:00", "隔离b的通勤情况");
        Util.recordTraInformation(res,"2022-03-09 00:00:00", "2022-03-10 00:00:00", "隔离C的通勤情况");
        Util.outputAgeGroup(res,"年龄段");
        Util.outputTrafficInformation_v1(res);
        Util.outputTrafficInformation_v3(res);
        Util.outputTrafficInformation_v2(res);
        Util.outputPOIInformation_v1(res);
        Util.outputPOIInformation_v4(res);
//        LocalTime simulationStartTime = LocalTime.now();
//        pathService.getTrajectoryAndSimulation(res,7,startTime,endTime, 360000, 120000, 30, 20, 72, 30, 144, 72);
//        for(int i = 0 ; i < res.size(); i ++){
//            var t = res.get(i);
//            if(t.state == Trajectory.State.S){
//                System.out.println("id:" + t.getId() + " 未感染病毒");
//            }else{
//                System.out.println("id:" + t.getId() + "感染了病毒，当前的状态是:" + t.state + "  感染的时间为：" + t.exposureTime +
//                        " 是被id:" + t.infectedBy + "的轨迹感染的");
//            }
//        }
//        LocalTime simulationEndTime = LocalTime.now();
//        Duration betweena = Duration.between(simulationStartTime, simulationEndTime);
//        System.out.println("模拟的总耗时" + betweena.getSeconds());
        return "已生成" + trajectoryNum + "个轨迹";
    }
    @GetMapping("/gettra")
    public String getTra() throws IOException{
        List<Trajectory> trajectories = new ArrayList<>();
        var st = LocalTime.now();
        try(Stream<Path> pathStream = Files.walk(Paths.get(".\\src\\main\\resources\\result\\行程"))){
            List<File> filesList = pathStream.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
            for (var file: filesList) {
                var inputStream = new FileInputStream(file);
                Util.readTraTimeFile(inputStream);
            }
        }
        return "success";
    }
    @GetMapping("/resetTrajectories")
    public String resetTrajectories() throws IOException{
        List<Trajectory> trajectories = new ArrayList<>();
        var st = LocalTime.now();
        try(Stream<Path> pathStream = Files.walk(Paths.get(".\\src\\main\\resources\\result\\行程"))){
            List<File> filesList = pathStream.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
            for (var file: filesList) {
                var inputStream = new FileInputStream(file);
                Util.readTraFile(inputStream);
            }
        }
        return "success";
    }
    @GetMapping("/outputTrafficInformation")
    public String outputTrafficInformation() throws IOException{
        List<Trajectory> trajectories = new ArrayList<>();
        var st = LocalTime.now();
        try(Stream<Path> pathStream = Files.walk(Paths.get(".\\src\\main\\resources\\static"))){
            List<File> filesList = pathStream.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
            for (var file: filesList) {
                LocalTime start = LocalTime.now();
                var inputStream = new FileInputStream(file);
                var tempTra = Util.readExcelFile(inputStream);
                tempTra.isInPoiContacted = true;
                String time = "2022-11-01 00:00:00";
                var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                tempTra.lastContactTimeInPoi = LocalDateTime.parse(time, dateFormatter);
                tempTra.lastPoiid = tempTra.path.get(0).poiid;
                tempTra.printTrajectoryInformation();
                trajectories.add(tempTra);
                LocalTime end = LocalTime.now();
                Duration between = Duration.between(start, end);
                System.out.println("读取单个文件的耗时为：" + between.getSeconds() + "s");
            }
        }
        LocalTime et = LocalTime.now();
        Duration between = Duration.between(st, et);
        System.out.println("读取文件的总耗时为：" + between.getSeconds() + "s");
        Util.outputPOIInformation_v1(trajectories);
        Util.outputPOIInformation_v2(trajectories);
        Util.outputPOIInformation_v3(trajectories);
        Util.outputPOIInformation_v4(trajectories);
        Util.outputTrafficInformation_v1(trajectories);
        Util.outputTrafficInformation_v2(trajectories);
        Util.outputTrafficInformation_v2(trajectories);
        Util.outputTrafficInformation_v3(trajectories);
        Util.outputTrafficInformation_v4(trajectories);
        Util.outputTrafficInformation_v5(trajectories);
        Util.outputTrafficInformation_v6(trajectories);
        return "success";
    }
    @GetMapping("/outputHomeInformation")
    public String outputHoomeInformation() throws IOException{
        List<Trajectory> trajectories = new ArrayList<>();
        var st = LocalTime.now();
        try(Stream<Path> pathStream = Files.walk(Paths.get(".\\src\\main\\resources\\static"))){
            List<File> filesList = pathStream.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
            int index = 0;
            for (var file: filesList) {
                LocalTime start = LocalTime.now();
                var inputStream = new FileInputStream(file);
                var tempTra = Util.readExcelFile(inputStream);
                tempTra.isInPoiContacted = true;
                tempTra.setFileName(file.getName());
                String time = "2022-11-01 00:00:00";
                var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                tempTra.lastContactTimeInPoi = LocalDateTime.parse(time, dateFormatter);
                tempTra.setId(index++);
                System.out.println("id:" + index);
                tempTra.lastPoiid = tempTra.path.get(0).poiid;
                tempTra.printTrajectoryInformation();
                trajectories.add(tempTra);
                LocalTime end = LocalTime.now();
                Duration between = Duration.between(start, end);
                System.out.println("读取单个文件的耗时为：" + between.getSeconds() + "s");
            }
        }
        LocalTime et = LocalTime.now();
        Duration between = Duration.between(st, et);
        System.out.println("读取文件的总耗时为：" + between.getSeconds() + "s");
        pathService.recordBusFromTrajectories(trajectories,"2022-03-01 00:00:00", "2022-03-02 00:00:00","公共交通");
//        pathService.recordTripFromTrajectories(trajectories, "2022-03-01 00:00:00", "2022-03-10 00:00:00", "行程");
//        Util.outputHomeInformation(trajectories, "aaa");
//        pathService.recordAPListFromTrajectories(trajectories,"2022-03-01 00:00:00", "2022-03-10 00:00:00", "AP");
//        pathService.recordRadiusFromTrajectories(trajectories,"2022-03-01 00:00:00", "2022-03-10 00:00:00", "全部");
//        pathService.recordRadiusFromTrajectories(trajectories,"2022-03-01 00:00:00", "2022-03-05 00:00:00", "工作日");
//        pathService.recordRadiusFromTrajectories(trajectories,"2022-03-05 00:00:00", "2022-03-07 00:00:00", "周末");
//        pathService.recordRadiusFromTrajectories(trajectories,"2022-03-07 00:00:00", "2022-03-08 00:00:00", "隔离A");
//        pathService.recordRadiusFromTrajectories(trajectories,"2022-03-08 00:00:00", "2022-03-09 00:00:00", "隔离B");
//        pathService.recordRadiusFromTrajectories(trajectories,"2022-03-09 00:00:00", "2022-03-10 00:00:00", "隔离C");
//        List<Position> homeList = new ArrayList<>();
//        for(int i = 0; i < trajectories.size(); i ++){
//            var tt = trajectories.get(i);
//            var pp = new Position(tt.homeLng, tt.homeLat, tt.homeName);
//            homeList.add(pp);
//        }
//        Util.outputHomeInformation(homeList);
//        Util.recordTraInformation(trajectories   ,"2022-03-01 00:00:00", "2022-03-10 00:00:00", "所有日期的通勤情况");
//        Util.recordTraInformation(trajectories,"2022-03-01 00:00:00", "2022-03-05 00:00:00", "工作日的通勤");
//        Util.recordTraInformation(trajectories,"2022-03-05 00:00:00", "2022-03-07 00:00:00", "非工作日的通勤");
//        Util.recordTraInformation(trajectories,"2022-03-07 00:00:00", "2022-03-08 00:00:00", "隔离A的通勤情况");
//        Util.recordTraInformation(trajectories,"2022-03-08 00:00:00", "2022-03-09 00:00:00", "隔离b的通勤情况");
//        Util.recordTraInformation(trajectories,"2022-03-09 00:00:00", "2022-03-10 00:00:00", "隔离C的通勤情况");
//        Util.outputAgeGroup(trajectories,"年龄段");
//        Util.outputTrafficInformation_v1(trajectories);
//        Util.outputTrafficInformation_v3(trajectories);
//        Util.outputTrafficInformation_v2(trajectories);
//        Util.outputPOIInformation_v1(trajectories);
//        Util.outputPOIInformation_v4(trajectories);
//        List<Position> homeList = new ArrayList<>();
//        for(int i = 0; i < trajectories.size(); i ++){
//            var tt = trajectories.get(i);
//            var pp = new Position(tt.homeLng, tt.homeLat, tt.homeName);
//            homeList.add(pp);
//        }
//        pathService.recordRadiusFromTrajectories(trajectories,"2022-03-01 00:00:00", "2022-03-05 00:00:00", "工作日的回转半径");
//        pathService.recordRadiusFromTrajectories(trajectories,"2022-03-05 00:00:00", "2022-03-07 00:00:00", "非工作日的回转半径");
//        Util.recordTraInformation(trajectories,"2022-03-01 00:00:00", "2022-03-05 00:00:00", "工作日的通勤");
//        Util.recordTraInformation(trajectories,"2022-03-05 00:00:00", "2022-03-07 00:00:00", "非工作日的通勤");
//        Util.outputAgeGroup(trajectories,"年龄段");
        return "success";
    }
    @GetMapping("/outputRadiusOfGyration")
    public String outputRadiusOfGyration() throws IOException{
        List<Trajectory> trajectories = new ArrayList<>();
        var st = LocalTime.now();
        try(Stream<Path> pathStream = Files.walk(Paths.get(".\\src\\main\\resources\\static"))){
            List<File> filesList = pathStream.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
            for (var file: filesList) {
                LocalTime start = LocalTime.now();
                var inputStream = new FileInputStream(file);
                var tempTra = Util.readExcelFile(inputStream);
                tempTra.isInPoiContacted = true;
                String time = "2022-11-01 00:00:00";
                var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                tempTra.lastContactTimeInPoi = LocalDateTime.parse(time, dateFormatter);
                tempTra.lastPoiid = tempTra.path.get(0).poiid;
                tempTra.printTrajectoryInformation();
                trajectories.add(tempTra);
                LocalTime end = LocalTime.now();
                Duration between = Duration.between(start, end);
                System.out.println("读取单个文件的耗时为：" + between.getSeconds() + "s");
            }
        }
        LocalTime et = LocalTime.now();
        Duration between = Duration.between(st, et);
        System.out.println("读取文件的总耗时为：" + between.getSeconds() + "s");

//        pathService.recordRadiusFromTrajectories(trajectories,"2022-11-09 00:00:00", "2022-11-10 00:00:00", "无隔离");
//        pathService.recordRadiusFromTrajectories(trajectories,"2022-11-11 00:00:00", "2022-11-12 00:00:00", "简单隔离");
        List<Position> homelist = new ArrayList<>();
        for(int i = 0; i < trajectories.size(); i ++){
            var tt = trajectories.get(i);
            var pp = new Position(tt.homeLng, tt.homeLat, tt.homeName);
            homelist.add(pp);
        }
        return "success";
    }
    @GetMapping("/GenerateTrajectory")
    public String generateTrajectory() throws IOException{
        List<Map<Integer, List<Map<String, Integer>>>> list = new ArrayList<>();
        for(int i = 0; i < 6; i ++){
            Map<Integer, List<Map<String, Integer>>> singleDay = new HashMap<>();
            List<Map<String, Integer>> tempList = new ArrayList<>();
            HashMap ttt = new HashMap<>();
            ttt.put("120302",30000);
            tempList.add(ttt);
            singleDay.put(0,tempList);
            singleDay.put(1,tempList);
            singleDay.put(2,tempList);
            singleDay.put(3,tempList);
            singleDay.put(4,tempList);
            singleDay.put(5,tempList);
            singleDay.put(6,tempList);
            singleDay.put(7,tempList);
            List<Map<String, Integer>> tempList2 = new ArrayList<>();
            HashMap ccc = new HashMap<>();
            ccc.put("141202",3000);
            tempList2.add(ccc);
            singleDay.put(8,tempList2);
            singleDay.put(9,tempList2);
            singleDay.put(10,tempList2);
            singleDay.put(11,tempList2);
            singleDay.put(12,tempList2);
            singleDay.put(13,tempList2);
            singleDay.put(14,tempList2);
            singleDay.put(15,tempList2);
            List<Map<String, Integer>> tempList3 = new ArrayList<>();
            HashMap tcc = new HashMap<>();
            tcc.put("050000",5000);
            tcc.put("061205",5000);
            tempList3.add(tcc);
            singleDay.put(16,tempList3);
            singleDay.put(17,tempList3);
            List<Map<String, Integer>> tempList4 = new ArrayList<>();
            HashMap cascs = new HashMap();
            cascs.put("060400",6000);
            cascs.put("050500",3000);
            cascs.put("061205",5000);
            tempList4.add(cascs);
            singleDay.put(18,tempList4);
            singleDay.put(19,tempList4);
            singleDay.put(20,tempList);
            singleDay.put(21,tempList);
            singleDay.put(22,tempList);
            singleDay.put(23,tempList);
            list.add(singleDay);
        }
        Map<Integer, List<Map<String, Integer>>> singleDay = new HashMap<>();
        List<Map<String, Integer>> tempList = new ArrayList<>();
        HashMap<String, Integer> aca = new HashMap();
        aca.put("120302", 4000);
        tempList.add(aca);
        singleDay.put(0,tempList);
        singleDay.put(1,tempList);
        singleDay.put(2,tempList);
        singleDay.put(3,tempList);
        singleDay.put(4,tempList);
        singleDay.put(5,tempList);
        singleDay.put(6,tempList);
        singleDay.put(7,tempList);
        List<Map<String, Integer>> tempList2 = new ArrayList<>();
        HashMap<String, Integer> asdavvbrf = new HashMap<>();
        asdavvbrf.put("120302",8000);
        tempList2.add(asdavvbrf);
        singleDay.put(8,tempList2);
        singleDay.put(9,tempList2);
        singleDay.put(10,tempList2);
        singleDay.put(11,tempList2);
        singleDay.put(12,tempList2);
        singleDay.put(13,tempList2);
        singleDay.put(14,tempList2);
        singleDay.put(15,tempList2);
        List<Map<String, Integer>> tempList3 = new ArrayList<>();
        HashMap<String, Integer> uyi = new HashMap<>();
        uyi.put("050000",5000);
        uyi.put("061205",99999);
        tempList3.add(uyi);
        singleDay.put(16,tempList3);
        singleDay.put(17,tempList3);
        List<Map<String, Integer>> tempList4 = new ArrayList<>();
        HashMap<String, Integer> dc = new HashMap<>();
        dc.put("060400",15000);
        dc.put("050500",20000);
        dc.put("061205",30000);
        tempList4.add(dc);
        singleDay.put(18,tempList4);
        singleDay.put(19,tempList4);
        singleDay.put(20,tempList);
        singleDay.put(21,tempList);
        singleDay.put(22,tempList);
        singleDay.put(23,tempList);
        list.add(singleDay);
        System.out.println(list);
        Trajectory trajectory;
        //trajectory = pathService.getTrajectory("中学生", "北京市","朝阳区","2022-03-19 00:00:00", "2022-03-23 00:00:00",list, false, 20, "学生","M", true, "疫苗",5,9,"fileName");

        //创建工作薄对象
        //Util.outputtheTrajectory(trajectory, "tra");
        return "1";
    }

    @GetMapping("/simulationTrajectory")
    public String simulationTrajectory() throws IOException{
        List<Trajectory> trajectories = new ArrayList<>();
        var st = LocalTime.now();
        try(Stream<Path> pathStream = Files.walk(Paths.get(".\\src\\main\\resources\\static"))){
            List<File> filesList = pathStream.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
            for (var file: filesList) {
                LocalTime start = LocalTime.now();
               var inputStream = new FileInputStream(file);
               var tempTra = Util.readExcelFile(inputStream);
               tempTra.isInPoiContacted = true;
               String time = "2022-11-01 00:00:00";
               var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
               tempTra.lastContactTimeInPoi = LocalDateTime.parse(time, dateFormatter);
               tempTra.lastPoiid = tempTra.path.get(0).poiid;
               tempTra.printTrajectoryInformation();
               trajectories.add(tempTra);
               LocalTime end = LocalTime.now();
               Duration between = Duration.between(start, end);
               System.out.println("读取单个文件的耗时为：" + between.getSeconds() + "s");
            }
        }
        LocalTime et = LocalTime.now();
        Duration between = Duration.between(st, et);
        System.out.println("读取文件的总耗时为：" + between.getSeconds() + "s");
        int z = 0;
        int x = 0;
        int s = 0;
        int t = 0;
        long agesum = 0;
        List<Position> homeList = new ArrayList<>();
        for(int i = 0; i < trajectories.size(); i ++){
            var tt = trajectories.get(i);
            agesum += trajectories.get(i).getAge();
            String pattern = trajectories.get(i).getPatternName();
            if(pattern.equals("中学生")){
                z++;
            }else if (pattern.equals("小学生")){
                x++;
            }else if (pattern.equals("上班族")){
                s++;
            }else{
                t++;
            }
            var pp = new Position(tt.homeLng, tt.homeLat, tt.homeName);
            homeList.add(pp);
        }
        Util.outputHomeInformation(homeList);
        homeList = null;
        List<List<Integer>> factorLists = new ArrayList<>();
        List<Integer> innerFactors1 = new ArrayList<>();
        innerFactors1.add(120000);
        innerFactors1.add(90000);
        innerFactors1.add(30);
        innerFactors1.add(10);
        innerFactors1.add(72);
        innerFactors1.add(30);
        innerFactors1.add(144);
        innerFactors1.add(72);
        factorLists.add(innerFactors1);
        List<Integer> innerFactors2 = new ArrayList<>();
        innerFactors2.add(360000);
        innerFactors2.add(120000);
        innerFactors2.add(30);
        innerFactors2.add(10);
        innerFactors2.add(72);
        innerFactors2.add(30);
        innerFactors2.add(144);
        innerFactors2.add(72);
        factorLists.add(innerFactors1);
        List<Integer> innerFactors3 = new ArrayList<>();
        innerFactors3.add(360000);
        innerFactors3.add(120000);
        innerFactors3.add(30);
        innerFactors3.add(10);
        innerFactors3.add(72);
        innerFactors3.add(30);
        innerFactors3.add(144);
        innerFactors3.add(72);
        factorLists.add(innerFactors1);
        List<Integer> innerFactors4 = new ArrayList<>();
        innerFactors4.add(360000);
        innerFactors4.add(120000);
        innerFactors4.add(30);
        innerFactors4.add(10);
        innerFactors4.add(72);
        innerFactors4.add(30);
        innerFactors4.add(104);
        innerFactors4.add(48);
        factorLists.add(innerFactors1);
        System.out.println("中学生：" +z + "小学生" + x + "上班族" + s + "退休者" + t);
        for (int j = 0; j < 4; j ++){
            LocalTime simulationStartTime = LocalTime.now();
            int vacRate = 12;
            if(j == 2){
                vacRate = 6;
            }
            List<Trajectory> trajectories1 = Util.deepCopuTtrajectories(trajectories, vacRate);
            List<Integer> factors = factorLists.get(j);
            pathService.getTrajectoryAndSimulation(trajectories1, 8,"2022-11-01 00:00:00", "2022-11-14 01:00:00",
                    factors.get(0), factors.get(1), factors.get(2), factors.get(3), factors.get(4),
                    factors.get(5), factors.get(6), factors.get(7));
            LocalTime simulationEndTime = LocalTime.now();
            Duration betweena = Duration.between(simulationStartTime, simulationEndTime);
            System.out.println("模拟的耗时" + betweena.getSeconds());
        }
        return "a";
    }
}
