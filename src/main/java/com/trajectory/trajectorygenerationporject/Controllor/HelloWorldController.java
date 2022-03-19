package com.trajectory.trajectorygenerationporject.Controllor;

import com.trajectory.trajectorygenerationporject.Common.Util;
import com.trajectory.trajectorygenerationporject.POJO.Trajectory;
import com.trajectory.trajectorygenerationporject.Service.CityService;
import com.trajectory.trajectorygenerationporject.Service.POIsService;
import com.trajectory.trajectorygenerationporject.Service.PathService;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileOutputStream;
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
        Trajectory trajectory;
        trajectory = pathService.getTrajectoriy("北京市","朝阳区","2022-03-19 00:00:00", "2022-03-23 00:00:00",list, false, 20, "学生","M", true, "疫苗",5,9);
        //创建工作薄对象
        Util.outputtheTrajectory(trajectory, "tra");
        return "1";
    }

}
