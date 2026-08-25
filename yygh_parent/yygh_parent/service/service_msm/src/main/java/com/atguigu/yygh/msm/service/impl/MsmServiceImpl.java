package com.atguigu.yygh.msm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.msm.service.MsmService;
import com.atguigu.yygh.msm.utils.HttpUtils;
import com.atguigu.yygh.vo.msm.MsmVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MsmServiceImpl implements MsmService {

    @Value("${aliyun.sms.appcode:}")
    private String appCode;

    @Value("${aliyun.sms.host}")
    private String host;

    @Value("${aliyun.sms.path}")
    private String path;

    @Value("${aliyun.sms.template-id}")
    private String defaultTemplateId;

    /**
     * 功能完善：MQ 通知不再打印“发送成功”，而是实际调用短信供应商并按返回值确认结果。
     */
    @Override
    public boolean send(MsmVo msmVo) {
        if (msmVo == null || !StringUtils.hasText(msmVo.getPhone())) {
            throw new YyghException(20001, "短信手机号不能为空");
        }
        String content = buildTemplateContent(msmVo.getParam());
        String templateId = StringUtils.hasText(msmVo.getTemplateCode())
                ? msmVo.getTemplateCode() : defaultTemplateId;
        return sendMessage(msmVo.getPhone(), content, templateId);
    }

    @Override
    public boolean sendMsm(String phone, String code) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            throw new YyghException(20001, "手机号和验证码不能为空");
        }
        return sendMessage(phone, "code:" + code, defaultTemplateId);
    }

    private boolean sendMessage(String phone, String content, String templateId) {
        if (!StringUtils.hasText(appCode)) {
            // 缺失凭据时明确失败，让 MQ 保留重试语义，禁止伪造成功结果。
            throw new YyghException(20001, "短信服务未配置 ALIYUN_SMS_APPCODE");
        }
        if (!StringUtils.hasText(templateId)) {
            throw new YyghException(20001, "短信模板未配置");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "APPCODE " + appCode);
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<>();
        Map<String, String> bodys = new HashMap<>();
        bodys.put("content", content);
        bodys.put("template_id", templateId);
        bodys.put("phone_number", phone);

        try {
            HttpResponse response = HttpUtils.doPost(host, path, "POST", headers, querys, bodys);
            if (response == null || response.getEntity() == null) {
                throw new YyghException(20001, "短信服务无响应");
            }
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            JSONObject resultJson = JSONObject.parseObject(result);
            if (resultJson != null && "OK".equalsIgnoreCase(resultJson.getString("status"))) {
                return true;
            }
            String reason = resultJson == null ? "未知错误" : resultJson.getString("reason");
            throw new YyghException(20001, "短信发送失败：" + reason);
        } catch (YyghException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("调用短信服务失败", exception);
            throw new YyghException(20001, "短信发送失败");
        }
    }

    private String buildTemplateContent(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        // 保持 key:value 形式以兼容现有阿里云市场模板参数协议。
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }
}
