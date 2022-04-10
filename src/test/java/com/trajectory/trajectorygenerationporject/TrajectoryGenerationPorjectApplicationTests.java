package com.trajectory.trajectorygenerationporject;

import com.alibaba.fastjson.JSONObject;
import com.trajectory.trajectorygenerationporject.Common.Util;
import com.trajectory.trajectorygenerationporject.POJO.OriginPath;
import com.trajectory.trajectorygenerationporject.POJO.Trajectory;
import com.trajectory.trajectorygenerationporject.Service.CityService;
import com.trajectory.trajectorygenerationporject.Service.CityServiceImpl;
import com.trajectory.trajectorygenerationporject.Service.PathServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@SpringBootTest
class TrajectoryGenerationPorjectApplicationTests {
    @Autowired //类型
    CityService cityService;
    @Test
    void contextLoads() throws IOException {
        JSONObject singlePath =  Util.sentGet("https://restapi.amap.com/v5/direction/driving?parameters&key=192b951ff8bc56e05cb476f8740a760c&origin=116.504050,39.799385&destination=116.698501,39.866691&origin_id=B000A8UDSX&destination_id=B000A8UO93&show_fields=cost,polyline");
        OriginPath originPath = PathServiceImpl.getOriginPathFromJSON(singlePath, "driving");
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var curTime = LocalDateTime.parse("2022-03-17 19:03:55",dateFormatter);
        //System.out.println("总耗时预计：" + originPath.getDuration() + "   预计结束时间为：" + curTime.plusSeconds(originPath.getDuration()));


        //Trajectory res = new Trajectory(25, "学生", "男性", true, "接种疫苗");
//        System.out.println(cityService.findCityListByCityCode("0535").toString());
//        System.out.println(cityService.findCityListByCityName("烟台市").toString());
//        System.out.println(cityService.findCityListByAdName("长安区"));
//        System.out.println(cityService.findCityByAdcode("370681"));
//        System.out.println(cityService.findCityListWithPOI());

//        double ans = 0;
//        ans += Util.getDistance("116.504792", "39.799388","116.504657","39.799553");
//        ans += Util.getDistance("116.504657","39.799553","116.504049","39.800312");
//        ans += Util.getDistance("116.504049","39.800312","116.502951","39.801671");
//        ans += Util.getDistance("116.502951","39.801671","116.502396","39.80237");
//        ans += Util.getDistance("116.502396","39.80237","116.502253","39.802543");
//        ans += Util.getDistance("116.502253","39.802543","116.502218","39.802578");
//        ans += Util.getDistance("116.502218","39.802578","116.502057","39.802747");
//        ans += Util.getDistance("116.502057","39.802747","116.501497","39.803464");
//        ans += Util.getDistance("116.501497","39.803464","116.501489","39.803481");
//        ans += Util.getDistance("116.501489","39.803481","116.500421","39.804805");
//        ans += Util.getDistance("116.500421","39.804805","116.499809","39.805556");
//        ans += Util.getDistance("116.499809","39.805556","116.499644","39.80576");
//        ans += Util.getDistance("116.499644","39.80576","116.499588","39.805829");
//        ans += Util.getDistance("116.499588","39.805829","116.499401","39.806059");
//        ans += Util.getDistance("116.499401","39.806059","116.499019","39.806532");
//        ans += Util.getDistance("116.499019","39.806532","116.49862","39.807031");
//        ans += Util.getDistance("116.49862","39.807031","116.497665","39.808203");
//        ans += Util.getDistance("116.497665","39.808203","116.497105","39.808902");
//        ans += Util.getDistance("116.497105","39.808902","116.496923","39.809167");
//        ans += Util.getDistance("116.496923","39.809167","116.496784","39.809353");
//        ans += Util.getDistance("116.496784","39.809353","116.496628","39.809527");
//        ans += Util.getDistance("116.496628","39.809527","116.494787","39.81178");
//        System.out.println(ans);
    }

}
