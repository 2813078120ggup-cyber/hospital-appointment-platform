package com.atguigu.yygh.orders.controller;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.enums.PaymentTypeEnum;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.orders.service.PaymentInfoService;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.orders.service.WeixinService;
import com.atguigu.yygh.orders.utils.ConstantPropertiesUtils;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/order/weixin")
@Slf4j
public class WeixinController {

    @Autowired
    private WeixinService weixinService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private OrderInfoService orderInfoService;

    //生成微信支付二维码
    @GetMapping("/createNative/{orderId}")
    public R createNative(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        Map<String,Object> map = weixinService.createNative(orderId, requireUserId(request));
        return R.ok().data(map);
    }

    //根据订单id查询支付状态，根据微信接口返回结果决定后续处理
    //@ApiOperation(value = "查询支付状态")
    @GetMapping("/queryPayStatus/{orderId}")
    public R queryPayStatus(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        //1 根据订单id查询微信支付状态接口
        Map<String, String> resultMap = weixinService.queryPayStatus(orderId, requireUserId(request));

        //2 根据微信支付状态查询接口返回结果，
        //2.1 失败
        if(resultMap == null) {
            throw new YyghException(20001,"支付失败");
        }

        //2.2 成功
        if ("SUCCESS".equals(resultMap.get("trade_state"))) {
            //交易号
            String out_trade_no = resultMap.get("out_trade_no");
            //根据订单交易号，更新订单状态 和 支付记录状态： 已经支付
            paymentInfoService.paySuccess(out_trade_no,resultMap);
            boolean synced = orderInfoService.updatePayStatusToHospital(orderId);
            return synced ? R.ok().message("支付成功")
                    : R.ok().message("支付成功，医院状态同步中");
        }

        //2.3 支付中
        return R.ok().message("支付中");
    }

    /**
     * 功能完善：接收微信支付 V2 XML 回调，验证签名后幂等更新平台订单并同步医院状态。
     */
    @PostMapping(value = "/notify", produces = MediaType.APPLICATION_XML_VALUE)
    public String notify(@RequestBody String xmlBody) {
        try {
            if (!StringUtils.hasText(ConstantPropertiesUtils.PARTNERKEY)
                    || !StringUtils.hasText(ConstantPropertiesUtils.APPID)
                    || !StringUtils.hasText(ConstantPropertiesUtils.PARTNER)) {
                return notifyResponse(WXPayConstants.FAIL, "支付配置未完成");
            }
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xmlBody);
            if (!WXPayUtil.isSignatureValid(resultMap, ConstantPropertiesUtils.PARTNERKEY)
                    || !WXPayConstants.SUCCESS.equals(resultMap.get("return_code"))
                    || !WXPayConstants.SUCCESS.equals(resultMap.get("result_code"))) {
                return notifyResponse(WXPayConstants.FAIL, "回调验签失败");
            }
            String outTradeNo = resultMap.get("out_trade_no");
            PaymentInfo paymentInfo = paymentInfoService.getPaymentInfoByOutTradeNo(outTradeNo);
            if (paymentInfo == null) {
                return notifyResponse(WXPayConstants.FAIL, "支付记录不存在");
            }
            String expectedFee = paymentInfo.getTotalAmount().movePointRight(2)
                    .setScale(0, RoundingMode.UNNECESSARY).toPlainString();
            if (!ConstantPropertiesUtils.APPID.equals(resultMap.get("appid"))
                    || !ConstantPropertiesUtils.PARTNER.equals(resultMap.get("mch_id"))
                    || !expectedFee.equals(resultMap.get("total_fee"))) {
                return notifyResponse(WXPayConstants.FAIL, "回调订单信息不匹配");
            }
            paymentInfoService.paySuccess(outTradeNo, resultMap);
            if (!orderInfoService.updatePayStatusToHospital(paymentInfo.getOrderId())) {
                // 返回 FAIL 让微信按规则重试，下一次回调会继续幂等同步医院状态。
                return notifyResponse(WXPayConstants.FAIL, "医院状态同步失败");
            }
            return notifyResponse(WXPayConstants.SUCCESS, "OK");
        } catch (Exception exception) {
            log.error("处理微信支付回调失败", exception);
            return notifyResponse(WXPayConstants.FAIL, "处理失败");
        }
    }

    private String notifyResponse(String returnCode, String returnMessage) {
        try {
            return WXPayUtil.mapToXml(Map.of(
                    "return_code", returnCode,
                    "return_msg", returnMessage));
        } catch (Exception exception) {
            return "<xml><return_code><![CDATA[FAIL]]></return_code>"
                    + "<return_msg><![CDATA[处理失败]]></return_msg></xml>";
        }
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        if (userId == null) {
            throw new YyghException(20001, "请先登录");
        }
        return userId;
    }
}
