package com.atguigu.yygh.order.client.fallback;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.order.client.OrderInfoFeignClient;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OrderInfoDegradeFeignClient implements OrderInfoFeignClient {

    @Override
    public Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo) {
        throw new YyghException(20001,"远程调用-获取统计数据失败");
    }
}
