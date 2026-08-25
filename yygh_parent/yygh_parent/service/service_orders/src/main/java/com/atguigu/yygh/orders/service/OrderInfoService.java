package com.atguigu.yygh.orders.service;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 订单表 服务类
 */
public interface OrderInfoService extends IService<OrderInfo> {

    //下单
    Long createOrder(String scheduleId, Long patientId);

    //获取订单信息
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 分页获取当前用户的订单。userId 必须来自当前请求的认证上下文，
     * 不能由客户端传入，以避免读取其他用户的订单。
     */
    IPage<OrderInfo> selectPageByUserId(Page<OrderInfo> pageParam, Long userId);

    //取消订单
    boolean cancelOrder(Long orderId);

    //就医提醒
    void patientTips(String dateString);

    //统计
    Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo);
}
