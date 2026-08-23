package com.atguigu.yygh.orders.controller;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.enums.PaymentTypeEnum;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.orders.service.PaymentInfoService;
import com.atguigu.yygh.orders.service.WeixinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/order/weixin")
public class WeixinController {

    @Autowired
    private WeixinService weixinService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    //生成微信支付二维码
    @GetMapping("/createNative/{orderId}")
    public R createNative(@PathVariable("orderId") Long orderId) {
        Map<String,Object> map = weixinService.createNative(orderId);
        return R.ok().data(map);
    }

    //根据订单id查询支付状态，根据微信接口返回结果决定后续处理
    //@ApiOperation(value = "查询支付状态")
    @GetMapping("/queryPayStatus/{orderId}")
    public R queryPayStatus(@PathVariable("orderId") Long orderId) {
        //1 根据订单id查询微信支付状态接口
        Map<String, String> resultMap = weixinService.queryPayStatus(orderId);

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
            //TODO 调用医院系统接口同步支付状态
            return R.ok().message("支付成功");
        }

        //2.3 支付中
        return R.ok().message("支付中");
    }
}
