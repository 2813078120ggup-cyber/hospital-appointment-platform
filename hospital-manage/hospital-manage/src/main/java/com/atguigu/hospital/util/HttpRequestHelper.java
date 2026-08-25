package com.atguigu.hospital.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class HttpRequestHelper {

    //private final static String signKey = "09c1ff67d1ae4999e137f34b0dff1046";

    public static void main(String[] args) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("d", "4");
        paramMap.put("b", "2");
        paramMap.put("c", "3");
        paramMap.put("a", "1");
        log.info(getSign(paramMap, ""));
    }

    /**
     *
     * @param paramMap
     * @return
     */
    public static Map<String, Object> switchMap(Map<String, String[]> paramMap) {
        Map<String, Object> resultMap = new HashMap<>();
        for (Map.Entry<String, String[]> param : paramMap.entrySet()) {
            resultMap.put(param.getKey(), param.getValue()[0]);
        }
        return resultMap;
    }

    /**
     * 请求数据获取签名
     * @param paramMap
     * @return
     */
    public static String getSign(Map<String, Object> paramMap, String signKey) {
        TreeMap<String, Object> sorted = new TreeMap<>(paramMap);
        sorted.remove("sign");
        StringBuilder str = new StringBuilder();
        for (Map.Entry<String, Object> param : sorted.entrySet()) {
            str.append(param.getValue()).append("|");
        }
        str.append(signKey);
        return MD5.encrypt(str.toString());
    }

    public static String getSignSingle(String signKey) {

        return MD5.encrypt(signKey);
    }

    /**
     * 签名校验
     * @param paramMap
     * @return
     */
    public static boolean isSignEquals(Map<String, Object> paramMap, String signKey) {
        Object sign = paramMap.get("sign");
        Object timestamp = paramMap.get("timestamp");
        if (sign == null || signKey == null || signKey.isBlank() || !isTimestampFresh(timestamp)) {
            return false;
        }
        String expected = getSignSingle(signKey);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                sign.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isTimestampFresh(Object timestamp) {
        try {
            long requestTime = Long.parseLong(String.valueOf(timestamp));
            // 功能完善：只接收五分钟内的签名请求，避免旧请求被重放。
            return Math.abs(System.currentTimeMillis() - requestTime) <= 5 * 60 * 1000L;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * 获取时间戳
     * @return
     */
    public static long getTimestamp() {
        return new Date().getTime();
    }

    /**
     * 封装同步请求
     * @param paramMap
     * @param url
     * @return
     */
    public static JSONObject sendRequest(Map<String, Object> paramMap, String url){
        try {
            //封装post参数
            StringBuilder postdata = new StringBuilder();
            for (Map.Entry<String, Object> param : paramMap.entrySet()) {
                if (postdata.length() > 0) {
                    postdata.append("&");
                }
                postdata.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(String.valueOf(param.getValue()), StandardCharsets.UTF_8));
            }
            byte[] reqData = postdata.toString().getBytes(StandardCharsets.UTF_8);
            byte[] respdata = HttpUtil.doPost(url,reqData);
            if (respdata == null) {
                return null;
            }
            return JSONObject.parseObject(new String(respdata, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("远程调用失败，url={}", url, ex);
            return null;
        }
    }
}
