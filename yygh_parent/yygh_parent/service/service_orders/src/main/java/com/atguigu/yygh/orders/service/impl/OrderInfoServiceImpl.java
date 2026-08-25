package com.atguigu.yygh.orders.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.hosp.client.HospitalFeignClient;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.orders.utils.HttpRequestHelper;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import com.atguigu.yygh.user.client.PatientFeignClient;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderCountVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 订单表 服务实现类
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;


    @Override
    public Long createOrder(String scheduleId, Long patientId) {
        //根据排班id获取排班数据
        ScheduleOrderVo scheduleOrderVo = hospitalFeignClient.getScheduleOrderVo(scheduleId);
        //根据就诊人id获取就诊人数据
        Patient patient = patientFeignClient.getPatientInfoById(patientId);

        //调用医院接口，封装相关数据
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("hoscode", scheduleOrderVo.getHoscode());
        paramMap.put("depcode", scheduleOrderVo.getDepcode());
        paramMap.put("hosScheduleId", scheduleOrderVo.getHosScheduleId());
        paramMap.put("reserveDate", new DateTime(scheduleOrderVo.getReserveDate()).toString("yyyy-MM-dd"));
        paramMap.put("reserveTime", scheduleOrderVo.getReserveTime());
        paramMap.put("amount", scheduleOrderVo.getAmount());

        paramMap.put("name", patient.getName());
        paramMap.put("certificatesType", patient.getCertificatesType());
        paramMap.put("certificatesNo", patient.getCertificatesNo());
        paramMap.put("sex", patient.getSex());
        paramMap.put("birthdate", patient.getBirthdate());
        paramMap.put("phone", patient.getPhone());
        paramMap.put("isMarry", patient.getIsMarry());
        paramMap.put("provinceCode", patient.getProvinceCode());
        paramMap.put("cityCode", patient.getCityCode());
        paramMap.put("districtCode", patient.getDistrictCode());
        paramMap.put("address", patient.getAddress());

        paramMap.put("contactsName", patient.getContactsName());
        paramMap.put("contactsCertificatesType", patient.getContactsCertificatesType());
        paramMap.put("contactsCertificatesNo", patient.getContactsCertificatesNo());
        paramMap.put("contactsPhone", patient.getContactsPhone());
        paramMap.put("timestamp", HttpRequestHelper.getTimestamp());
        //String sign = HttpRequestHelper.getSign(paramMap, signInfoVo.getSignKey());
        paramMap.put("sign", "");

        //调用医院接口，发送httpclient请求，下单
        JSONObject result = HttpRequestHelper.sendRequest(paramMap, "http://localhost:9998/order/submitOrder");
        if (result.getInteger("code") == 200) {
            JSONObject jsonData = result.getJSONObject("data");

            //获取医院预约记录主键
            String hosRecordId = jsonData.getString("hosRecordId");
            //预约序号
            Integer number = jsonData.getInteger("number");
            //取号时间
            String fetchTime = jsonData.getString("fetchTime");
            //取号地址
            String fetchAddress = jsonData.getString("fetchAddress");

            //保存平台挂号数据信息
            OrderInfo orderInfo = new OrderInfo();
            //封装排班信息
            BeanUtils.copyProperties(scheduleOrderVo, orderInfo);
            //封装就诊人信息
            String outTradeNo = System.currentTimeMillis() + "" + new Random().nextInt(100);
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setScheduleId(scheduleId);
            orderInfo.setUserId(patient.getUserId());
            orderInfo.setPatientId(patientId);
            orderInfo.setPatientName(patient.getName());
            orderInfo.setPatientPhone(patient.getPhone());
            orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());
            orderInfo.setHosRecordId(hosRecordId);
            orderInfo.setNumber(number);
            orderInfo.setFetchTime(fetchTime);
            orderInfo.setFetchAddress(fetchAddress);
            baseMapper.insert(orderInfo); //平台保存订单

            //排班可预约数
            Integer reservedNumber = jsonData.getInteger("reservedNumber");
            //排班剩余预约数
            Integer availableNumber = jsonData.getInteger("availableNumber");

            //TODO 根据医院返回数据，更新排班数据(TODO 给就诊人发送短信)

            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(scheduleId);
            orderMqVo.setReservedNumber(reservedNumber);
            orderMqVo.setAvailableNumber(availableNumber);

            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());

            String reserveDate = new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd")
                    + (orderInfo.getReserveTime() == 0 ? "上午" : "下午");

            Map<String, Object> param = new HashMap<>();
            param.put("title", orderInfo.getHosname() + "|" + orderInfo.getDepname() + "|" + orderInfo.getTitle());
            param.put("amount", orderInfo.getAmount());
            param.put("reserveDate", reserveDate);
            param.put("name", orderInfo.getPatientName());
            param.put("quitTime", new DateTime(orderInfo.getQuitTime()).toString("yyyy-MM-dd HH:mm"));
            msmVo.setParam(param);

            orderMqVo.setMsmVo(msmVo);
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);

            return orderInfo.getId(); //主键回填
        } else {
            System.out.println("下单失败");
            throw new YyghException(20001, "下单失败");
        }
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        return this.packOrderInfo(orderInfo);
    }

    @Override
    public IPage<OrderInfo> selectPageByUserId(Page<OrderInfo> pageParam, Long userId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId)
                .orderByDesc(OrderInfo::getCreateTime)
                .orderByDesc(OrderInfo::getId);

        IPage<OrderInfo> page = baseMapper.selectPage(pageParam, wrapper);
        page.getRecords().forEach(this::packOrderInfo);
        return page;
    }

    private OrderInfo packOrderInfo(OrderInfo orderInfo) {
        orderInfo.getParam().put("orderStatusString", OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus()));
        return orderInfo;
    }


    @Override
    public boolean cancelOrder(Long orderId) {
        //1.超过取消时间不能取消
        OrderInfo orderInfo = baseMapper.selectById(orderId);  //根据传入id查询订单信息
        DateTime dateTime = new DateTime(orderInfo.getQuitTime());  // 获取取消时间
        if (dateTime.isBeforeNow()) {
            throw new YyghException();
        }

        //2.调用医院远程接口，修改取消订单状态
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("hoscode", orderInfo.getHoscode());
        reqMap.put("hosRecordId", orderInfo.getHosRecordId()); //医院那边订单号
        reqMap.put("timestamp", HttpRequestHelper.getTimestamp());
        reqMap.put("sign", "");
        JSONObject result = HttpRequestHelper.sendRequest(reqMap, "http://localhost:9998/order/updatePayStatus");
        if (result.getInteger("code") != 200) {
            // 返回不是200则调用失败
            throw new YyghException(ResultCodeEnum.FAIL.getCode(), result.getString("message"));
        } else {
            // 调用成功，将订单状态设置为cancle
            orderInfo.setOrderStatus(OrderStatusEnum.CANCLE.getStatus());
            baseMapper.updateById(orderInfo); // 更新到数据库
            
            //发送消息更新预约数量，
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(orderInfo.getScheduleId());

            //顺便帮我携带下，给用户发短信的有关信息
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            orderMqVo.setMsmVo(msmVo);

            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo); // RabbitMQ 会根据前两个参数，把消息路由到 HospitalReceiver 监听的那个队列。
        }
        return true;
    }

    // 定时任务
    @Override
    public void patientTips(String dateString) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getReserveDate,dateString) // 预约日期
                        .ne(OrderInfo::getOrderStatus,OrderStatusEnum.CANCLE.getStatus()); // 预约状态不是取消
        
        List<OrderInfo> orderInfoList = baseMapper.selectList(wrapper); // 根据条件查询数据库
        for (OrderInfo orderInfo : orderInfoList) {
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM,MqConst.ROUTING_MSM_ITEM,msmVo);
        }
    }


    @Override
    public Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo) {
        List<OrderCountVo> orderCountVoList = orderInfoMapper.selectOrderCount(orderCountQueryVo);

        List<String> dateList = orderCountVoList.stream().map(OrderCountVo::getReserveDate).toList();
        List<Integer> countList = orderCountVoList.stream().map(OrderCountVo::getCount).toList();

        Map<String, Object> map = new HashMap<>();
        map.put("countList",countList);
        map.put("dateList",dateList);
        return map;
    }
}
