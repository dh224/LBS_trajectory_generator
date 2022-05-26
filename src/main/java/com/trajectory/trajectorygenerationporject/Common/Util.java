package com.trajectory.trajectorygenerationporject.Common;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.uuid.Generators;
import com.trajectory.trajectorygenerationporject.POJO.*;
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
    public static String getTypeFromTypeCode(String typeCode){
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

    public static void outputPOIInformation_v4(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        recordTimeStringList.add("2022-03-05 10:30:00");
        recordTimeStringList.add("2022-03-06 10:30:00");
        List<LocalDateTime> recordTime = new ArrayList<>();
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(!tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            row.createCell(5).setCellValue(tra.path.get(k).poiid);
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"周末十点半的位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputPOIInformation_v3(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        recordTimeStringList.add("2022-11-05 20:30:00");
        recordTimeStringList.add("2022-11-06 20:30:00");
        List<LocalDateTime> recordTime = new ArrayList<>();
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(!tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            row.createCell(5).setCellValue(tra.path.get(k).poiid);
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"周末晚上的位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputPOIInformation_v2(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        recordTimeStringList.add("2022-11-02 20:30:00");
        recordTimeStringList.add("2022-11-03 20:30:00");
        recordTimeStringList.add("2022-11-04 20:30:00");
        List<LocalDateTime> recordTime = new ArrayList<>();
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(!tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            row.createCell(5).setCellValue(tra.path.get(k).poiid);
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"工作日晚上的位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }

    public static void outputPOINumInTime(List<Trajectory> trajectories){
    }

    public static void outputPOIInformation_v1(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        recordTimeStringList.add("2022-03-01 10:30:00");
        recordTimeStringList.add("2022-03-02 10:30:00");
        recordTimeStringList.add("2022-03-03 10:30:00");
        recordTimeStringList.add("2022-03-04 10:30:00");
        List<LocalDateTime> recordTime = new ArrayList<>();
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(!tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            row.createCell(5).setCellValue(tra.path.get(k).poiid);
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"工作日十点半的位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputTrafficInformation_v6(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        recordTimeStringList.add("2022-11-05 07:30:00");
        recordTimeStringList.add("2022-11-06 07:30:00");
        List<LocalDateTime> recordTime = new ArrayList<>();
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"周末7点半的交通轨迹位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputTrafficInformation_v5(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        recordTimeStringList.add("2022-11-05 08:30:00");
        recordTimeStringList.add("2022-11-06 08:30:00");
        List<LocalDateTime> recordTime = new ArrayList<>();
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"周末8点半的交通轨迹位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static double[] test(double lat, double lon, double distance, double radius) {

        lat = (lat * (Math.PI)) / 180;
        lon = (lon * (Math.PI)) / 180; // 先换算成弧度
        double result[] = new double[4];
        double rad_dist = distance / radius; // 计算X公里在地球圆周上的弧度
        double lat_min = lat - rad_dist;
        double lat_max = lat + rad_dist; // 计算纬度范围

        double lon_min, lon_max;
        // 因为纬度在-90度到90度之间，如果超过这个范围，按情况进行赋值
        if (lat_min > -(Math.PI) / 2 && lat_max < (Math.PI) / 2) {
            // 开始计算经度范围
            double lon_t = Math.asin(Math.sin(rad_dist) / Math.cos(lat));
            lon_min = lon - lon_t;
            // 同理，经度的范围在-180度到180度之间
            if (lon_min < -(Math.PI))
                lon_min += 2 * (Math.PI);
            lon_max = lon + lon_t;
            if (lon_max > (Math.PI))
                lon_max -= 2 * (Math.PI);
        } else {
            lat_min = Math.max(lat_min, -(Math.PI) / 2);
            lat_max = Math.min(lat_max, (Math.PI) / 2);
            lon_min = -(Math.PI);
            lon_max = (Math.PI);
        }
        // 最后置换成角度进行输出
        lat_min = lat_min * 180 / (Math.PI);
        lat_max = lat_max * 180 / (Math.PI);
        lon_min = lon_min * 180 / (Math.PI);
        lon_max = lon_max * 180 / (Math.PI);
        result[0] = lat_min;
        result[1] = lat_max;
        result[2] = lon_min;
        result[3] = lon_max;
        // System.out.println(lat_min);
        // System.out.println(lat_max);
        // System.out.println(lon_min);
        // System.out.println(lon_max);
        return result;
    }



    public static void outputTrafficInformation_v4(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        recordTimeStringList.add("2022-11-05 17:30:00");
        recordTimeStringList.add("2022-11-06 17:30:00");
        List<LocalDateTime> recordTime = new ArrayList<>();
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"周末5点半的交通轨迹位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputTrafficInformation_v3(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        recordTimeStringList.add("2022-03-01 17:30:00");
        recordTimeStringList.add("2022-03-02 17:30:00");
        recordTimeStringList.add("2022-03-03 17:30:00");
        recordTimeStringList.add("2022-03-04 17:30:00");
        List<LocalDateTime> recordTime = new ArrayList<>();
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"5点半的交通轨迹位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputTrafficInformation_v2(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        recordTimeStringList.add("2022-03-02 08:30:00");
        recordTimeStringList.add("2022-03-03 08:30:00");
        recordTimeStringList.add("2022-03-04 08:30:00");
        List<LocalDateTime> recordTime = new ArrayList<>();
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"8点半的交通轨迹位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputTrafficInformation_v1(List<Trajectory> trajectories) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String> recordTimeStringList = new ArrayList<>();
        List<LocalDateTime> recordTime = new ArrayList<>();
        recordTimeStringList.add("2022-03-01 07:30:00");
        recordTimeStringList.add("2022-03-02 07:30:00");
        recordTimeStringList.add("2022-02-03 07:30:00");
        recordTimeStringList.add("2022-03-04 07:30:00");
        for(int j = 0; j < recordTimeStringList.size(); j++){
            recordTime.add(LocalDateTime.parse(recordTimeStringList.get(j), dateFormatter));
        }
        int size = trajectories.size();
        int recordSize = 0;
        for(int i = 0; i< size; i++){
            var tra = trajectories.get(i);
            int pathSize = tra.path.size();
            for(int j = 0; j < recordTime.size(); j ++ ) {
                var rdTime = recordTime.get(j);
                for(int k = 0; k < pathSize; k ++){
                    var timeLine = tra.timeLine.get(k);
                    if(timeLine.compareTo(rdTime) > 0) {
                        if(tra.path.get(k).isInPoi()){
                            break;
                        }else{
                            System.out.println("aaa" + timeLine);
                            XSSFRow row = sheet.createRow(recordSize++);
                            row.createCell(0).setCellValue(tra.path.get(k).lng);
                            row.createCell(1).setCellValue(tra.path.get(k).lat);
                            row.createCell(2).setCellValue(tra.path.get(k).typeCode);
                            row.createCell(3).setCellValue(dateFormatter.format(tra.timeLine.get(k)));
                            row.createCell(4).setCellValue("[" + tra.path.get(k).lng + "," + tra.path.get(k).getLat() + "],");
                            break;
                        }
                    }
                }
            }
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"7点半的交通轨迹位置 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputHomeInformation(List<Position> homeList) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        XSSFRow row = sheet.createRow(0);
        row.createCell(0).setCellValue("lng");
        row.createCell(1).setCellValue("lat");
        row.createCell(2).setCellValue("[lng,lat]");
        row.createCell(3).setCellValue("typecode");
        int size = homeList.size();
        for(int i = 0; i < size;i ++){
            //创建工作表的行
            row = sheet.createRow(i + 1);//设置第一行，从零开始
            row.createCell(0).setCellValue(homeList.get(i).lng);
            row.createCell(1).setCellValue(homeList.get(i).lat);
            row.createCell(2).setCellValue("[" + homeList.get(i).lng + "," + homeList.get(i).getLat() + "],");
            row.createCell(3).setCellValue(homeList.get(i).typeCode);
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"当前轨迹的起始点分布 "+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }

    public static List<Trajectory> deepCopuTtrajectories(List<Trajectory> trajectories, int vacRate){
        List<Trajectory> res = new ArrayList<>();
        for(int i = 0; i < trajectories.size();i++){
            var temp = trajectories.get(i);
            Trajectory t;
            if(rand.nextInt(10) > vacRate){
                 t = new Trajectory(temp.getPatternName(), temp.getMaskRate(), "A", temp.getAge(), temp.getSex(),
                        temp.getStartTime(), temp.getEndTime(), temp.getHomeLng(), temp.getHomeLat(),temp.getHomeName());
            }else {
                t = new Trajectory(temp.getPatternName(), temp.getMaskRate(), temp.getIsVaccines(), temp.getAge(), temp.getSex(),
                        temp.getStartTime(), temp.getEndTime(), temp.getHomeLng(), temp.getHomeLat(),temp.getHomeName());
            }
            List<Position> path = new ArrayList<>();
            List<LocalDateTime> timeLine = new ArrayList<>();
            for(int j = 0; j < temp.path.size();j++){
                var tTime = LocalDateTime.parse(temp.timeLine.get(j).toString());
                var ttp = temp.path.get(j);
                var tp = new Position(ttp.getLng(), ttp.getLat(), ttp.getTypeCode());
                tp.setMask(ttp.mask);
                tp.poiid = ttp.poiid;
                path.add(tp);
                timeLine.add(tTime);
            }
            t.path = path;;
            t.timeLine = timeLine;
            res.add(t);
            if(i % 500 == 0){
                System.out.println("当前复制到第" + i);
            }
        }
        return res;
    }

    public static void readTrafileOut(InputStream inputStream) throws IOException {
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(inputStream);
        } catch (Exception e){
            System.out.println(e);
        }
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        XSSFSheet xssfSheet = workbook.getSheetAt(0);
        XSSFWorkbook Nworkbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        List<List<Double>> all = new ArrayList<>();

        //创建工作表对象
        for(int i = 1; i < xssfSheet.getPhysicalNumberOfRows(); i++){
            XSSFRow row = xssfSheet.getRow(i);
            if(row == null){
                continue;
            }
            String dis = row.getCell(0).toString();
            String ve = row.getCell(11).toString();
            var distance = Double.parseDouble(dis);
            var velocity = Double.parseDouble(ve);
            if((int)(distance / 1000) < 29){
                all.get((int)(distance / 1000)).add(velocity);
            }else{
                all.get(29).add(velocity);
            }
        }
        //文档输出
        List<Double> avg = new ArrayList<>();
        List<Integer> num = new ArrayList<>();
        for(int i = 0; i < all.size(); i ++){
            var t = all.get(i);
            double sum = 0;
            for(int j = 0; j < t.size(); j ++){
                sum += t.get(j);
            }
            avg.add(sum / t.size());
            num.add(t.size());
        }
        XSSFWorkbook newbook =new XSSFWorkbook();//这里也可以设置sheet的Name
        var newSheet = newbook.createSheet();
        var row = newSheet.createRow(0);
        var row2 = newSheet.createRow(1);
        for(int i = 0; i < all.size(); i ++){
            row.createCell(i).setCellValue(avg.get(i));
            row2.createCell(i).setCellValue(num.get(i));
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"轨迹数据" + "u"+ uuid1.toString() + ".xlsx");
        newbook.write(out);
        out.close();
    }

    public static void readTraTimeFile(InputStream inputStream) throws IOException {
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(inputStream);
        } catch (Exception e){
            System.out.println(e);
        }
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        XSSFSheet xssfSheet = workbook.getSheetAt(0);
        XSSFWorkbook Nworkbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        List<List<Double>> all = new ArrayList<>();
        for(int i = 0; i <= 28000; i += 1000){
            all.add(new ArrayList<>());
        }
        all.add(new ArrayList<>());
        //创建工作表对象
        for(int i = 1; i < xssfSheet.getPhysicalNumberOfRows(); i++){
            XSSFRow row = xssfSheet.getRow(i);
            if(row == null){
                continue;
            }
            String dis = row.getCell(0).toString();
            String ve = row.getCell(11).toString();
            var distance = Double.parseDouble(dis);
            var velocity = Double.parseDouble(ve);
            if((int)(distance / 1000) < 29){
                all.get((int)(distance / 1000)).add(velocity);
            }else{
                all.get(29).add(velocity);
            }
        }
        //文档输出
        List<Double> avg = new ArrayList<>();
        List<Integer> num = new ArrayList<>();
        for(int i = 0; i < all.size(); i ++){
            var t = all.get(i);
            double sum = 0;
            for(int j = 0; j < t.size(); j ++){
                sum += t.get(j);
            }
            avg.add(sum / t.size());
            num.add(t.size());
        }
        XSSFWorkbook newbook =new XSSFWorkbook();//这里也可以设置sheet的Name
        var newSheet = newbook.createSheet();
        var row = newSheet.createRow(0);
        var row2 = newSheet.createRow(1);
        for(int i = 0; i < all.size(); i ++){
            row.createCell(i).setCellValue(avg.get(i));
            row2.createCell(i).setCellValue(num.get(i));
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"轨迹数据" + "u"+ uuid1.toString() + ".xlsx");
        newbook.write(out);
        out.close();
    }



    public static void readTraFile(InputStream inputStream) throws IOException {
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(inputStream);
        } catch (Exception e){
            System.out.println(e);
        }
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        XSSFSheet xssfSheet = workbook.getSheetAt(0);
        XSSFWorkbook Nworkbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        for(int i = 1; i < xssfSheet.getPhysicalNumberOfRows(); i++){
            XSSFRow row = xssfSheet.getRow(i);
            if(row == null){
                continue;
            }
            String endType = row.getCell(10).toString();
            String startType = row.getCell(4).toString();
            row.createCell(8).setCellValue(Util.getTypeFromTypeCode(endType));
            row.createCell(9).setCellValue(Util.getTypeFromTypeCode(startType));
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"行程数据" + "u"+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
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
            Integer mask = (int) Double.parseDouble(row.getCell(6).toString()) ;
            LocalDateTime time = LocalDateTime.parse(row.getCell(4).toString(), dateFormatter);
            if(poiid.equals("-1")){
                poiid = null;
            }
            Position tempPosition = new Position(lng, lat, typeCode);
            tempPosition.setMask(mask);
            tempPosition.setPoiid(poiid);
            path.add(tempPosition);
            timeLine.add(time);
        }
        res.path = path;
        res.timeLine = timeLine;
        return res;
    }

    public static void outputAgeGroup(List<Trajectory> res, String fileName) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        XSSFRow row = sheet.createRow(0);
        row.createCell(0).setCellValue("pattern");
        row.createCell(1).setCellValue("age");
        for(int i = 0; i < res.size(); i ++){
            row = sheet.createRow(i + 1);
            String patternName = res.get(i).getPatternName();
            int age = res.get(i).getAge();
            row.createCell(0).setCellValue(patternName);
            row.createCell(1).setCellValue(age);
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"生成的年龄" + "u"+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }

    public static void outputHomeInformation(List<Trajectory> res, String fileName) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        XSSFRow row = sheet.createRow(0);
        int m = 0;
        int fm = 0;
        int ageSUm = 0;
        int ordinary = 0;
        int overtime = 0;
        int retiree = 0;
        int middleschoolstudent = 0;
        int pupil = 0;
        int freelancer = 0;
        for(int i = 0; i < res.size(); i ++){
            if("M".equals(res.get(i).getSex())){
                m++;
            }else fm++;
            ageSUm += res.get(i).getAge();
            String patternName = res.get(i).getPatternName();
            if(patternName.equals("ordinary worker")){
                ordinary++;
            }else if(patternName.equals("overtime worker")){
                overtime++;
            }else if(patternName.equals("retiree")){
                retiree++;
            }else if(patternName.equals("middleschool student")){
                middleschoolstudent++;
            }else if(patternName.equals("pupil")){
                pupil++;
            }else{
                freelancer++;
            }
        }
        row.createCell(0).setCellValue(m);
        row.createCell(1).setCellValue(fm);
        double avgAge = ageSUm / res.size();
        row.createCell(2).setCellValue(avgAge);
        row.createCell(3).setCellValue(ordinary);
        row.createCell(4).setCellValue(overtime);
        row.createCell(5).setCellValue(retiree);
        row.createCell(6).setCellValue(middleschoolstudent);
        row.createCell(7).setCellValue(pupil);
        row.createCell(8).setCellValue(freelancer);
        for(int i = 0; i < res.size(); i ++){
            //创建工作表的行
            row = sheet.createRow(i+1);//设置第一行，从零开始
            row.createCell(0).setCellValue("[" + res.get(i).homeLng+ "," + res.get(i).homeLat +  "],");//第一行第三列为aaaaaaaaaaaa
            row.createCell(1).setCellValue(String.valueOf(res.get(i).homeName));//第一行第一列为日期
        }
        //文档输出
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"当前生成的一些统计数据" + "u"+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputSameBus(List<List<String>> tra,List<LocalDateTime> timelines, String fileName) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        XSSFRow row = sheet.createRow(0);
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for(int i = 0; i < tra.size(); i ++){
            //创建工作表的行
            row = sheet.createRow(i+1);//设置第一行，从零开始
            row.createCell((0)).setCellValue(dateFormatter.format(timelines.get(i)));
            for(int j = 0; j < tra.get(i).size();j++){
                row.createCell(j+1).setCellValue(tra.get(i).get(j));
            }
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" + fileName + "a"+ uuid1.toString() + ".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputTrip(List<Trip> tripList, String fileName) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        //创建工作表对象
        XSSFSheet sheet = workbook.createSheet();
        XSSFRow row = sheet.createRow(0);
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for(int i = 0; i < tripList.size(); i ++){
            //创建工作表的行
            row = sheet.createRow(i+1);//设置第一行，从零开始
            row.createCell((0)).setCellValue(tripList.get(i).dis);
            row.createCell((1)).setCellValue(tripList.get(i).durationM);
            row.createCell((2)).setCellValue(tripList.get(i).durationS);
            row.createCell((3)).setCellValue(tripList.get(i).endType);
            row.createCell((4)).setCellValue(tripList.get(i).startType);
            row.createCell((5)).setCellValue(dateFormatter.format(tripList.get(i).endTime) );
            row.createCell((6)).setCellValue(dateFormatter.format(tripList.get(i).startTime));
            row.createCell((7)).setCellValue(tripList.get(i).patternName);
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\static\\" + fileName + "a"+ uuid1.toString() + ".xlsx");
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
                row.createCell(7).setCellValue(Util.getTypeFromTypeCode(trajectory.path.get(i).typeCode));
            }else{
                row.createCell((3)).setCellValue("-1");
                row.createCell(7).setCellValue("-1");
            }
            row.createCell(4).setCellValue(dateFormatter.format(trajectory.getTimeLine().get(i)));//第一行第一列为日期
            row.createCell(5).setCellValue("[" + trajectory.path.get(i).getLng()+ "," + trajectory.path.get(i).getLat() +  "],");//第一行第三列为aaaaaaaaaaaa
            row.createCell(6).setCellValue(trajectory.path.get(i).getMask());
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

    public static Position getCenterPointFromListOfCoordinates(List<POIRecord> coordinateList) {
        if (coordinateList == null) {
            return null;
        }
        int total = coordinateList.size();
        double X = 0;
        double Y = 0;
        double Z = 0;
        for (var coordinate : coordinateList) {
            double lat = Double.parseDouble(coordinate.getPosition().getLat()) * Math.PI / 180;
            double lon = Double.parseDouble(coordinate.getPosition().getLng()) * Math.PI / 180;
            X += Math.cos(lat) * Math.cos(lon);
            Y += Math.cos(lat) * Math.sin(lon);
            Z += Math.sin(lat);
        }
        X = X / total;
        Y = Y / total;
        Z = Z / total;
        double lon2 = Math.atan2(Y, X);
        double hyp = Math.sqrt(X * X + Y * Y);
        double lat2 = Math.atan2(Z, hyp);
        Position center = new Position();
        Double lngStinrg = (lon2 * 180 / Math.PI);
        Double latStinrg = (lat2 * 180 / Math.PI);
        center.setLng(lngStinrg.toString());
        center.setLat(latStinrg.toString());
        return center;
    }

    public static void outputPOINumbers(List<List<Integer>> arrList, List<LocalDateTime> times, String fileName) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var row = sheet.createRow(0);
        row.createCell(0).setCellValue("time");
        row.createCell(1).setCellValue("Home");
        row.createCell(2).setCellValue("Work");
        row.createCell(3).setCellValue("Dining");
        row.createCell(4).setCellValue("Shopping");
        row.createCell(5).setCellValue("Recreation");
        row.createCell(6).setCellValue("Education");
        row.createCell(7).setCellValue("Other");
        for(int i = 0; i < times.size(); i ++){
            row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(times.get(i).format(dateFormatter));
            row.createCell(1).setCellValue(arrList.get(i).get(0));
            row.createCell(2).setCellValue(arrList.get(i).get(1));
            row.createCell(3).setCellValue(arrList.get(i).get(2));
            row.createCell(4).setCellValue(arrList.get(i).get(3));
            row.createCell(5).setCellValue(arrList.get(i).get(4));
            row.createCell(6).setCellValue(arrList.get(i).get(5));
            row.createCell(7).setCellValue(arrList.get(i).get(6));
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"统计信息：" + fileName + uuid1.toString() +".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputTraInformation(List<List<Integer>> arrList, List<LocalDateTime> times, String fileName) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var row = sheet.createRow(0);
        row.createCell(0).setCellValue("time");
        row.createCell(1).setCellValue("ordinary worker");
        row.createCell(2).setCellValue("ordinary worker");
        row.createCell(3).setCellValue("ordinary worker");
        row.createCell(4).setCellValue("ordinary worker");
        row.createCell(5).setCellValue("ordinary worker");
        row.createCell(6).setCellValue("overtime worker");
        row.createCell(7).setCellValue("overtime worker");
        row.createCell(8).setCellValue("overtime worker");
        row.createCell(9).setCellValue("overtime worker");
        row.createCell(10).setCellValue("overtime worker");
        row.createCell(11).setCellValue("retiree");
        row.createCell(12).setCellValue("retiree");
        row.createCell(13).setCellValue("retiree");
        row.createCell(14).setCellValue("retiree");
        row.createCell(15).setCellValue("retiree");
        row.createCell(16).setCellValue("pupil");
        row.createCell(17).setCellValue("pupil");
        row.createCell(18).setCellValue("pupil");
        row.createCell(19).setCellValue("pupil");
        row.createCell(20).setCellValue("pupil");
        row.createCell(21).setCellValue("midschoolstudent");
        row.createCell(22).setCellValue("midschoolstudent");
        row.createCell(23).setCellValue("midschoolstudent");
        row.createCell(24).setCellValue("midschoolstudent");
        row.createCell(25).setCellValue("midschoolstudent");
        row.createCell(26).setCellValue("freelancer");
        row.createCell(27).setCellValue("freelancer");
        row.createCell(28).setCellValue("freelancer");
        row.createCell(29).setCellValue("freelancer");
        row.createCell(30).setCellValue("freelancer");
        for(int i = 0; i < times.size(); i ++){
            row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(times.get(i).format(dateFormatter));
            row.createCell(1).setCellValue(arrList.get(i).get(0));
            row.createCell(2).setCellValue(arrList.get(i).get(1));
            row.createCell(3).setCellValue(arrList.get(i).get(2));
            row.createCell(4).setCellValue(arrList.get(i).get(3));
            row.createCell(5).setCellValue(arrList.get(i).get(4));
            row.createCell(6).setCellValue(arrList.get(i).get(5));
            row.createCell(7).setCellValue(arrList.get(i).get(6));
            row.createCell(8).setCellValue(arrList.get(i).get(7));
            row.createCell(9).setCellValue(arrList.get(i).get(8));
            row.createCell(10).setCellValue(arrList.get(i).get(9));
            row.createCell(11).setCellValue(arrList.get(i).get(10));
            row.createCell(12).setCellValue(arrList.get(i).get(11));
            row.createCell(13).setCellValue(arrList.get(i).get(12));
            row.createCell(14).setCellValue(arrList.get(i).get(13));
            row.createCell(15).setCellValue(arrList.get(i).get(14));
            row.createCell(16).setCellValue(arrList.get(i).get(15));
            row.createCell(17).setCellValue(arrList.get(i).get(16));
            row.createCell(18).setCellValue(arrList.get(i).get(17));
            row.createCell(19).setCellValue(arrList.get(i).get(18));
            row.createCell(20).setCellValue(arrList.get(i).get(19));
            row.createCell(21).setCellValue(arrList.get(i).get(20));
            row.createCell(22).setCellValue(arrList.get(i).get(21));
            row.createCell(23).setCellValue(arrList.get(i).get(22));
            row.createCell(24).setCellValue(arrList.get(i).get(23));
            row.createCell(25).setCellValue(arrList.get(i).get(24));
            row.createCell(26).setCellValue(arrList.get(i).get(25));
            row.createCell(27).setCellValue(arrList.get(i).get(26));
            row.createCell(28).setCellValue(arrList.get(i).get(27));
            row.createCell(29).setCellValue(arrList.get(i).get(28));
            row.createCell(30).setCellValue(arrList.get(i).get(29));
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"统计信息：" + fileName + uuid1.toString() +".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputAPInformation(List<List<Integer>> arrList, List<LocalDateTime> times, String fileName) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var row = sheet.createRow(0);
        row.createCell(0).setCellValue("time");
        row.createCell(1).setCellValue("ordinary worker");
        row.createCell(2).setCellValue("overtime worker");
        row.createCell(3).setCellValue("retiree");
        row.createCell(4).setCellValue("pupil");
        row.createCell(5).setCellValue("midschoolstudent");
        row.createCell(6).setCellValue("freelancer");
        for(int i = 0; i < times.size(); i ++){
            row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(times.get(i).format(dateFormatter));
            row.createCell(1).setCellValue(arrList.get(i).get(0));
            row.createCell(2).setCellValue(arrList.get(i).get(1));
            row.createCell(3).setCellValue(arrList.get(i).get(2));
            row.createCell(4).setCellValue(arrList.get(i).get(3));
            row.createCell(5).setCellValue(arrList.get(i).get(4));
            row.createCell(6).setCellValue(arrList.get(i).get(5));
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"统计信息：" + fileName + uuid1.toString() +".xlsx");
        workbook.write(out);
        out.close();
    }
    public static void outputRadiusOfGyration(List<Trajectory> trajectories, List<Double> radiusList, String filename) throws IOException{
        XSSFWorkbook workbook=new XSSFWorkbook();//这里也可以设置sheet的Name
        XSSFSheet sheet = workbook.createSheet();
        for(int i = 0; i < trajectories.size(); i ++){
            var row = sheet.createRow(i);
            row.createCell(0).setCellValue(trajectories.get(i).getId());
            row.createCell(1).setCellValue(radiusList.get(i));
            row.createCell(2).setCellValue(trajectories.get(i).getPatternName());
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" +"回转半径" +filename + uuid1.toString() +".xlsx");
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

    public static void outputSimulationResult(List<LocalDateTime> timeLine, List<List<Integer>> res, double r0) throws IOException{
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var attribute = sheet.createRow(0);
        attribute.createCell(0).setCellValue(r0);
        for(int i = 1; i < timeLine.size(); i ++){
            var row = sheet.createRow(i);
            row.createCell(0).setCellValue(i);
            row.createCell(1).setCellValue(timeLine.get(i).format(dateFormatter));
            row.createCell(2).setCellValue(res.get(i).get(0));//seiar
            row.createCell(3).setCellValue(res.get(i).get(1));
            row.createCell(4).setCellValue(res.get(i).get(2));
            row.createCell(5).setCellValue(res.get(i).get(3));
            row.createCell(6).setCellValue(res.get(i).get(4));//i + r
            row.createCell(7).setCellValue(res.get(i).get(2) + res.get(i).get(3));
            row.createCell(8).setCellValue(res.get(i).get(5));// walk
            row.createCell(9).setCellValue(res.get(i).get(6));// metro
            row.createCell(10).setCellValue(res.get(i).get(7));// bus
            row.createCell(11).setCellValue(res.get(i).get(8));// bicy
            row.createCell(12).setCellValue(res.get(i).get(9));// poi
        }
        UUID uuid1 = Generators.timeBasedGenerator().generate();
        FileOutputStream out = new FileOutputStream(".\\src\\main\\resources\\result\\" + "当前模拟的结果uid "+ uuid1.toString() + ".xlsx");
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
    public static void recordTraInformation(List<Trajectory> trajectories, String startT, String endT, String fileName) throws IOException{
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var startTime = LocalDateTime.parse(startT, dateFormatter);
        var endTime = LocalDateTime.parse(endT, dateFormatter);
        List<List<Integer>> arrList = new ArrayList<>();
        List<List<Integer>> depList = new ArrayList<>();
        List<LocalDateTime> localDateTimeList = new ArrayList<>();
        var curTime = startTime;
        List<Integer> latestPositionIndex = new ArrayList<>();
        List<Position> lastPosition = new ArrayList<>();
        List<List<Integer>> poiN = new ArrayList<>();
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

            int home = 0;
            int work = 0;
            int dining = 0;
            int shopping = 0;
            int recreation = 0;
            int education = 0;
            int other = 0;

            int ordinaryworkWalk = 0;
            int ordinaryworkb = 0;
            int ordinaryworkmetro = 0;
            int ordinaryworkbus = 0;
            int ordinaryworkdriving = 0;

            int overtimeWalk = 0;
            int overtimeb = 0;
            int overtimemetro = 0;
            int overtimebus = 0;
            int overtimedriving = 0;

            int retireeWalk = 0;
            int retireeb = 0;
            int retireemetro = 0;
            int retireebus = 0;
            int retireedriving = 0;

            int midWalk = 0;
            int midb = 0;
            int midmetro = 0;
            int midbus = 0;
            int middriving = 0;

            int pupilWalk = 0;
            int pupilb = 0;
            int pupilmetro = 0;
            int pupilbus = 0;
            int pupildriving = 0;

            int freelancerwalk = 0;
            int freelancerb = 0;
            int freelancermetro = 0;
            int freelancerbus = 0;
            int freelancerdriving = 0;
            for(int i = 0; i < size; i ++){
                var tra = trajectories.get(i);
                if (tra.path.get(latestPositionIndex.get(i)).isInPoi()) {
                    if(getTypeFromTypeCode(tra.path.get(latestPositionIndex.get(i)).getTypeCode()).equals("home")){
                        home++;
                    }else if(getTypeFromTypeCode(tra.path.get(latestPositionIndex.get(i)).getTypeCode()).equals("dining")){
                        dining++;
                    }else if(getTypeFromTypeCode(tra.path.get(latestPositionIndex.get(i)).getTypeCode()).equals("publicShopping") ||
                            getTypeFromTypeCode(tra.path.get(latestPositionIndex.get(i)).getTypeCode()).equals("normalShopping")  ){
                        shopping++;
                    }else if(getTypeFromTypeCode(tra.path.get(latestPositionIndex.get(i)).getTypeCode()).equals("daytimeRecreation") ||
                            getTypeFromTypeCode(tra.path.get(latestPositionIndex.get(i)).getTypeCode()).equals("nighttimeRecreation")){
                        recreation++;
                    }else if(getTypeFromTypeCode(tra.path.get(latestPositionIndex.get(i)).getTypeCode()).equals("publicScienceAndEducation")){
                        education++;
                    }else if(getTypeFromTypeCode(tra.path.get(latestPositionIndex.get(i)).getTypeCode()).equals("other")){
                        other++;
                    }else if(getTypeFromTypeCode(tra.path.get(latestPositionIndex.get(i)).getTypeCode()).equals("work")){
                        work++;
                    }
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
                    if(tra.getPatternName().equals("ordinary worker")){
                        if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("motro")){
                            ordinaryworkmetro++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bus")){
                            ordinaryworkbus++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("walking")){
                            ordinaryworkWalk++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("driving")){
                            ordinaryworkdriving++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bicycling")){
                            ordinaryworkb++;
                        }
                    }else if(tra.getPatternName().equals("overtime worker")){
                        if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("motro")){
                            overtimemetro++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bus")){
                            overtimebus++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("walking")){
                            overtimeWalk++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("driving")){
                            overtimedriving++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bicycling")){
                            overtimeb++;
                        }
                    }else if(tra.getPatternName().equals("retiree")){
                        if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("motro")){
                            retireemetro++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bus")){
                            retireebus++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("walking")){
                            retireeWalk++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("driving")){
                            retireedriving++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bicycling")){
                            retireeb++;
                        }
                    }else if(tra.getPatternName().equals("middleschool student")){
                        if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("motro")){
                            midmetro++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bus")){
                            midbus++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("walking")){
                            midWalk++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("driving")){
                            middriving++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bicycling")){
                            midb++;
                        }

                    }else if(tra.getPatternName().equals("pupil")){
                        if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("motro")){
                            pupilmetro++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bus")){
                            pupilbus++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("walking")){
                            pupilWalk++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("driving")){
                            pupildriving++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bicycling")){
                            pupilb++;
                        }

                    }else{
                        if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("motro")){
                            freelancermetro++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bus")){
                            freelancerbus++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("walking")){
                            freelancerwalk++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("driving")){
                            freelancerdriving++;
                        }else if(tra.path.get(latestPositionIndex.get(i)).getTypeCode().equals("bicycling")){
                            freelancerb++;
                        }

                    }
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
            List<Integer> poiList = new ArrayList<>();
            poiList.add(home);
            poiList.add(work);
            poiList.add(dining);
            poiList.add(shopping);
            poiList.add(recreation);
            poiList.add(education);
            poiList.add(other);
            poiN.add(poiList);

            List<Integer> tempArrList = new ArrayList<>();
            tempArrList.add(ordinaryworkWalk);
            tempArrList.add(ordinaryworkb);
            tempArrList.add(ordinaryworkbus);
            tempArrList.add(ordinaryworkmetro);
            tempArrList.add(ordinaryworkdriving);
            tempArrList.add(overtimeWalk);
            tempArrList.add(overtimeb);
            tempArrList.add(overtimebus);
            tempArrList.add(overtimemetro);
            tempArrList.add(overtimedriving);
            tempArrList.add(retireeWalk);
            tempArrList.add(retireeb);
            tempArrList.add(retireebus);
            tempArrList.add(retireemetro);
            tempArrList.add(retireedriving);
            tempArrList.add(midWalk);
            tempArrList.add(midb);
            tempArrList.add(midbus);
            tempArrList.add(midmetro);
            tempArrList.add(middriving);
            tempArrList.add(pupilWalk);
            tempArrList.add(pupilb);
            tempArrList.add(pupilbus);
            tempArrList.add(pupilmetro);
            tempArrList.add(pupildriving);
            tempArrList.add(freelancerwalk);
            tempArrList.add(freelancerb);
            tempArrList.add(freelancerbus);
            tempArrList.add(freelancermetro);
            tempArrList.add(freelancerdriving);
            arrList.add(tempArrList);
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
        Util.outputPOINumbers(poiN,localDateTimeList,fileName + "POI数量");
        Util.outputTraInformation(arrList, localDateTimeList, fileName);
    }
}
