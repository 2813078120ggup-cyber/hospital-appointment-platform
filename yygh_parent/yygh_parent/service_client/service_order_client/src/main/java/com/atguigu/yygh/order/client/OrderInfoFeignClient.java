package com.atguigu.yygh.order.client;

import com.atguigu.yygh.order.client.fallback.OrderInfoDegradeFeignClient;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

// 当前微服务通过服务名 service-orders 去调用订单服务中的“订单统计接口”。
@FeignClient(name = "service-orders", fallback = OrderInfoDegradeFeignClient.class) // 熔断类：当调用订单服务失败时，执行当前类中的方法
public interface OrderInfoFeignClient {

    @PostMapping("/api/order/orderInfo/inner/getCountMap")
    public Map<String, Object> getCountMap(@RequestBody OrderCountQueryVo orderCountQueryVo);
}
