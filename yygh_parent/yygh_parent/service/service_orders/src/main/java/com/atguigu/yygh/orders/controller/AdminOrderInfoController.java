package com.atguigu.yygh.orders.controller;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.enums.PaymentStatusEnum;
import com.atguigu.yygh.enums.RefundStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.model.order.RefundInfo;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.orders.service.PaymentInfoService;
import com.atguigu.yygh.orders.service.RefundInfoService;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台管理端订单运营接口。
 */
@RestController
@RequestMapping("/admin/order/orderInfo")
public class AdminOrderInfoController {

    private final OrderInfoService orderInfoService;
    private final PaymentInfoService paymentInfoService;
    private final RefundInfoService refundInfoService;

    public AdminOrderInfoController(OrderInfoService orderInfoService,
                                    PaymentInfoService paymentInfoService,
                                    RefundInfoService refundInfoService) {
        this.orderInfoService = orderInfoService;
        this.paymentInfoService = paymentInfoService;
        this.refundInfoService = refundInfoService;
    }

    @GetMapping("/{page}/{limit}")
    public R list(@PathVariable Long page,
                  @PathVariable Long limit,
                  OrderQueryVo orderQueryVo) {
        long current = page == null || page < 1 ? 1 : page;
        long size = limit == null || limit < 1 ? 10 : Math.min(limit, 100);
        Page<OrderInfo> pageParam = new Page<>(current, size);
        IPage<OrderInfo> pageModel = orderInfoService.selectAdminPage(pageParam, orderQueryVo);
        return R.ok().data("pageModel", pageModel);
    }

    @GetMapping("/show/{orderId}")
    public R show(@PathVariable Long orderId) {
        OrderInfo orderInfo = orderInfoService.getAdminOrderInfo(orderId);
        PaymentInfo paymentInfo = paymentInfoService.getPaymentInfoByOrderId(orderId);
        RefundInfo refundInfo = refundInfoService.getOne(new LambdaQueryWrapper<RefundInfo>()
                .eq(RefundInfo::getOrderId, orderId)
                .orderByDesc(RefundInfo::getId)
                .last("LIMIT 1"));
        return R.ok().data("orderInfo", orderInfo)
                .data("paymentInfo", safePayment(paymentInfo))
                .data("refundInfo", safeRefund(refundInfo));
    }

    @GetMapping("/summary")
    public R summary() {
        return R.ok().data("summary", orderInfoService.getAdminSummary());
    }

    @GetMapping("/statusList")
    public R statusList() {
        return R.ok().data("list", OrderStatusEnum.getStatusList());
    }

    @PostMapping("/cancel/{orderId}")
    public R cancel(@PathVariable Long orderId,
                    @RequestBody Map<String, String> requestBody,
                    HttpServletRequest request) {
        Long adminId = AuthContextHolder.getUserId(request);
        if (!Long.valueOf(0L).equals(adminId)) {
            throw new YyghException(20001, "没有订单运营权限");
        }
        String operator = AuthContextHolder.getUserName(request);
        if (!StringUtils.hasText(operator)) {
            operator = "平台管理员";
        }
        String reason = requestBody == null ? null : requestBody.get("reason");
        orderInfoService.cancelOrderByAdmin(orderId, reason, operator);
        return R.ok().message("订单取消处理完成");
    }

    private Map<String, Object> safePayment(PaymentInfo paymentInfo) {
        if (paymentInfo == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("outTradeNo", paymentInfo.getOutTradeNo());
        result.put("tradeNo", paymentInfo.getTradeNo());
        result.put("totalAmount", paymentInfo.getTotalAmount());
        result.put("paymentStatus", paymentInfo.getPaymentStatus());
        result.put("paymentStatusString", statusName(paymentInfo.getPaymentStatus(), true));
        result.put("callbackTime", paymentInfo.getCallbackTime());
        result.put("createTime", paymentInfo.getCreateTime());
        return result;
    }

    private Map<String, Object> safeRefund(RefundInfo refundInfo) {
        if (refundInfo == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("outTradeNo", refundInfo.getOutTradeNo());
        result.put("tradeNo", refundInfo.getTradeNo());
        result.put("totalAmount", refundInfo.getTotalAmount());
        result.put("refundStatus", refundInfo.getRefundStatus());
        result.put("refundStatusString", statusName(refundInfo.getRefundStatus(), false));
        result.put("callbackTime", refundInfo.getCallbackTime());
        result.put("createTime", refundInfo.getCreateTime());
        return result;
    }

    private String statusName(Integer status, boolean payment) {
        if (status == null) {
            return "未知";
        }
        if (payment) {
            for (PaymentStatusEnum item : PaymentStatusEnum.values()) {
                if (status.equals(item.getStatus())) {
                    return item.getName();
                }
            }
        } else {
            for (RefundStatusEnum item : RefundStatusEnum.values()) {
                if (status.equals(item.getStatus())) {
                    return item.getName();
                }
            }
        }
        return "未知";
    }
}
