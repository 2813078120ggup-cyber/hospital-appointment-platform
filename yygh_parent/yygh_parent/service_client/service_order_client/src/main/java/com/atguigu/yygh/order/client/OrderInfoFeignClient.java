package com.atguigu.yygh.order.client;

import com.atguigu.yygh.order.client.fallback.OrderInfoDegradeFeignClient;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "service-orders", fallback = OrderInfoDegradeFeignClient.class)
public interface OrderInfoFeignClient {

    @PostMapping("/api/order/orderInfo/inner/getCountMap")
    public Map<String, Object> getCountMap(@RequestBody OrderCountQueryVo orderCountQueryVo);
}
