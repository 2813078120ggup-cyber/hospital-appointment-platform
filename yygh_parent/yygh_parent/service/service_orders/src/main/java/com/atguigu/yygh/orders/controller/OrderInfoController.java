package com.atguigu.yygh.orders.controller;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.utils.AuthContextHolder;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/api/order/orderInfo")
public class OrderInfoController {

    @Autowired
    OrderInfoService orderInfoService;

    @PostMapping("auth/submitOrder/{scheduleId}/{patientId}")
    public R submitOrder(@PathVariable("scheduleId") String scheduleId,
                         @PathVariable("patientId") Long patientId,
                         HttpServletRequest request) {
        Long orderId = orderInfoService.createOrder(scheduleId, patientId, requireUserId(request));
        return R.ok().data("orderId", orderId);
    }

    @GetMapping("auth/getOrders/{orderId}")
    public R getOrders(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderId, requireUserId(request));
        return R.ok().data("orderInfo", orderInfo);
    }

    /**
     * 当前登录用户的挂号订单列表。
     * 用户 ID 从 token 中解析，接口不接受 userId 参数，避免越权查询。
     */
    @GetMapping("auth/{page}/{limit}")
    public R list(@PathVariable("page") Long page,
                  @PathVariable("limit") Long limit,
                  HttpServletRequest request) {
        Long userId = requireUserId(request);

        long current = page == null || page < 1 ? 1 : page;
        long size = limit == null || limit < 1 ? 10 : Math.min(limit, 100);
        Page<OrderInfo> pageParam = new Page<>(current, size);
        IPage<OrderInfo> pageModel = orderInfoService.selectPageByUserId(pageParam, userId);
        return R.ok().data("pageModel", pageModel);
    }

    @GetMapping("auth/cancelOrder/{orderId}")
    public R cancelOrder(@PathVariable("orderId") Long orderId, HttpServletRequest request) {
        boolean flag = orderInfoService.cancelOrder(orderId, requireUserId(request));
        return R.ok().data("flag", flag);
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthContextHolder.getUserId(request);
        if (userId == null) {
            throw new YyghException(20001, "请先登录");
        }
        return userId;
    }

    @PostMapping("inner/getCountMap")
    public Map<String,Object> getCountMap(@RequestBody OrderCountQueryVo orderCountQueryVo){
        Map<String, Object> countMap = orderInfoService.getCountMap(orderCountQueryVo);
        return countMap;
    }

    /**
     * 热点值超过 QPS 阈值，返回结果
     *
     * @param hoscode
     * @param scheduleId
     * @param patientId
     * @param e
     * @return
     */
    public R submitOrderBlockHandler(String hoscode, String scheduleId, Long patientId, BlockException e) {
        return R.error().message("系统业务繁忙，请稍后下单");
    }

    //使用热点值规则 ，代码实现
    //构造方法
    public OrderInfoController() {
        initRule();
    }

    /**
     * 导入热点值限流规则
     * 也可在Sentinel dashboard界面配置（仅测试）
     */
    public void initRule() {
        ParamFlowRule pRule = new ParamFlowRule("submitOrder")//资源名称，与SentinelResource值保持一致
                //限流第一个参数   submitOrder/10000/2/3
                .setParamIdx(0)
                //单机阈值
                .setCount(5);
        // 针对 热点参数值单独设置限流 QPS 阈值，而不是全局的阈值.
        //如：1000（北京协和医院）,可以通过数据库表一次性导入，目前为测试
        //热点值规则
        ParamFlowItem item1 = new ParamFlowItem().setObject("10000")//热点值
                .setClassType(String.class.getName())//热点值类型
                .setCount(1);//热点值 QPS 阈值

        //多个热点值规则放到list集合
        List<ParamFlowItem> list = new ArrayList<>();
        list.add(item1);

        pRule.setParamFlowItemList(list);
        ParamFlowRuleManager.loadRules(Collections.singletonList(pRule));
    }

}

