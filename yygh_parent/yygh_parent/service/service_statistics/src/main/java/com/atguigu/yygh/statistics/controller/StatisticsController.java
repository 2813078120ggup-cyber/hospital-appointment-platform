package com.atguigu.yygh.statistics.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.order.client.OrderInfoFeignClient;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/statistics")
public class StatisticsController {

    @Autowired
    OrderInfoFeignClient orderInfoFeignClient;

    @GetMapping("/getCountMap")
    public R getCountMap(OrderCountQueryVo orderCountQueryVo){
        Map<String, Object> countMap = orderInfoFeignClient.getCountMap(orderCountQueryVo);
        return R.ok().data(countMap);
    }
}
