package com.trajectory.trajectorygenerationporject.Controllor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.trajectory.trajectorygenerationporject.Common.Util;
import com.trajectory.trajectorygenerationporject.POJO.POIType;
import com.trajectory.trajectorygenerationporject.POJO.POIs;
import com.trajectory.trajectorygenerationporject.POJO.Trajectory;
import com.trajectory.trajectorygenerationporject.Service.CityService;
import com.trajectory.trajectorygenerationporject.Service.POITypeService;
import com.trajectory.trajectorygenerationporject.Service.POIsService;
import com.trajectory.trajectorygenerationporject.Service.PathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        int longRange = 20000;
        int midRange = 10000;
        int shortRange = 3000;
        String pName = post.get("pname").toString();
        String cityName = post.get("cityname").toString();
        if(pName.equals("北京市") || pName.equals("上海市") || pName.equals("重庆市")|| pName.equals("天津市")){
            cityName = pName;
        }else{
        }
        int trajectoryNum = Integer.parseInt(post.get("trajectorysize").toString()) ;
        System.out.println(post);
        String startTime = post.get("starttime").toString();
        String endTime = post.get("endtime").toString();
        String temp =JSONArray.toJSONString(post.get("pattern"));
        JSONArray patterns = JSON.parseArray(temp);
        List<List<Map<Integer, List<Map<String, Integer>>>>> patternsList = new ArrayList<>();
        List<JSONObject> patternsAttributeList = new ArrayList<>();
        for(int i = 0; i < patterns.size(); i++){
            var onePatternInfo = (JSONObject)patterns.get(i);
            JSONArray onePattern = (JSONArray)onePatternInfo.get("pattern");
            JSONObject onePatternAttributeInfo = (JSONObject)onePattern.get(0);
            JSONObject onePatternWorkdayPatternInfo = (JSONObject)onePattern.get(1);
            JSONObject onePatternNoworkdayPatternInfo = (JSONObject)onePattern.get(2);
            JSONObject onePatternAttribute  = (JSONObject)onePatternAttributeInfo.get("attribute");
            JSONArray onePatternWorkdayPattern =  (JSONArray)onePatternWorkdayPatternInfo.get("workdayPattern");
            JSONArray onePatternNoworkdayPattern =  (JSONArray)onePatternNoworkdayPatternInfo.get("noworkdayPattern");
            patternsAttributeList.add(onePatternAttribute);
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
                    }else if(commuteString.equals("长距离")) radius = longRange;
                    else if(commuteString.equals("中距离")) radius = midRange;
                    else if(commuteString.equals("短距离")) radius = shortRange;

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
                    }else if(commuteString.equals("长距离")) radius = 20000;
                    else if(commuteString.equals("中距离")) radius = 10000;
                    else if(commuteString.equals("短距离")) radius = 5000;

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
        for(int i =0; i < patternsAttributeList.size(); i++){
            int singlePatternRate =  Integer.parseInt(patternsAttributeList.get(i).get("patternrate").toString());
            for(int j = 0; j < singlePatternRate; j++){
                patternChooser.add(i);
            }
            System.out.println("当前的patternrate为："+ singlePatternRate);
            TrajectoryRateNum += singlePatternRate;
        }
        System.out.println("pattern:" + patternsList);
        List<Trajectory> res = new ArrayList<>();
        for(int i = 0; i < trajectoryNum; i ++){
            int num  = pathService.choosePattern(TrajectoryRateNum, patternChooser);
            int maxage = Integer.parseInt(patternsAttributeList.get(num).get("maxage").toString());
            int minAge = Integer.parseInt(patternsAttributeList.get(num).get("minage").toString());
            int drivingRate = Integer.parseInt(patternsAttributeList.get(num).get("drivingrate").toString());
            int genderRate = Integer.parseInt(patternsAttributeList.get(num).get("genderrate").toString());
            String gender = "FM";
            if(Util.rand.nextInt(10) > genderRate){
                gender = "M";
            }
            var pattern = patternsList.get(num);
            String TraName = patternsAttributeList.get(num).get("patternname").toString();
            int randomage = Util.rand.nextInt(maxage - minAge + 1) + minAge;
            Trajectory trajectory = pathService.getTrajectory(cityName,cityName,startTime, endTime,pattern, false, randomage, "学生",gender, true, "疫苗",drivingRate,9,TraName+ i+"POIs");
            Util.outputtheTrajectory(trajectory, TraName + i) ;
        }
        return "已生成" + trajectoryNum + "个轨迹";
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
        trajectory = pathService.getTrajectory("北京市","朝阳区","2022-03-19 00:00:00", "2022-03-23 00:00:00",list, false, 20, "学生","M", true, "疫苗",5,9,"fileName");

        //创建工作薄对象
        Util.outputtheTrajectory(trajectory, "tra");
        return "1";
    }

}
