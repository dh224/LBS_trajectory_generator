package com.trajectory.trajectorygenerationporject.Service;

import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.trajectory.trajectorygenerationporject.Common.Util;
import com.trajectory.trajectorygenerationporject.DAO.CityDAO;
import com.trajectory.trajectorygenerationporject.DAO.POIsDAO;
import com.trajectory.trajectorygenerationporject.POJO.POIs;
import com.trajectory.trajectorygenerationporject.POJO.Position;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class POIsServiceImpl implements POIsService {
    private final Log log = LogFactory.getLog(POIsServiceImpl.class);

    @Autowired
    POIsDAO poisDAO;
    @Autowired
    CityDAO cityDAO;
    @Override
    public List<POIs> POIsList() {
        return poisDAO.POIsList();
    }

    @Override
    public List<POIs> findPOIListByCityName(String cityName) {
        return poisDAO.findPOIListByCityName(cityName);
    }

    @Override
    public List<POIs> findPOIListByCityCode(String cityCode) {
        return poisDAO.findPOIListByCityCode(cityCode);
    }

    @Override
    public List<POIs> findPOIListByAdCode(String adCode) {
        return poisDAO.findPOIListByAdCode(adCode);
    }

    @Override
    public List<POIs> findPOIListByAdName(String adAdName) {
        return poisDAO.findPOIListByAdName(adAdName);
    }

    @Override
    public List<POIs> findPOIListByTypeName(String typeName) {
        return poisDAO.findPOIListByTypeName(typeName);
    }

    @Override
    public List<POIs> findPOIListByTypeCode(String typeCode) {
        return poisDAO.findPOIListByTypeCode(typeCode);
    }

    @Override
    public POIs findPOIByID(String poiid) {
        return poisDAO.findPOIByID(poiid);
    }

    @Override
    public void insertPOI(POIs pois) {
        poisDAO.insertPOI(pois);
    }

    @Override
    public void insertPOI(String POIID, String adCode, String adName, String cityCode, String cityName, String POIName, String POITypeName, String POItypeCode, String lng, String lat, String entrLng, String entrLat, String exitLng, String exitLat) {
        POIs pois = new POIs(adCode, adName, cityCode, cityName, POIID, POIName, POITypeName,
                POITypeName, lng, lat, entrLng, entrLat, exitLng, exitLat);
        System.out.println("?????????POI??????" + pois.getPOIName() + "  " + pois.getPOITypeCode());
        poisDAO.insertPOI(pois);
    }

    @Override
    public void searchPOIsAllType(String cityName, String adName) throws IOException {
        var adCode = cityDAO.findAdCodeByCityNameAndAdname(cityName, adName);
        if (adCode != null) {
            //allType           ?????????????????? ?????????????????? ?????????????????? ???????????? ???????????? ????????????    ?????????      ??????????????????  ?????????????????? ????????????  ????????????    ??????      ???????????????  ????????????    ?????????       ?????????   ?????????     ????????????    ??????      ??????       ??????       ????????????    ?????????     ????????????  ??????       ??????      ????????????
            String[] POIType = {"050000", "060000", "070000","080100", "080300", "080500", "080600", "090000", "100000", "110100", "110200", "120200", "120300", "130000" , "140100", "140400","140500", "141000", "141201", "141202", "141203", "150100", "150200", "150700", "160100", "17000", "20000"};
            for (var type : POIType) {
                for(int i = 1; i <3 ; i++){
                    JSONObject res = Util.sentGet("https://restapi.amap.com/v5/place/text?key=192b951ff8bc56e05cb476f8740a760c&types=" + type  + "&region=" + adCode + "&city_limit=true&page_num="+   i +"&page_size=25&show_fields=navi");
                    var pois = res.getJSONArray("pois");
                    for(int j =0; j < pois.size(); j++){
                        parsePOIAndInsert(pois, "typecode",j);
                    }
                }
            }
        }
    }
    @Override
    public String findRandomPOIWithCityCodeAndAdCodeAndTypeCode(String cityCode, String adCode, String typeCode, String key) throws IOException{
        Integer page_num = Util.getNormalRand(1,90);
        if(page_num < 0) page_num = - page_num;
//         = Util.rand.nextInt(20);
        //System.out.println("findRandomPOIWithCityCodeAndTypeCode" + cityCode + " typecode:" + typeCode);
        String url = "https://restapi.amap.com/v5/place/text?parameters&key=" + key + "&types=" + typeCode +
                "&region=" + adCode + "&city_limit=true&show_fields=navi&page_size=25&page_num=" + page_num;
        JSONObject res = Util.sentGet(url);
        var pois = res.getJSONArray("pois");
        int Max_num = 0;
        while(pois.size() == 0){
            if(Max_num++ > 30) return null;
            this.log.info("???????????????????????????????????????.");
            page_num = Util.getNormalRand(1,90);
            if(page_num < 0) page_num = - page_num;
            url = "https://restapi.amap.com/v5/place/text?parameters&key=" + key + "&types=" + typeCode +
                    "&region=" + adCode + "&city_limit=true&show_fields=navi&page_size=25&page_num=" + page_num;
//            url = "https://restapi.amap.com/v5/place/text?parameters&key=192b951ff8bc56e05cb476f8740a760c&types=" + typeCode +
//                    "&region=" + cityCode + "&show_fields=navi&page_size=25&page_num=" + page_num;
            res = Util.sentGet(url);
            pois = res.getJSONArray("pois");
        }
        int poi_num = Util.rand.nextInt(pois.size());
        String poiid = parsePOIAndInsert(pois, typeCode,poi_num);
        return poiid;
    }

    public String findRandomPOIWithCityCodeAndTypeCode(String cityCode, String typeCode) throws IOException {
        Integer page_num = Util.getNormalRand(1,90);
        if(page_num < 0) page_num = - page_num;
        String url = "https://restapi.amap.com/v5/place/text?parameters&key=192b951ff8bc56e05cb476f8740a760c&types=" + typeCode +
                "&region=" + cityCode + "&show_fields=navi&page_size=25&page_num=" + page_num;
//        this.log.info("????????????????????????POI url:" + url);
        JSONObject res = Util.sentGet(url);
        var pois = res.getJSONArray("pois");
        int Max_num = 0;
        while(pois.size() == 0){
            if(Max_num++ > 50) return null;
            this.log.info("???????????????????????????????????????.");
            page_num = Util.getNormalRand(1,90);
            if(page_num < 0) page_num = - page_num;
            url = "https://restapi.amap.com/v5/place/text?parameters&key=192b951ff8bc56e05cb476f8740a760c&types=" + typeCode +
                    "&region=" + cityCode + "&show_fields=navi&page_size=25&page_num=" + page_num;
            res = Util.sentGet(url);
            pois = res.getJSONArray("pois");
        }
        int poi_num = Util.rand.nextInt(pois.size());
        String poiid = parsePOIAndInsert(pois, typeCode,poi_num);
        return poiid;
    }



    @Override
    public String findRandomPOIWithCityCodeAndTypeCodeAndDistance_v3(String cityCode, String typeCode, Position basePosition, Integer radius, String key) throws IOException, NullPointerException{
        String url = "https://restapi.amap.com/v3/place/around?parameter&key=" + key + "&types=" + typeCode + "&city=" + cityCode + "&location=" +
                basePosition.getLng() + "," + basePosition.getLat() + "&radius=" + radius + "&sortrule=distance&&city_limit=true&page=1";
        if(radius >= 20000){
            if(Util.rand.nextInt(10) > 6){
                double[] result = Util.test(Double.parseDouble(basePosition.getLat()),Double.parseDouble(basePosition.getLng()),6,6371 );
                String lat = Double.toString ((Util.rand.nextDouble() * (result[1] - result[0]) + result[0]));
                String lng = Double.toString((Util.rand.nextDouble() * (result[3] - result[2]) + result[2]));
                //System.out.println("??????????????????????????????????????????: " + Util.getDistance(lng,lat,basePosition.lng,basePosition.lat));
                basePosition.setLng(lng);
                basePosition.setLat(lat);
            }
        }
        JSONObject res = Util.sentGet(url);
//        System.out.println("??????v3???url?????????????????????" + url);
        int count = res.getInteger("count");
        if(count == 0) return null;
        else {
            int num = Util.getNormalRand(0,count - 1);
            num = Util.rand.nextInt(count);
//            if(num < 0) num = - num;
////            if(num > count - 1){
////                num = count - 1;
////            }
            int pagesize = num / 20;
            url = "https://restapi.amap.com/v3/place/around?parameter&key=" + key + "&types=" + typeCode + "&city=" + cityCode + "&location=" +
                    basePosition.getLng() + "," + basePosition.getLat() + "&radius=" + radius + "&sortrule=distance&offset=20&extensions=all&city_limit=true&page=" + pagesize;
            num = num % 20;
//            System.out.println("??????v3???url?????????" + url);
            res = Util.sentGet(url);
            while(res.getInteger("count") == 0){
                url = "https://restapi.amap.com/v3/place/around?parameter&key=" + key + "&types=" + typeCode + "&city=" + cityCode + "&location=" +
                        basePosition.getLng() + "," + basePosition.getLat() + "&radius=" + radius + "&sortrule=distance&offset=20&extensions=all&city_limit=true&page=" + pagesize / 2;
                res = Util.sentGet(url);
            }
            JSONArray pois = res.getJSONArray("pois");
            if(pois.size() < num){
                num = pois.size() - 1;
            }
            JSONObject poi = pois.getJSONObject(num);
            String poiid = parsePOIAndInsert(pois, typeCode, num);
            return poiid;
        }
    }

    public String findRandomPOIWithCityCodeAndTypeCodeAndDistance(String cityCode, String typeCode, Position basePosition, Integer radius) throws IOException{
        Integer page_num = Util.getNormalRand(1,90);
        if(page_num < 0) page_num = - page_num;
        System.out.println("findRandomPOIWithCityCodeAndTypeCodeAndDistance" + radius );
        String url = "https://restapi.amap.com/v5/place/around?parameters" + "&key=192b951ff8bc56e05cb476f8740a760c&types=" + typeCode +
                "&region=" + cityCode + "&location=" + basePosition.getLng() + "," + basePosition.getLat() + "&radius=" + radius + "&city_limit=true" +"&show_fields=navi&page_size=20&page_num=" + page_num;
//        this.log.info("????????????????????????????????????POI url:" + url);
        JSONObject res = Util.sentGet(url);
        var pois = res.getJSONArray("pois");
        int Max_num = 0;
        while(pois.size() == 0){
            if(Max_num++ > 30) return null;
            this.log.info("???????????????????????????????????????.");
            page_num = Util.getNormalRand(1,90);
            if(page_num < 0) page_num = - page_num;
            url = "https://restapi.amap.com/v5/place/around?parameters" + "&key=192b951ff8bc56e05cb476f8740a760c&types=" + typeCode +
                    "&region=" + cityCode + "&location=" + basePosition.getLng() + "," + basePosition.getLat() + "&radius=" + radius + "&city_limit=true" +"&show_fields=navi&page_size=20&page_num=" + page_num;
            res = Util.sentGet(url);
            pois = res.getJSONArray("pois");
        }
        int poi_num = Util.rand.nextInt(pois.size());
        String poiid = parsePOIAndInsert(pois, typeCode,poi_num);
//        System.out.println("??????????????????" + radius);
//        System.out.println("?????????POI ??????typeCode" + typeCode);
//        System.out.println("????????????POI????????????" + findPOIByID(poiid).getPOITypeCode());
        return poiid;
    }
    public String parsePOIAndInsert(JSONArray pois, String typeCode,int index){
        var poi = pois.getJSONObject(index);
        String poiName = poi.get("name").toString();
        var poiid = poi.get("id").toString();
        var poiadCode = poi.get("adcode").toString();
        var poiadName = poi.get("adname").toString();
        var poiCitycode = poi.get("citycode").toString();
        var poiCityname = poi.get("cityname").toString();
        var poiTypeName = poi.get("type").toString();
        var poiTypeCode = poi.get("typecode").toString();
        var location = poi.get("location").toString();
        var locations = location.split(",");
        var poilng = locations[0];
        var poilat = locations[1];
        String entrLocation = null;
        String entrLng = null,entrLat = null;
        if(poi.containsKey("navi")){
            JSONObject oj = (JSONObject) poi.get("navi");
            if(oj.containsKey("entr_location")){
                entrLocation =  oj.get("entr_location").toString();
                entrLng = entrLocation.split(",")[0];
                entrLat = entrLocation.split(",")[1];
            }
        }
        POIs poIs = new POIs(poiadCode,poiadName,poiCitycode,poiCityname,poiid, poiName, poiTypeName,
                typeCode, poilng,poilat,entrLng, entrLat,null,null);
//            System.out.println("?????????POI??????!" + poIs);
            try{
                poisDAO.insertPOI(poIs);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
            String cityName = poIs.getCityName();
            String adName = poIs.getAdName();
            String adCode = poIs.getAdCode();
            cityDAO.AddOnePOINumberByAdCode(poIs.getAdCode());

            if(cityName.equals("?????????") || cityName.equals("?????????") || cityName.equals("?????????")|| adName.equals("?????????")|| cityName.equals("?????????")){
                String cityAdCode = adCode.substring(0,3);
                cityAdCode +="000";
                System.out.println("1citycode==" + cityAdCode);
                System.out.println(cityAdCode);
                cityDAO.AddOnePOINumberByAdCode(cityAdCode);
            }else{
                String cityAdCode = adCode.substring(0,4);
                cityAdCode += "00";
                cityDAO.AddOnePOINumberByAdCode(cityAdCode);
            }
        return poIs.getPOIID();
    }
}
