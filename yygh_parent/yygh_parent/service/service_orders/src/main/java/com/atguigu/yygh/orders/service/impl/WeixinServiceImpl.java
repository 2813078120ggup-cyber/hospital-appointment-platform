package com.atguigu.yygh.orders.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.enums.PaymentTypeEnum;
import com.atguigu.yygh.enums.RefundStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.model.order.RefundInfo;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.orders.service.PaymentInfoService;
import com.atguigu.yygh.orders.service.RefundInfoService;
import com.atguigu.yygh.orders.service.WeixinService;
import com.atguigu.yygh.orders.utils.ConstantPropertiesUtils;
import com.atguigu.yygh.orders.utils.HttpClient;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class WeixinServiceImpl implements WeixinService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private RefundInfoService refundInfoService;

    @Value("${weixin.pay.notify-url}")
    private String notifyUrl;

    //生成微信支付二维码
    @Override
    public Map<String, Object> createNative(Long orderId, Long userId) {
        requirePaymentConfiguration(false);
        //1 根据orderId查询订单信息
        OrderInfo orderInfo = this.packOrderInfo(requireOwnedOrder(orderId, userId));
        if (!OrderStatusEnum.UNPAID.getStatus().equals(orderInfo.getOrderStatus())) {
            throw new YyghException(20001, "当前订单状态不允许发起支付");
        }

        //2 向支付记录表添加支付记录（状态：正在支付）
        paymentInfoService.savePaymentInfo(orderInfo);

        //3 调用微信固定接口，得到二维码地址等信息
        //3.1 使用map集合封装参数
        Map<String, String> paramMap = new HashMap<>();
        //公众号appid
        paramMap.put("appid", ConstantPropertiesUtils.APPID);
        //商户号
        paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
        //随机字符串
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
        Date reserveDate = orderInfo.getReserveDate();
        String reserveDateString = new DateTime(reserveDate).toString("yyyy/MM/dd");
        String body = reserveDateString + "就诊"+ orderInfo.getDepname();
        //扫描后手机显示内容
        paramMap.put("body", body);
        //订单交易号
        paramMap.put("out_trade_no", orderInfo.getOutTradeNo());
        // 功能完善：按订单真实金额换算为分，禁止固定使用 1 分测试金额。
        paramMap.put("total_fee", toFen(orderInfo.getAmount()));
        //客户端ip
        paramMap.put("spbill_create_ip", "127.0.0.1");
        //支付成功回调地址
        paramMap.put("notify_url", notifyUrl);
        //二维码类型
        paramMap.put("trade_type", "NATIVE");
        try {
            //3.2 使用httpclient方式发送post请求微信接口
            //设置请求路径
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            //设置请求需要参数
            client.setXmlParam(WXPayUtil.generateSignedXml(paramMap,ConstantPropertiesUtils.PARTNERKEY));
            //设置其他参数
            client.setHttps(true);
            //发送post请求
            client.post();

            //获取接口返回数据
            String xml = client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            if (!WXPayConstants.SUCCESS.equals(resultMap.get("return_code"))
                    || !WXPayConstants.SUCCESS.equals(resultMap.get("result_code"))
                    || !WXPayUtil.isSignatureValid(resultMap, ConstantPropertiesUtils.PARTNERKEY)) {
                throw new YyghException(20001, "微信支付下单失败：" + safeWechatMessage(resultMap));
            }

            //4 微信接口返回数据，把返回进行封装，返回
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", orderId);
            map.put("totalFee", orderInfo.getAmount());
            map.put("resultCode", resultMap.get("result_code"));
            map.put("codeUrl", resultMap.get("code_url")); //微信二维码地址
            return map;
        } catch (Exception e) {
            if (e instanceof YyghException) {
                throw (YyghException) e;
            }
            log.error("生成微信支付二维码失败，orderId={}", orderId, e);
            throw new YyghException(20001, "生成微信支付二维码失败");
        }
    }

    //1 调用微信接口，查询订单支付状态
    @Override
    public Map<String, String> queryPayStatus(Long orderId, Long userId) {
        requirePaymentConfiguration(false);
        //根据orderId查询订单信息
        OrderInfo orderInfo = this.packOrderInfo(requireOwnedOrder(orderId, userId));
        //封装微信查询支付状态接口需要参数，使用map集合
        Map paramMap = new HashMap<>();
        //微信公众号appid
        paramMap.put("appid", ConstantPropertiesUtils.APPID);
        //商户号
        paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
        //订单交易号
        paramMap.put("out_trade_no", orderInfo.getOutTradeNo());
        //随机字符串
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
        //使用httpclient调用
        HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
        //设置参数
        try {
            client.setXmlParam(WXPayUtil.generateSignedXml(paramMap, ConstantPropertiesUtils.PARTNERKEY));
            client.setHttps(true);
            //发送post请求
            client.post();

            //获取请求结果
            String xml = client.getContent();
            Map<String, String> result = WXPayUtil.xmlToMap(xml);
            if (!WXPayConstants.SUCCESS.equals(result.get("return_code"))
                    || !WXPayUtil.isSignatureValid(result, ConstantPropertiesUtils.PARTNERKEY)) {
                throw new YyghException(20001, "查询微信支付状态失败：" + safeWechatMessage(result));
            }
            if (!orderInfo.getOutTradeNo().equals(result.get("out_trade_no"))
                    || (result.get("total_fee") != null
                    && !toFen(orderInfo.getAmount()).equals(result.get("total_fee")))) {
                throw new YyghException(20001, "微信支付状态与平台订单不匹配");
            }
            return result;
        } catch (Exception e) {
            if (e instanceof YyghException) {
                throw (YyghException) e;
            }
            log.error("查询微信支付状态失败，orderId={}", orderId, e);
            throw new YyghException(20001, "查询微信支付状态失败");
        }
    }

    //退款
    @Override
    public boolean refund(Long orderId) {
        requirePaymentConfiguration(true);
        //1 根据orderId查询支付记录表，获取支付记录
        //为了退款获取支付记录 字段：trade_no
        PaymentInfo paymentInfo = paymentInfoService.getPaymentInfoByOrderId(orderId);
        if(paymentInfo == null) {
            throw new YyghException(20001,"支付记录不存在");
        }

        //2 向退款记录表添加一条记录（状态：退款中）
        RefundInfo refundInfo = refundInfoService.savefundInfo(paymentInfo);
        //判断是否需要退款
        Integer refundStatus = refundInfo.getRefundStatus();
        //已经退款  refundStatus==2
        if(refundStatus.intValue() == RefundStatusEnum.REFUND.getStatus().intValue()) {
            return true;
        }

        //3 调用微信退款接口进行退款
        //封装微信退款接口需要参数，使用map
        Map<String,String> paramMap = new HashMap<>(8);
        paramMap.put("appid",ConstantPropertiesUtils.APPID);       //公众账号ID
        paramMap.put("mch_id",ConstantPropertiesUtils.PARTNER);   //商户编号
        paramMap.put("nonce_str",WXPayUtil.generateNonceStr());
        paramMap.put("transaction_id",paymentInfo.getTradeNo()); //微信订单号
        paramMap.put("out_trade_no",paymentInfo.getOutTradeNo()); //商户订单编号
        paramMap.put("out_refund_no","tk"+paymentInfo.getOutTradeNo()); //商户退款单号
        // 功能完善：退款金额必须与支付记录一致，避免测试常量造成账务不一致。
        String totalFee = toFen(paymentInfo.getTotalAmount());
        paramMap.put("total_fee", totalFee);
        paramMap.put("refund_fee", totalFee);
        try {
            String paramXml = WXPayUtil.generateSignedXml(paramMap,ConstantPropertiesUtils.PARTNERKEY);
            //使用httpclient调用微信退款接口
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/secapi/pay/refund");
            client.setXmlParam(paramXml);//设置xml格式参数
            client.setHttps(true);//支持https协议
            client.setCert(true); //使用证书
            client.setCertPassword(ConstantPropertiesUtils.PARTNER);//设置证明密码
            client.post();

            //4 得到微信退款接口返回数据，根据返回决定后续流程
            String xml = client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);

            //返回退款成功，更新退款记录状态（退款成功），返回true
            if (null != resultMap
                    && WXPayConstants.SUCCESS.equalsIgnoreCase(resultMap.get("result_code"))
                    && WXPayUtil.isSignatureValid(resultMap, ConstantPropertiesUtils.PARTNERKEY)) {
                //更新退款记录状态（退款成功），返回true
                refundInfo.setRefundStatus(RefundStatusEnum.REFUND.getStatus());
                refundInfo.setCallbackTime(new Date());
                refundInfo.setTradeNo(resultMap.get("refund_id"));
                refundInfo.setCallbackContent(JSONObject.toJSONString(resultMap));
                refundInfoService.updateById(refundInfo);

                //返回true
                return true;
            }
        } catch (Exception e) {
            log.error("微信退款失败，orderId={}", orderId, e);
            throw new YyghException(20001, "微信退款失败");
        }
        return false;
    }

    private OrderInfo packOrderInfo(OrderInfo orderInfo) {
        orderInfo.getParam().put("orderStatusString",
                OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus()));
        return orderInfo;
    }

    private OrderInfo requireOwnedOrder(Long orderId, Long userId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo == null || userId == null || !Objects.equals(orderInfo.getUserId(), userId)) {
            // 功能完善：支付二维码和支付状态只能由订单所属用户访问。
            throw new YyghException(20001, "无权访问该订单支付信息");
        }
        return orderInfo;
    }

    private String toFen(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new YyghException(20001, "订单金额不正确");
        }
        try {
            return amount.movePointRight(2)
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .toBigIntegerExact()
                    .toString();
        } catch (ArithmeticException exception) {
            throw new YyghException(20001, "订单金额最多支持两位小数");
        }
    }

    private void requirePaymentConfiguration(boolean requireCertificate) {
        if (!StringUtils.hasText(ConstantPropertiesUtils.APPID)
                || !StringUtils.hasText(ConstantPropertiesUtils.PARTNER)
                || !StringUtils.hasText(ConstantPropertiesUtils.PARTNERKEY)
                || !StringUtils.hasText(notifyUrl)) {
            throw new YyghException(20001, "微信支付配置不完整");
        }
        if (requireCertificate && !StringUtils.hasText(ConstantPropertiesUtils.CERT)) {
            throw new YyghException(20001, "微信退款证书未配置");
        }
    }

    private String safeWechatMessage(Map<String, String> resultMap) {
        String message = resultMap.get("err_code_des");
        return StringUtils.hasText(message) ? message : "微信接口返回失败";
    }
}
