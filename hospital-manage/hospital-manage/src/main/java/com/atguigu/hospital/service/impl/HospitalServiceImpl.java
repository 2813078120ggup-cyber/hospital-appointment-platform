package com.atguigu.hospital.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.hospital.mapper.OrderInfoMapper;
import com.atguigu.hospital.mapper.ScheduleMapper;
import com.atguigu.hospital.model.OrderInfo;
import com.atguigu.hospital.model.Patient;
import com.atguigu.hospital.model.Schedule;
import com.atguigu.hospital.service.HospitalService;
import com.atguigu.hospital.util.ResultCodeEnum;
import com.atguigu.hospital.util.YyghException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class HospitalServiceImpl implements HospitalService {

    @Autowired
    private ScheduleMapper hospitalMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> submitOrder(Map<String, Object> paramMap) {
        String hoscode = (String) paramMap.get("hoscode");
        String depcode = (String) paramMap.get("depcode");
        String hosScheduleId = (String) paramMap.get("hosScheduleId");
        String reserveDate = (String) paramMap.get("reserveDate");
        String reserveTime = (String) paramMap.get("reserveTime");
        String amount = (String) paramMap.get("amount");
        String platformOrderNo = (String) paramMap.get("platformOrderNo");
        if (platformOrderNo == null || platformOrderNo.isBlank() || platformOrderNo.length() > 30) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }

        Schedule schedule = hospitalMapper.selectByIdForUpdate(Long.valueOf(hosScheduleId));
        if (null == schedule) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }

        if (!schedule.getHoscode().equals(hoscode)
                || !schedule.getDepcode().equals(depcode)
                || !schedule.getWorkDate().equals(reserveDate)
                || !schedule.getWorkTime().toString().equals(reserveTime)
                || !Integer.valueOf(1).equals(schedule.getStatus())
                || new BigDecimal(schedule.getAmount()).compareTo(new BigDecimal(amount)) != 0) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }

        // 功能完善：排班行锁内复查平台订单号。响应丢失后的重试直接返回原医院订单，不重复扣号。
        LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(OrderInfo::getPlatformOrderNo, platformOrderNo);
        OrderInfo existingOrder = orderInfoMapper.selectOne(orderQuery);
        if (existingOrder != null) {
            if (!schedule.getId().equals(existingOrder.getScheduleId())) {
                throw new YyghException(ResultCodeEnum.DATA_ERROR);
            }
            return buildOrderResult(existingOrder, schedule);
        }

        //就诊人信息
        // 先把 paramMap 转成 JSON 字符串，再把 JSON 字符串转换成 Patient 对象.
        Patient patient = JSONObject.parseObject(JSONObject.toJSONString(paramMap), Patient.class);
        //处理就诊人业务
        Long patientId = this.savePatient(patient);  // 医院端保存就诊人id，与微服务端就诊人id不一样

        Map<String, Object> resultMap = new HashMap<>();
        int availableNumber = schedule.getAvailableNumber().intValue() - 1;
        if (availableNumber >= 0) {
            schedule.setAvailableNumber(availableNumber);
            hospitalMapper.updateById(schedule);

            //记录预约记录
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setPatientId(patientId);
            orderInfo.setScheduleId(schedule.getId());
            orderInfo.setPlatformOrderNo(platformOrderNo);
            int number = schedule.getReservedNumber().intValue() - schedule.getAvailableNumber().intValue();
            orderInfo.setNumber(number); // 挂号序号
            orderInfo.setAmount(new BigDecimal(amount));
            String fetchTime = reserveDate + ("0".equals(reserveTime) ? " 09:30前" : " 14:00前");
            orderInfo.setFetchTime(fetchTime); // 取号日期+时间
            orderInfo.setFetchAddress("一楼9号窗口");
            //默认 未支付
            orderInfo.setOrderStatus(0); // 0下单未支付  1已支付  2取号  -1取消（OrderStatusEnum）
            orderInfoMapper.insert(orderInfo);

            resultMap.putAll(buildOrderResult(orderInfo, schedule));
        } else {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }
        return resultMap;
    }

    private Map<String, Object> buildOrderResult(OrderInfo orderInfo, Schedule schedule) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("resultCode", "0000");
        resultMap.put("resultMsg", "预约成功");
        resultMap.put("hosRecordId", orderInfo.getId());
        resultMap.put("number", orderInfo.getNumber());
        resultMap.put("fetchTime", orderInfo.getFetchTime());
        resultMap.put("fetchAddress", orderInfo.getFetchAddress());
        resultMap.put("reservedNumber", schedule.getReservedNumber());
        resultMap.put("availableNumber", schedule.getAvailableNumber());
        return resultMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayStatus(Map<String, Object> paramMap) {
        String hoscode = (String) paramMap.get("hoscode");
        String hosRecordId = (String) paramMap.get("hosRecordId");

        OrderInfo orderInfo = orderInfoMapper.selectById(hosRecordId);
        if (null == orderInfo) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }
        if (!Integer.valueOf(1).equals(orderInfo.getOrderStatus())) {
            // 功能完善：支付状态同步保持幂等，重复回调不会重复修改业务数据。
            orderInfo.setOrderStatus(1);
            orderInfo.setPayTime(new Date());
            orderInfoMapper.updateById(orderInfo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCancelStatus(Map<String, Object> paramMap) {
        String hoscode = (String) paramMap.get("hoscode");
        String hosRecordId = (String) paramMap.get("hosRecordId");

        OrderInfo orderInfo = orderInfoMapper.selectById(hosRecordId);
        if (null == orderInfo) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }
        if (Integer.valueOf(-1).equals(orderInfo.getOrderStatus())) {
            return;
        }
        Schedule schedule = hospitalMapper.selectByIdForUpdate(orderInfo.getScheduleId());
        if (schedule == null) {
            throw new YyghException(ResultCodeEnum.DATA_ERROR);
        }
        if (schedule.getAvailableNumber() < schedule.getReservedNumber()) {
            // 功能完善：医院侧取消成功后同步恢复排班余量，重复取消不会重复加号。
            schedule.setAvailableNumber(schedule.getAvailableNumber() + 1);
            hospitalMapper.updateById(schedule);
        }
        //已取消
        orderInfo.setOrderStatus(-1);
        orderInfo.setQuitTime(new Date());
        orderInfoMapper.updateById(orderInfo);
    }

    private Schedule getSchedule(String frontSchId) {
        return hospitalMapper.selectById(frontSchId);
    }

    /**
     * 医院处理就诊人信息
     * @param patient
     */
    private Long savePatient(Patient patient) {
        // 业务：略
        return 1L;
    }


}
