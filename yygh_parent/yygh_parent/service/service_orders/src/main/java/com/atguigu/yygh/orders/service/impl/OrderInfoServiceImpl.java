package com.atguigu.yygh.orders.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.hosp.client.HospitalFeignClient;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import com.atguigu.yygh.orders.service.CompensationTaskService;
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
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.atguigu.yygh.vo.order.SignInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 订单表 服务实现类
 */
@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration IDEMPOTENCY_LOCK_TTL = Duration.ofMinutes(2);
    private static final int CANCEL_STATUS_PROCESSING = 1;
    private static final int CANCEL_STATUS_SUCCESS = 2;
    private static final int CANCEL_STATUS_FAILED = 3;
    private static final int CANCEL_SOURCE_USER = 1;
    private static final int CANCEL_SOURCE_ADMIN = 2;
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

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

    @Autowired
    private CompensationTaskService compensationTaskService;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Value("${yygh.mock-hospital.enabled:false}")
    private boolean mockHospitalEnabled;

    @Override
    public Long createOrder(String scheduleId,
                            Long patientId,
                            Long userId,
                            String idempotencyKey) {
        if (userId == null) {
            throw new YyghException(20001, "请先登录");
        }
        if (!StringUtils.hasText(idempotencyKey)) {
            return createOrderInternal(scheduleId, patientId, userId, newRandomOutTradeNo());
        }

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String outTradeNo = createIdempotentOutTradeNo(userId, normalizedKey);
        OrderInfo existingOrder = findIdempotentOrder(outTradeNo, scheduleId, patientId, userId);
        if (existingOrder != null) {
            return existingOrder.getId();
        }

        String lockKey = "order:submit:" + outTradeNo;
        String lockValue = UUID.randomUUID().toString();
        boolean acquired;
        try {
            acquired = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, IDEMPOTENCY_LOCK_TTL));
        } catch (DataAccessException exception) {
            // 功能完善：幂等基础设施不可用时拒绝继续扣号，避免降级为可能重复下单。
            throw new YyghException(20001, "订单幂等服务暂不可用，请稍后重试");
        }
        if (!acquired) {
            existingOrder = findIdempotentOrder(outTradeNo, scheduleId, patientId, userId);
            if (existingOrder != null) {
                return existingOrder.getId();
            }
            throw new YyghException(20001, "订单正在处理中，请勿重复提交");
        }

        try {
            // 获取锁后再次查询，覆盖多个实例同时进入首次查询的竞态窗口。
            existingOrder = findIdempotentOrder(outTradeNo, scheduleId, patientId, userId);
            return existingOrder == null
                    ? createOrderInternal(scheduleId, patientId, userId, outTradeNo)
                    : existingOrder.getId();
        } finally {
            releaseIdempotencyLock(lockKey, lockValue);
        }
    }

    @Override
    public Long findOrderIdByIdempotencyKey(Long userId, String idempotencyKey) {
        if (userId == null) {
            throw new YyghException(20001, "请先登录");
        }
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String outTradeNo = createIdempotentOutTradeNo(userId, normalizedKey);
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOutTradeNo, outTradeNo)
                .eq(OrderInfo::getUserId, userId);
        OrderInfo existingOrder = baseMapper.selectOne(wrapper);
        return existingOrder == null ? null : existingOrder.getId();
    }

    private Long createOrderInternal(String scheduleId,
                                     Long patientId,
                                     Long userId,
                                     String outTradeNo) {
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
        if (mockHospitalEnabled) {
            // 本地演示不经过医院外部签名接口，仍必须通过医院服务在 MongoDB
            // 中原子扣减号源，不能只写平台订单。
            if (!Boolean.TRUE.equals(hospitalFeignClient.decrementAvailableNumber(scheduleId))) {
                throw new YyghException(20001, "号源已约满、停诊或排班不存在");
            }
            try {
                return createMockOrder(scheduleId, scheduleOrderVo, patient, patientId, outTradeNo);
            } catch (RuntimeException exception) {
                // 平台订单写入失败时补偿已扣减的号源，避免留下不可见库存占用。
                try {
                    hospitalFeignClient.restoreAvailableNumber(scheduleId);
                } catch (RuntimeException compensationException) {
                    log.error("模拟下单写订单失败且号源补偿失败，scheduleId={}", scheduleId,
                            compensationException);
                }
                throw exception;
            }
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
        // 医院模拟端也使用平台交易号执行幂等校验，覆盖医院成功但平台响应丢失后的重试。
        paramMap.put("platformOrderNo", outTradeNo);

        // 医院端需要保留自己的患者档案，传递平台用户号用于医院侧患者去重和追溯。
        paramMap.put("userId", patient.getUserId());
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
        paramMap.put("cardNo", patient.getCardNo());
        paramMap.put("isInsure", patient.getIsInsure());
        paramMap.put("status", patient.getStatus());
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
            throw new YyghException(20001, "下单失败");
        }
    }

    private Long createMockOrder(String scheduleId,
                                 ScheduleOrderVo scheduleOrderVo,
                                 Patient patient,
                                 Long patientId,
                                 String outTradeNo) {
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(scheduleOrderVo, orderInfo);
        // 平台订单必须保存 MongoDB 排班主键；医院自己的 hosScheduleId 不能用于
        // 取消回补，否则 HospitalReceiver 无法定位同一条排班记录。
        orderInfo.setScheduleId(scheduleId);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setUserId(patient.getUserId());
        orderInfo.setPatientId(patientId);
        orderInfo.setPatientName(patient.getName());
        orderInfo.setPatientPhone(patient.getPhone());
        orderInfo.setHosRecordId("MOCK-" + outTradeNo);
        orderInfo.setNumber(0);
        orderInfo.setFetchTime("就诊当天按医院安排取号");
        orderInfo.setFetchAddress("请以医院现场安排为准");
        orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());
        if (baseMapper.insert(orderInfo) != 1 || orderInfo.getId() == null) {
            throw new YyghException(20001, "平台订单保存失败，请稍后重试");
        }
        return orderInfo.getId();
    }

    private OrderInfo findIdempotentOrder(String outTradeNo,
                                          String scheduleId,
                                          Long patientId,
                                          Long userId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOutTradeNo, outTradeNo);
        OrderInfo existingOrder = baseMapper.selectOne(wrapper);
        if (existingOrder == null) {
            return null;
        }
        if (!Objects.equals(existingOrder.getUserId(), userId)
                || !Objects.equals(existingOrder.getPatientId(), patientId)
                || !Objects.equals(existingOrder.getScheduleId(), scheduleId)) {
            throw new YyghException(20001, "幂等键已用于其他订单");
        }
        return existingOrder;
    }

    private String newRandomOutTradeNo() {
        // 交易号保持在支付表 30 字符限制内，并增加随机熵降低并发碰撞概率。
        return System.currentTimeMillis() + String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String createIdempotentOutTradeNo(Long userId, String idempotencyKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((userId + ":" + idempotencyKey)
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder("AI");
            for (int index = 0; index < 14; index++) {
                value.append(String.format("%02x", bytes[index]));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new YyghException(20001, "幂等键不能为空");
        }
        String normalizedKey = idempotencyKey.trim();
        if (normalizedKey.length() > 128 || !normalizedKey.matches("[A-Za-z0-9._\\-:]+")) {
            throw new YyghException(20001, "幂等键格式不正确");
        }
        return normalizedKey;
    }

    private void releaseIdempotencyLock(String lockKey, String lockValue) {
        try {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
        } catch (RuntimeException exception) {
            // 锁有 TTL，释放失败只记录无敏感信息的键；不得覆盖已经完成的订单结果。
            log.warn("释放订单幂等锁失败，lockKey={}", lockKey, exception);
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

    @Override
    public IPage<OrderInfo> selectAdminPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo) {
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        if (orderQueryVo != null) {
            if (orderQueryVo.getUserId() != null) {
                wrapper.eq("user_id", orderQueryVo.getUserId());
            }
            if (StringUtils.hasText(orderQueryVo.getOutTradeNo())) {
                wrapper.like("out_trade_no", orderQueryVo.getOutTradeNo().trim());
            }
            if (orderQueryVo.getPatientId() != null) {
                wrapper.eq("patient_id", orderQueryVo.getPatientId());
            }
            if (StringUtils.hasText(orderQueryVo.getPatientName())) {
                wrapper.like("patient_name", orderQueryVo.getPatientName().trim());
            }
            if (StringUtils.hasText(orderQueryVo.getKeyword())) {
                String keyword = orderQueryVo.getKeyword().trim();
                wrapper.and(condition -> condition.like("hosname", keyword)
                        .or().like("depname", keyword));
            }
            if (StringUtils.hasText(orderQueryVo.getOrderStatus())) {
                try {
                    wrapper.eq("order_status", Integer.valueOf(orderQueryVo.getOrderStatus()));
                } catch (NumberFormatException exception) {
                    throw new YyghException(20001, "订单状态参数不正确");
                }
            }
            if (StringUtils.hasText(orderQueryVo.getCancelStatus())) {
                try {
                    int cancelStatus = Integer.parseInt(orderQueryVo.getCancelStatus());
                    if (cancelStatus < 0 || cancelStatus > CANCEL_STATUS_FAILED) {
                        throw new NumberFormatException("cancel status out of range");
                    }
                    wrapper.eq("cancel_status", cancelStatus);
                } catch (NumberFormatException exception) {
                    throw new YyghException(20001, "取消处理状态参数不正确");
                }
            }
            if (StringUtils.hasText(orderQueryVo.getReserveDate())) {
                wrapper.eq("reserve_date", orderQueryVo.getReserveDate().trim());
            }
            if (StringUtils.hasText(orderQueryVo.getCreateTimeBegin())) {
                wrapper.ge("create_time", orderQueryVo.getCreateTimeBegin().trim());
            }
            if (StringUtils.hasText(orderQueryVo.getCreateTimeEnd())) {
                wrapper.le("create_time", orderQueryVo.getCreateTimeEnd().trim());
            }
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");
        IPage<OrderInfo> page = baseMapper.selectPage(pageParam, wrapper);
        page.getRecords().forEach(this::packOrderInfo);
        return page;
    }

    @Override
    public OrderInfo getAdminOrderInfo(Long orderId) {
        if (orderId == null) {
            throw new YyghException(20001, "订单参数不正确");
        }
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        if (orderInfo == null) {
            throw new YyghException(20001, "订单不存在");
        }
        return packOrderInfo(orderInfo);
    }

    @Override
    public Map<String, Object> getAdminSummary() {
        DateTime today = new DateTime().withTimeAtStartOfDay();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", count());
        summary.put("todayOrders", count(new QueryWrapper<OrderInfo>()
                .ge("create_time", today.toDate())
                .lt("create_time", today.plusDays(1).toDate())));
        summary.put("unpaidOrders", count(new QueryWrapper<OrderInfo>()
                .eq("order_status", OrderStatusEnum.UNPAID.getStatus())));
        summary.put("paidOrders", count(new QueryWrapper<OrderInfo>()
                .eq("order_status", OrderStatusEnum.PAID.getStatus())));
        summary.put("completedOrders", count(new QueryWrapper<OrderInfo>()
                .eq("order_status", OrderStatusEnum.GET_NUMBER.getStatus())));
        summary.put("cancelledOrders", count(new QueryWrapper<OrderInfo>()
                .eq("order_status", OrderStatusEnum.CANCLE.getStatus())));
        summary.put("cancelProcessingOrders", count(new QueryWrapper<OrderInfo>()
                .eq("cancel_status", CANCEL_STATUS_PROCESSING)));
        summary.put("cancelFailedOrders", count(new QueryWrapper<OrderInfo>()
                .eq("cancel_status", CANCEL_STATUS_FAILED)));
        return summary;
    }

    private OrderInfo packOrderInfo(OrderInfo orderInfo) {
        if (orderInfo == null) {
            return null;
        }
        orderInfo.getParam().put("orderStatusString", OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus()));
        String disabledReason = getCancelDisabledReason(orderInfo);
        orderInfo.getParam().put("canCancel", disabledReason == null);
        orderInfo.getParam().put("cancelDisabledReason", disabledReason);
        return orderInfo;
    }

    private String getCancelDisabledReason(OrderInfo orderInfo) {
        if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.CANCLE.getStatus())) {
            return "订单已取消";
        }
        if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.GET_NUMBER.getStatus())) {
            return "订单已取号";
        }
        if (!Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.UNPAID.getStatus())
                && !Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PAID.getStatus())) {
            return "当前订单状态不允许取消";
        }
        if (Objects.equals(orderInfo.getCancelStatus(), CANCEL_STATUS_PROCESSING)) {
            return "取消申请正在处理中";
        }
        if (orderInfo.getQuitTime() == null) {
            return "未配置最晚取消时间";
        }
        if (new DateTime(orderInfo.getQuitTime()).isBeforeNow()) {
            return "已超过最晚取消时间";
        }
        return null;
    }


    @Override
    public boolean cancelOrder(Long orderId, Long userId) {
        OrderInfo orderInfo = requireOwnedOrder(orderId, userId);
        return cancelOrderInternal(orderInfo,
                CANCEL_SOURCE_USER,
                "用户主动取消预约",
                "用户#" + userId);
    }

    @Override
    public boolean cancelOrderByAdmin(Long orderId, String reason, String operator) {
        String normalizedReason = normalizeAdminCancelReason(reason);
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        if (orderInfo == null) {
            throw new YyghException(20001, "订单不存在");
        }
        String normalizedOperator = StringUtils.hasText(operator) ? operator.trim() : "平台管理员";
        if (normalizedOperator.length() > 100) {
            normalizedOperator = normalizedOperator.substring(0, 100);
        }
        return cancelOrderInternal(orderInfo,
                CANCEL_SOURCE_ADMIN,
                normalizedReason,
                normalizedOperator);
    }

    private boolean cancelOrderInternal(OrderInfo orderInfo,
                                        int cancelSource,
                                        String reason,
                                        String operator) {
        if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.CANCLE.getStatus())) {
            // 订单已经取消时也确保库存同步 outbox 存在；重复请求不会重复创建任务。
            sendCancelMessage(orderInfo);
            return true;
        }
        if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.GET_NUMBER.getStatus())) {
            throw new YyghException(20001, "已取号订单不能取消");
        }
        if (!Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.UNPAID.getStatus())
                && !Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PAID.getStatus())) {
            throw new YyghException(20001, "当前订单状态不允许取消");
        }
        if (orderInfo.getQuitTime() == null) {
            throw new YyghException(20001, "订单未配置最晚取消时间");
        }
        DateTime dateTime = new DateTime(orderInfo.getQuitTime());  // 获取取消时间
        if (dateTime.isBeforeNow()) {
            throw new YyghException(20001, "已超过预约取消时间");
        }

        OrderInfo cancelRequest = new OrderInfo();
        cancelRequest.setCancelStatus(CANCEL_STATUS_PROCESSING);
        cancelRequest.setCancelSource(cancelSource);
        cancelRequest.setCancelReason(reason);
        cancelRequest.setCancelOperator(operator);
        UpdateWrapper<OrderInfo> claimWrapper = new UpdateWrapper<OrderInfo>()
                .eq("id", orderInfo.getId())
                .ne("order_status", OrderStatusEnum.CANCLE.getStatus())
                .and(wrapper -> wrapper.isNull("cancel_status")
                        .or().ne("cancel_status", CANCEL_STATUS_PROCESSING))
                .set("cancel_error", null);
        if (baseMapper.update(cancelRequest, claimWrapper) != 1) {
            OrderInfo current = baseMapper.selectById(orderInfo.getId());
            if (current != null
                    && Objects.equals(current.getOrderStatus(), OrderStatusEnum.CANCLE.getStatus())) {
                return true;
            }
            throw new YyghException(20001, "取消申请正在处理中，请勿重复操作");
        }

        try {
            return completeCancellation(orderInfo, cancelSource, reason, operator);
        } catch (RuntimeException exception) {
            markCancelFailed(orderInfo.getId(), safeCancelError(exception));
            enqueueFailedCancellation(orderInfo.getId());
            if (exception instanceof YyghException) {
                throw exception;
            }
            log.error("取消订单失败，orderId={}", orderInfo.getId(), exception);
            throw new YyghException(20001, "取消处理失败，请稍后重试");
        }
    }

    /**
     * outbox worker 使用的取消补偿入口。这里不再重新抢占取消状态，避免
     * worker 与用户重复提交互相覆盖；每个副作用本身保持幂等后再完成平台状态。
     */
    @Override
    public boolean retryCancellationFromCompensation(Long orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        if (orderInfo == null) {
            throw new YyghException(20001, "订单不存在");
        }
        if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.CANCLE.getStatus())) {
            sendCancelMessage(orderInfo);
            return true;
        }
        int source = orderInfo.getCancelSource() == null || orderInfo.getCancelSource() == 0
                ? CANCEL_SOURCE_USER : orderInfo.getCancelSource();
        String reason = StringUtils.hasText(orderInfo.getCancelReason())
                ? orderInfo.getCancelReason() : "系统自动补偿取消预约";
        String operator = StringUtils.hasText(orderInfo.getCancelOperator())
                ? orderInfo.getCancelOperator() : "系统补偿任务";
        try {
            return completeCancellation(orderInfo, source, reason, operator);
        } catch (RuntimeException exception) {
            markCancelFailed(orderId, safeCancelError(exception));
            throw exception;
        }
    }

    private boolean completeCancellation(OrderInfo orderInfo,
                                         int cancelSource,
                                         String reason,
                                         String operator) {
        cancelAtHospital(orderInfo);
        if (Objects.equals(orderInfo.getOrderStatus(), OrderStatusEnum.PAID.getStatus())
                && !weixinService.refund(orderInfo.getId())) {
            throw new YyghException(20001, "医院已取消预约，但退款未完成，请稍后重试");
        }

        orderInfo.setOrderStatus(OrderStatusEnum.CANCLE.getStatus());
        orderInfo.setCancelStatus(CANCEL_STATUS_SUCCESS);
        orderInfo.setCancelSource(cancelSource);
        orderInfo.setCancelReason(reason);
        orderInfo.setCancelOperator(operator);
        orderInfo.setCancelTime(new Date());
        orderInfo.setCancelError(null);
        if (baseMapper.updateById(orderInfo) != 1) {
            throw new YyghException(20001, "平台订单状态更新失败，请稍后重试");
        }
        // 库存回补/同步进入 outbox；当前实例可立即尝试，失败由 worker 退避重试。
        sendCancelMessage(orderInfo);
        return true;
    }

    private void cancelAtHospital(OrderInfo orderInfo) {
        if (mockHospitalEnabled) {
            // 模拟医院的库存回补在平台订单成功落库后由 STOCK_RESTORE outbox 完成，
            // 避免回补成功后平台状态更新失败导致重试再次加号。
            return;
        }
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
            String message = result == null ? "医院取消接口无响应" : result.getString("message");
            throw new YyghException(ResultCodeEnum.FAIL.getCode(), message);
        }
    }

    private void sendCancelMessage(OrderInfo orderInfo) {
        try {
            compensationTaskService.enqueueAndDispatch(
                    mockHospitalEnabled
                            ? CompensationTaskService.TYPE_STOCK_RESTORE
                            : CompensationTaskService.TYPE_STOCK_SYNC,
                    orderInfo.getId());
        } catch (RuntimeException exception) {
            // outbox 写入失败时不能只记录日志；由上层将整条取消流程放入补偿队列。
            log.error("订单已取消，但库存同步 outbox 写入失败，orderId={}", orderInfo.getId(), exception);
            throw new YyghException(20001, "库存同步任务写入失败，请稍后重试");
        }
    }

    private void enqueueFailedCancellation(Long orderId) {
        try {
            compensationTaskService.enqueue(CompensationTaskService.TYPE_CANCEL_FLOW, orderId);
        } catch (RuntimeException compensationException) {
            // 原始取消错误已经返回给调用方；这里保留错误日志，定时对账还会再次发现失败订单。
            log.error("取消失败且补偿任务写入失败，orderId={}", orderId, compensationException);
        }
    }

    private void markCancelFailed(Long orderId, String errorMessage) {
        OrderInfo failed = new OrderInfo();
        failed.setId(orderId);
        failed.setCancelStatus(CANCEL_STATUS_FAILED);
        failed.setCancelError(errorMessage);
        baseMapper.updateById(failed);
    }

    private String normalizeAdminCancelReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new YyghException(20001, "请填写取消原因");
        }
        String normalized = reason.trim();
        if (normalized.length() < 5) {
            throw new YyghException(20001, "取消原因至少填写 5 个字符");
        }
        if (normalized.length() > 255) {
            throw new YyghException(20001, "取消原因不能超过 255 个字符");
        }
        return normalized;
    }

    private String safeCancelError(RuntimeException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            message = "取消处理失败";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    @Override
    public boolean updatePayStatusToHospital(Long orderId) {
        if (mockHospitalEnabled) {
            return true;
        }
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        if (orderInfo == null) {
            return false;
        }
        try {
            SignInfoVo signInfoVo = requireSignInfo(orderInfo.getHoscode());
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("hoscode", orderInfo.getHoscode());
            reqMap.put("hosRecordId", orderInfo.getHosRecordId());
            reqMap.put("timestamp", HttpRequestHelper.getTimestamp());
            reqMap.put("sign", HttpRequestHelper.getSignSingle(signInfoVo.getSignKey()));
            JSONObject result = HttpRequestHelper.sendRequest(reqMap,
                    normalizeBaseUrl(signInfoVo.getApiUrl()) + "/order/updatePayStatus");
            boolean synced = result != null && result.getInteger("code") == 200;
            if (!synced) {
                enqueuePaymentStatusCompensation(orderId);
            }
            return synced;
        } catch (RuntimeException exception) {
            enqueuePaymentStatusCompensation(orderId);
            log.warn("医院支付状态同步失败，已进入补偿队列，orderId={}", orderId, exception);
            return false;
        }
    }

    private void enqueuePaymentStatusCompensation(Long orderId) {
        try {
            compensationTaskService.enqueue(
                    CompensationTaskService.TYPE_HOSPITAL_PAYMENT_SYNC, orderId);
        } catch (RuntimeException compensationException) {
            log.error("医院支付状态同步失败且补偿任务写入失败，orderId={}",
                    orderId, compensationException);
        }
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
