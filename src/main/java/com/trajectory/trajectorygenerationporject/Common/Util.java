package com.trajectory.trajectorygenerationporject.Common;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.uuid.Generators;
import com.trajectory.trajectorygenerationporject.POJO.OriginPath;
import com.trajectory.trajectorygenerationporject.POJO.POIs;
import com.trajectory.trajectorygenerationporject.POJO.Position;
import com.trajectory.trajectorygenerationporject.POJO.Trajectory;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;


import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

public class Util {
    static final OkHttpClient client = new OkHttpClient();
    private static final double EARTH_RADIUS = 6378.137;
    public static Random rand = new Random();
    public static JSONObject sentGet(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            Headers responseHeaders = response.headers();
//            for (int i = 0; i < responseHeaders.size(); i++) {
//                System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
//            }
            String stringRes = response.body().string();
            JSONObject jsonRes = JSONObject.parseObject(stringRes);
            return jsonRes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getNormalRand(int a, int b){
        return (int)Math.round(Math.sqrt(b)*rand.nextGaussian() + a);
    }

    public static String getTimeString(String fake){
        String[] startTimeN = fake.split("T");
        String startTime = startTimeN[0] + " " +  startTimeN[1];
        startTime = startTime.substring(0,startTime.length() - 5);
        return startTime;
    }
    public static boolean isHaveTypeCode(String typecode ,List<Map<String, Integer>> list){
        for(int i = 0; i < list.size(); i ++){
            if(list.get(i).containsKey(typecode)){
                return true;
            }
        }
        return false;
    }
    public static double getDistance(String longitude1, String latitude1, String longitude2, String latitude2) {
        double x_Lng = Double.valueOf(longitude1);
        double x_Lat = Double.valueOf(latitude1);
        double y_Lng = Double.valueOf(longitude2);
        double y_Lat = Double.valueOf(latitude2);
        // 纬度;
        double lat1 = Math.toRadians(x_Lat);
        double lat2 = Math.toRadians(y_Lat);
        // 经度
        double lng1 = Math.toRadians(x_Lng);
        double lng2 = Math.toRadians(y_Lng);
        // 纬度之差
        double a = lat1 - lat2;
        // 经度之差
        double b = lng1 - lng2;
        // 计算两点距离的公式
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(b / 2), 2)));
        // 弧长乘地球半径, 返回单位: 米
        s =  s * EARTH_RADIUS;
        return s * 1000;
    }
    public static List<Double> getMidLL(String lng1, String lat1, String lng2, String lat2, double rate){
        double x_Lng = Double.valueOf(lng1);
        double x_Lat = Double.valueOf(lat1);
        double y_Lng = Double.valueOf(lng2);
        double y_Lat = Double.valueOf(lat2);
        double distance_Lng = y_Lng - x_Lng;
        double distance_Lat = y_Lat - x_Lat;
        distance_Lng *= rate;
        distance_Lat *= rate;
        List<Double> res = new ArrayList<>();
        //四舍五入
        BigDecimal bg1 = new BigDecimal(x_Lng + distance_Lng).setScale(6, RoundingMode.UP);
        BigDecimal bg2 = new BigDecimal(x_Lat + distance_Lat).setScale(6, RoundingMode.UP);
        res.add(bg1.doubleValue());
        res.add(bg2.doubleValue());
        return res;
    }

    public static List<Trajectory> deepCopuTtrajectories(List<Trajectory> trajectories){
        List<Trajectory> res = new ArrayList<>();
        for(int i = 0; i < trajectories.size();i++){
            var temp = trajectories.get(i);
            var t = new Trajectory(temp.getPatternName(), temp.getMaskRate(), temp.getIsVaccines(), temp.getAge(), temp.getSex(),
                    temp.getStartTime(), temp.getEndTime(), temp.getHomeLng(), temp.getHomeLat(),temp.getHomeName());
            List<Position> path = new ArrayList<>();
            List<LocalDateTime> timeLine = new ArrayList<>();
            for(int j = 0; j < temp.path.size();j++){
                var tTime = LocalDateTime.parse(temp.timeLine.get(j).toString());
                var ttp = temp.path.get(j);
                var tp = new Position(ttp.getLng(), ttp.getLat(), ttp.getTypeCode());
                tp.poiid = ttp.poiid;
                path.add(tp);
                timeLine.add(tTime);
            }
            t.path = path;;
            t.timeLine = timeLine;
            res.add(t);
            System.out.println("当前复制到第" + i);
        }
        return res;
    }

    public static Trajectory readExcelFile(InputStream inputStream){
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(inputStream);
        } catch (Exception e){
            System.out.println(e);
        }
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        XSSFSheet xssfSheet = workbook.getSheetAt(0);
        XSSFRow attribute = xssfSheet.getRow(0);
        String patternName = attribute.getCell(0).toString();
        int age = (int)Double.parseDouble(attribute.getCell(1).toString()) ;
        String sex = attribute.getCell(2).toString();
        int maskRate = (int)Double.parseDouble(attribute.getCell(3).toString());
        String vicc = attribute.getCell(4).toString();
        String homeLng = attribute.getCell(5).toString();
        String homeLat = attribute.getCell(6).toString();
        String homeName = attribute.getCell(7).toString();
        LocalDateTime startTime = LocalDateTime.parse(attribute.getCell(9).toString(), dateFormatter);
        LocalDateTime endTime = LocalDateTime.parse(attribute.getCell(10).toString(), dateFormatter);
        var res = new Trajectory(patternName, maskRate, vicc, age, sex, startTime, endTime, homeLng, homeLat, homeName);
        List<Position> path = new ArrayList<>();
        List<LocalDateTime> timeLine = new ArrayList<>();
        for(int i = 1; i < xssfSheet.getPhysicalNumberOfRows(); i++){
            XSSFRow row = xssfSheet.getRow(i);
            if(row == null){
                continue;
            }
            String lng = row.getCell(0).toString();
            String lat = row.getCell(1).toString();
            String typeCode = row.getCell(2).toString();
            String poiid = row.getCell(3).toString();
            LocalDateTime time = LocalDateTime.parse(row.getCell(4).toString(), dateFormatter);
            if(poiid.equals("-1")){
                poiid = null;
            }
            Position tempPosition = new Position(lng, lat, typeCode);
            tempPosition.setPoiid(poiid);
            path.add(tempPosition);
            timeLine.add(time);
        }
        res.path = path;
        res.timeLine = timeLine;
        return res;
    }

    public static void outputHomeInformation(List<Trajectory> res, String fileName) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        XSSFRow row = sheet.createRow(0);
        for(int i = 0; i < res.size(); i ++){
            //创建工作表的行
            row = sheet.createRow(i+1);//设置第一行，从零开始
            row.createCell(0).setCellValue("[" + res.get(i).homeLng+ "," + res.get(i).homeLat +  "],");//第一行第三列为aaaaaaaaaaaa
            row.createCell(1).setCellValue(String.valueOf(res.get(i).homeName));//第一行第一列为日期
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\static\\" +"当前批次生成的轨迹的起始点" + "u"+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }

    public static void outputReuseTrajectory(Trajectory trajectory, String fileName) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        XSSFRow row = sheet.createRow(0);
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        row.createCell(0).setCellValue(trajectory.patternName);// 模式名
        row.createCell(1).setCellValue(trajectory.age);//
        row.createCell(2).setCellValue(trajectory.sex);//第一行第一列为日期
        row.createCell(3).setCellValue(trajectory.maskRate);//第一行第一列为日期
        row.createCell(4).setCellValue(trajectory.isVaccines);//第一行第一列为日期
        row.createCell(5).setCellValue(trajectory.homeLng);//第一行第一列为日期
        row.createCell(6).setCellValue(trajectory.homeLat);//第一行第一列为日期
        row.createCell(7).setCellValue(trajectory.homeName);//第一行第一列为日期
        row.createCell(8).setCellValue(trajectory.path.size());
        row.createCell(9).setCellValue(dateFormatter.format(trajectory.startTime).toString());
        row.createCell(10).setCellValue(dateFormatter.format(trajectory.endTime).toString());
        for(int i = 0; i < trajectory.path.size(); i ++){
            //创建工作表的行
            row = sheet.createRow(i+1);//设置第一行，从零开始
            row.createCell((0)).setCellValue(trajectory.path.get(i).getLng());
            row.createCell((1)).setCellValue(trajectory.path.get(i).getLat());
            row.createCell((2)).setCellValue(trajectory.path.get(i).getTypeCode());
            if(trajectory.path.get(i).isInPoi()){
                row.createCell((3)).setCellValue(trajectory.path.get(i).getPoiid());
            }else{
                row.createCell((3)).setCellValue("-1");
            }
            row.createCell(4).setCellValue(dateFormatter.format(trajectory.getTimeLine().get(i)));//第一行第一列为日期
            row.createCell(5).setCellValue("[" + trajectory.path.get(i).getLng()+ "," + trajectory.path.get(i).getLat() +  "],");//第一行第三列为aaaaaaaaaaaa
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\static\\" + fileName + "uid"+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }

    public static void outputtheTrajectory(Trajectory trajectory, String filenName) throws IOException {
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        XSSFRow row = sheet.createRow(0);
        row.createCell(2).setCellValue(trajectory.age);//第一行第一列为日期
        row.createCell(3).setCellValue(trajectory.sex);//第一行第一列为日期
        for(int i = 0; i < trajectory.path.size(); i ++){
            //创建工作表的行
            row = sheet.createRow(i+1);//设置第一行，从零开始
            row.createCell(0).setCellValue("[" + trajectory.path.get(i).getLng()+ "," + trajectory.path.get(i).getLat() +  "],");//第一行第三列为aaaaaaaaaaaa
            row.createCell(1).setCellValue(String.valueOf(trajectory.timeLine.get(i)));//第一行第一列为日期
            row.createCell(4).setCellValue(trajectory.path.get(i).getTypeCode());//第一行第一列为日期
        }
        //文档输出
        System.out.println("生成轨迹");
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\static\\" +filenName + "u"+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }

    public static void outputtheTrajectoryPOIS(Map<String, POIs> poIsMap, String filenName) throws IOException {
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        XSSFSheet sheet = workbook.createSheet();
        Set<String> allKeys = poIsMap.keySet();
        int i = 0;
        for(var key : allKeys){
            XSSFRow row = sheet.createRow(i++);//设置第一行，从零开始
            row.createCell(0).setCellValue(poIsMap.get(key).getPOIName());
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\static\\" +filenName + "u"+ uuid1.toString() +".xlsx");
        workbook.write(out);
        out.close();
    }

    public static void outputSimulationResult(List<LocalDateTime> timeLine, List<List<Integer>> res, String fileName) throws IOException{
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet();
        for(int i = 0; i < timeLine.size(); i ++){
            var row = sheet.createRow(i);
            row.createCell(0).setCellValue(i);
            row.createCell(1).setCellValue(timeLine.get(i).toString());
            row.createCell(2).setCellValue(res.get(i).get(0));//seiar
            row.createCell(3).setCellValue(res.get(i).get(1));
            row.createCell(4).setCellValue(res.get(i).get(2));
            row.createCell(5).setCellValue(res.get(i).get(3));
            row.createCell(6).setCellValue(res.get(i).get(4));//i + r
            row.createCell(7).setCellValue(res.get(i).get(2) + res.get(i).get(3));
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\static\\" +fileName+ "当前模拟的结果uid "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static Set<Integer> getRandomsNoRepeat(int start, int end, int count){
        if(start > end || count < 1){
            count = 0;
        }
        Set<Integer> res = new HashSet<>();
        if(count>0){
            while(res.size() < count){
                res.add(start + rand.nextInt(end - start));
            }
        }
        return res;
    }
}
