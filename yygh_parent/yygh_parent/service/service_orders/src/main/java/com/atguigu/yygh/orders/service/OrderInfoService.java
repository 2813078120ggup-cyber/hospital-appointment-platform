package com.atguigu.yygh.orders.service;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderQueryVo;
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
    Long createOrder(String scheduleId, Long patientId, Long userId, String idempotencyKey);

    Long findOrderIdByIdempotencyKey(Long userId, String idempotencyKey);

    //获取订单信息
    OrderInfo getOrderInfo(Long orderId, Long userId);

    /**
     * 分页获取当前用户的订单。userId 必须来自当前请求的认证上下文，
     * 不能由客户端传入，以避免读取其他用户的订单。
     */
    IPage<OrderInfo> selectPageByUserId(Page<OrderInfo> pageParam, Long userId);

    // 管理端订单列表、详情与运营汇总。
    IPage<OrderInfo> selectAdminPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo);

    OrderInfo getAdminOrderInfo(Long orderId);

    Map<String, Object> getAdminSummary();

    //取消订单
    boolean cancelOrder(Long orderId, Long userId);

    // 管理端取消订单，记录运营原因和管理员身份。
    boolean cancelOrderByAdmin(Long orderId, String reason, String operator);

    // 功能完善：支付成功后把状态同步到医院系统，供轮询和支付回调复用。
    boolean updatePayStatusToHospital(Long orderId);

    /** 由 outbox worker 重试已进入失败状态的取消、退款和医院状态同步流程。 */
    boolean retryCancellationFromCompensation(Long orderId);

    //就医提醒
    void patientTips(String dateString);

    //统计
    Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo);
}
