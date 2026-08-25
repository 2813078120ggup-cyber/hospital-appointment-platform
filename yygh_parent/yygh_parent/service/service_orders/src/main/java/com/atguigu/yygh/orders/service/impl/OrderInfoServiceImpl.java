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
import com.atguigu.yygh.orders.service.WeixinService;
import com.atguigu.yygh.orders.utils.HttpRequestHelper;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import com.atguigu.yygh.user.client.PatientFeignClient;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderCountVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.atguigu.yygh.vo.order.SignInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.security.SecureRandom;

/**
 * 订单表 服务实现类
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private WeixinService weixinService;


    @Override
    public Long createOrder(String scheduleId, Long patientId, Long userId) {
        if (userId == null) {
            throw new YyghException(20001, "请先登录");
        }
        //根据排班id获取排班数据
        ScheduleOrderVo scheduleOrderVo = hospitalFeignClient.getScheduleOrderVo(scheduleId);
        if (scheduleOrderVo == null) {
            throw new YyghException(20001, "排班不存在或已下架");
        }
        //根据就诊人id获取就诊人数据
        Patient patient = patientFeignClient.getPatientInfoById(patientId);
        // 功能完善：下单前再次校验就诊人归属，防止绕过患者接口后越权下单。
        if (patient == null || !Objects.equals(patient.getUserId(), userId)) {
            throw new YyghException(20001, "无权使用该就诊人下单");
        }
        SignInfoVo signInfoVo = requireSignInfo(scheduleOrderVo.getHoscode());

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
        paramMap.put("sign", HttpRequestHelper.getSignSingle(signInfoVo.getSignKey()));

        //调用医院接口，发送httpclient请求，下单
        JSONObject result = HttpRequestHelper.sendRequest(paramMap,
                normalizeBaseUrl(signInfoVo.getApiUrl()) + "/order/submitOrder");
        if (result != null && result.getInteger("code") == 200) {
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
            // 功能完善：交易号保持在支付表 30 字符限制内，并增加随机熵降低并发碰撞概率。
            String outTradeNo = System.currentTimeMillis()
                    + String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
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

            // 功能完善：通过订单消息同时更新平台排班余量，并异步发送就诊人短信。

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
    public OrderInfo getOrderInfo(Long orderId, Long userId) {
        OrderInfo orderInfo = requireOwnedOrder(orderId, userId);
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
        if (orderInfo == null) {
            return null;
        }
        orderInfo.getParam().put("orderStatusString", OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus()));
        return orderInfo;
    }


    @Override
    public boolean cancelOrder(Long orderId, Long userId) {
        //1.超过取消时间不能取消
        OrderInfo orderInfo = requireOwnedOrder(orderId, userId);
        if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.CANCLE.getStatus())) {
            return true;
        }
        DateTime dateTime = new DateTime(orderInfo.getQuitTime());  // 获取取消时间
        if (dateTime.isBeforeNow()) {
            throw new YyghException(20001, "已超过预约取消时间");
        }
        if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.GET_NUMBER.getStatus())) {
            throw new YyghException(20001, "已取号订单不能取消");
        }

        //2.调用医院远程接口，修改取消订单状态
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("hoscode", orderInfo.getHoscode());
        reqMap.put("hosRecordId", orderInfo.getHosRecordId()); //医院那边订单号
        reqMap.put("timestamp", HttpRequestHelper.getTimestamp());
        SignInfoVo signInfoVo = requireSignInfo(orderInfo.getHoscode());
        reqMap.put("sign", HttpRequestHelper.getSignSingle(signInfoVo.getSignKey()));
        // 功能完善：取消预约必须调用医院取消接口，不能误写为支付成功。
        JSONObject result = HttpRequestHelper.sendRequest(reqMap,
                normalizeBaseUrl(signInfoVo.getApiUrl()) + "/order/updateCancelStatus");
        if (result == null || result.getInteger("code") != 200) {
            // 返回不是200则调用失败
            String message = result == null ? "医院取消接口无响应" : result.getString("message");
            throw new YyghException(ResultCodeEnum.FAIL.getCode(), message);
        } else {
            if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PAID.getStatus())
                    && !weixinService.refund(orderId)) {
                // 功能完善：已支付订单只有退款成功后才更新平台取消状态。
                throw new YyghException(20001, "医院已取消预约，但微信退款未完成，请稍后重试");
            }
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

    @Override
    public boolean updatePayStatusToHospital(Long orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        if (orderInfo == null) {
            return false;
        }
        SignInfoVo signInfoVo = requireSignInfo(orderInfo.getHoscode());
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("hoscode", orderInfo.getHoscode());
        reqMap.put("hosRecordId", orderInfo.getHosRecordId());
        reqMap.put("timestamp", HttpRequestHelper.getTimestamp());
        reqMap.put("sign", HttpRequestHelper.getSignSingle(signInfoVo.getSignKey()));
        JSONObject result = HttpRequestHelper.sendRequest(reqMap,
                normalizeBaseUrl(signInfoVo.getApiUrl()) + "/order/updatePayStatus");
        return result != null && result.getInteger("code") == 200;
    }

    private OrderInfo requireOwnedOrder(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            throw new YyghException(20001, "订单参数不正确");
        }
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        if (orderInfo == null || !Objects.equals(orderInfo.getUserId(), userId)) {
            // 功能完善：订单详情、取消和支付统一执行资源归属校验。
            throw new YyghException(20001, "无权访问该订单");
        }
        return orderInfo;
    }

    private SignInfoVo requireSignInfo(String hoscode) {
        SignInfoVo signInfoVo = hospitalFeignClient.getSignInfo(hoscode);
        if (signInfoVo == null || !StringUtils.hasText(signInfoVo.getApiUrl())
                || !StringUtils.hasText(signInfoVo.getSignKey())) {
            throw new YyghException(20001, "医院接口或签名信息未配置");
        }
        return signInfoVo;
    }

    private String normalizeBaseUrl(String apiUrl) {
        return apiUrl.replaceAll("/+$", "");
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
