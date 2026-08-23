package com.atguigu.yygh.orders.mapper;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderCountVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 订单表 Mapper 接口
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    //统计每天平台预约数量
    List<OrderCountVo> selectOrderCount(OrderCountQueryVo orderCountQueryVo);

}
