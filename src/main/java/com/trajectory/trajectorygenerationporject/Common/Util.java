package com.trajectory.trajectorygenerationporject.Common;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.uuid.Generators;
import com.trajectory.trajectorygenerationporject.POJO.OriginPath;
import com.trajectory.trajectorygenerationporject.POJO.POIs;
import com.trajectory.trajectorygenerationporject.POJO.Trajectory;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
