package com.atguigu.yygh.msm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.msm.service.MsmService;
import com.atguigu.yygh.msm.utils.HttpUtils;
import com.atguigu.yygh.msm.utils.RandomUtil;
import com.atguigu.yygh.vo.msm.MsmVo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class MsmServiceImpl implements MsmService {
    
    //TODO 仅为了测试
    //发送短信实现
    @Override
    public boolean send(MsmVo msmVo) {
        System.out.println("短信发送成功...phone=" + msmVo.getPhone() + " code=1111");
        return true;
    }
    
    
    @Override
    public boolean sendMsm(String phone, String code) {
        String host = "https://dfsns.market.alicloudapi.com";
        String path = "/data/send_sms";
        String method = "POST";
        String appcode = "099a1b83be634353ae83a654639264f6";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        //根据API的要求，定义相对应的Content-Type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<String, String>();
        Map<String, String> bodys = new HashMap<String, String>();
        bodys.put("content", "code:" + code);
        bodys.put("template_id", "CST_ptdie100");  //注意，CST_ptdie100该模板ID仅为调试使用，调试结果为"status": "OK" ，即表示接口调用成功，然后联系客服报备自己的专属签名模板ID，以保证短信稳定下发
        bodys.put("phone_number", phone);
        
        
        try {
            /**
             * 重要提示如下:
             * HttpUtils请从
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
             * 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             */
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "UTF-8");
            System.out.println("短信接口返回结果：" + result);
            //解析API返回结果，判断是否发送成功
            JSONObject resultJson = JSONObject.parseObject(result);
            if (resultJson != null && "OK".equals(resultJson.getString("status"))) {
                return true;
            } else {
                throw new YyghException(20001, "短信发送失败：" + (resultJson != null ? resultJson.getString("reason") : "未知错误"));
            }
        } catch (YyghException e) {
            throw e; //自定义异常直接抛出
        } catch (Exception e) {
            e.printStackTrace();
            throw new YyghException(20001, "短信发送失败");
        }
        
    }
    

//需要赠送身份证OCR识别，vip优惠券,技术支持。请直接联系客服。
//商品说明可以在商品介绍里查看

/**
 *重要提示：
 *如您的返回结果中，没有我们接口的返回报文，或者连header的信息都打印出来了。可能是您的代码环境未能适配该请求示例。
 *那么，以下两个命令行，您可以二选一，选择一个适合你环境的加入到请求示例中。即可打印我们接口的返回报文。
 *或者直接联系客服  VX 18600814970
 *
 *System.out.println(EntityUtils.toString(response.getEntity()));
 *
 *System.out.println(response.body().string());
 */
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 

/*
    //发送短信实现
    @Override
    public boolean send(MsmVo msmVo) {
        if (!StringUtils.isEmpty(msmVo.getPhone())) {
            String code = RandomUtil.getFourBitRandom();
            return this.sendMessage(msmVo.getPhone(), code);
        }
        return false;
    }

    //发送短信方法
    private boolean sendMessage(String phone, String verifyCode) {
        String host = "https://dfsns.market.alicloudapi.com";
        String path = "/data/send_sms";
        String method = "POST";

        String appcode = "83359fd73fe94948385f570e31139105";

        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e31139105
        headers.put("Authorization", "APPCODE " + appcode);
        //根据API的要求，定义相对应的Content-Type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<String, String>();
        Map<String, String> bodys = new HashMap<String, String>();

        bodys.put("content", "code:" + verifyCode);
        bodys.put("template_id", "CST_ptdie100");  //注意，CST_ptdie100该模板ID仅为调试使用，调试结果为"status": "OK" ，即表示接口调用成功，然后联系客服报备自己的专属签名模板ID，以保证短信稳定下发
        bodys.put("phone_number", phone);

        try {
            *//**
     * 重要提示如下:
     * HttpUtils请从
     * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
     * 下载
     *
     * 相应的依赖请参照
     * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
     *//*
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "UTF-8");
            System.out.println("短信接口返回结果：" + result);
            //解析API返回结果，判断是否发送成功
            JSONObject resultJson = JSONObject.parseObject(result);
            if (resultJson != null && "OK".equals(resultJson.getString("message"))) {
                return true;
            } else {
                String errorMsg = resultJson != null ? resultJson.getString("message") : "未知错误";
                throw new YyghException(20001, "短信发送失败：" + errorMsg);
            }
        } catch (YyghException e) {
            throw e; //自定义异常直接抛出，不再被下面的catch吞掉
        } catch (Exception e) {
            e.printStackTrace();
            throw new YyghException(20001, "短信发送失败");
        }
    }*/
}
