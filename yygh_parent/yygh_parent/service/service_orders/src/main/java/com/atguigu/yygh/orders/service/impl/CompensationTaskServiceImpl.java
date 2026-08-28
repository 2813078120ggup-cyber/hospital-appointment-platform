package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.hosp.client.HospitalFeignClient;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.enums.PaymentStatusEnum;
import com.atguigu.yygh.model.order.OrderCompensationTask;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.orders.mapper.OrderCompensationTaskMapper;
import com.atguigu.yygh.orders.mapper.OrderInfoMapper;
import com.atguigu.yygh.orders.mapper.PaymentInfoMapper;
import com.atguigu.yygh.orders.service.CompensationTaskService;
import com.atguigu.yygh.orders.service.OrderInfoService;
import com.atguigu.yygh.rabbit.RabbitService;
import com.atguigu.yygh.rabbit.constant.MqConst;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 订单 outbox worker。
 *
 * <p>发送外部医院、库存同步消息和退款均可能跨越多个系统，不能依赖一次
 * HTTP/Rabbit 调用完成。任务先落订单库，再由带租约的 worker 领取；连续失败
 * 达到上限后保留为数据库死信，管理员可以查看并重新投递。</p>
 */
@Service
@Slf4j
public class CompensationTaskServiceImpl implements CompensationTaskService {

    private static final int CLAIM_BATCH_SIZE = 20;
    private static final int LEASE_SECONDS = 120;
    private static final long RECONCILE_CUTOFF_MILLIS = 60_000L;
    private static final int RECONCILE_BATCH_SIZE = 100;

    @Autowired
    private OrderCompensationTaskMapper taskMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Autowired
    @Lazy
    private OrderInfoService orderInfoService;

    @Value("${yygh.compensation.max-attempts:8}")
    private int maxAttempts;

    @Value("${yygh.mock-hospital.enabled:false}")
    private boolean mockHospitalEnabled;

    @Value("${yygh.compensation.reconcile-lookback-hours:24}")
    private int reconcileLookbackHours;

    private final String workerId = "order-compensation-" + UUID.randomUUID();

    @Override
    public Long enqueue(String taskType, Long orderId) {
        if (!StringUtils.hasText(taskType) || orderId == null) {
            throw new IllegalArgumentException("补偿任务参数不完整");
        }
        String normalizedType = taskType.trim();
        OrderCompensationTask task = new OrderCompensationTask();
        task.setTaskKey(normalizedType + ":" + orderId);
        task.setTaskType(normalizedType);
        task.setOrderId(orderId);
        task.setNextRetryTime(new Date());
        taskMapper.insertPending(task);
        OrderCompensationTask persisted = taskMapper.selectByTaskKey(task.getTaskKey());
        if (persisted == null || persisted.getId() == null) {
            throw new IllegalStateException("补偿任务写入失败");
        }
        return persisted.getId();
    }

    @Override
    public Long enqueueAndDispatch(String taskType, Long orderId) {
        Long taskId = enqueue(taskType, orderId);
        dispatchNow(taskId);
        return taskId;
    }

    @Override
    public void dispatchNow(Long taskId) {
        if (taskId == null) {
            return;
        }
        OrderCompensationTask task = taskMapper.selectById(taskId);
        if (task == null || Integer.valueOf(STATUS_SUCCESS).equals(task.getStatus())) {
            return;
        }
        Date lockedUntil = Date.from(Instant.now().plusSeconds(LEASE_SECONDS));
        if (taskMapper.claim(taskId, workerId, lockedUntil) != 1) {
            return;
        }
        try {
            execute(task);
            if (taskMapper.markSuccess(taskId, workerId) != 1) {
                log.warn("补偿任务执行成功但状态落库失败，taskId={}", taskId);
            }
        } catch (RuntimeException exception) {
            recordFailure(task, exception);
        }
    }

    @Override
    public boolean retryDeadLetter(Long taskId) {
        boolean reset = taskId != null && taskMapper.retryDeadLetter(taskId) == 1;
        if (reset) {
            dispatchNow(taskId);
        }
        return reset;
    }

    @Override
    public IPage<OrderCompensationTask> selectPage(Page<OrderCompensationTask> page,
                                                    Integer status,
                                                    String taskType) {
        QueryWrapper<OrderCompensationTask> wrapper = new QueryWrapper<>();
        if (status != null && status >= STATUS_PENDING && status <= STATUS_DEAD) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(taskType)) {
            wrapper.eq("task_type", taskType.trim());
        }
        wrapper.orderByAsc("status")
                .orderByAsc("next_retry_time")
                .orderByDesc("id");
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    @Scheduled(fixedDelayString = "${yygh.compensation.worker-delay-ms:10000}",
            initialDelayString = "${yygh.compensation.worker-initial-delay-ms:5000}")
    public void processDueTasks() {
        List<OrderCompensationTask> tasks = taskMapper.selectDueTasks(CLAIM_BATCH_SIZE);
        for (OrderCompensationTask task : tasks) {
            dispatchNow(task.getId());
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${yygh.compensation.reconcile-delay-ms:60000}",
            initialDelayString = "${yygh.compensation.reconcile-initial-delay-ms:15000}")
    public void reconcile() {
        Date cutoff = new Date(System.currentTimeMillis() - RECONCILE_CUTOFF_MILLIS);
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        wrapper.in("cancel_status", 1, 3)
                .ne("order_status", -1)
                .lt("update_time", cutoff)
                .orderByAsc("update_time")
                .last("LIMIT " + RECONCILE_BATCH_SIZE);
        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);
        for (OrderInfo order : orders) {
            try {
                enqueue(TYPE_CANCEL_FLOW, order.getId());
            } catch (RuntimeException exception) {
                log.error("取消补偿对账任务写入失败，orderId={}", order.getId(), exception);
            }
        }

        // 订单状态更新与 outbox 写入之间若发生进程中断，扫描近期已取消订单补齐库存任务。
        // 只在任务不存在时写入，死信任务留给管理员判断，避免对历史订单重复回补号源。
        QueryWrapper<OrderInfo> canceledWrapper = new QueryWrapper<>();
        canceledWrapper.eq("order_status", OrderStatusEnum.CANCLE.getStatus())
                .isNotNull("cancel_time")
                .ge("cancel_time", lookbackCutoff())
                .orderByAsc("cancel_time")
                .last("LIMIT " + RECONCILE_BATCH_SIZE);
        List<OrderInfo> canceledOrders = orderInfoMapper.selectList(canceledWrapper);
        String stockTaskType = mockHospitalEnabled ? TYPE_STOCK_RESTORE : TYPE_STOCK_SYNC;
        for (OrderInfo order : canceledOrders) {
            String taskKey = stockTaskType + ":" + order.getId();
            if (taskMapper.selectByTaskKey(taskKey) == null) {
                try {
                    enqueue(stockTaskType, order.getId());
                } catch (RuntimeException exception) {
                    log.error("取消订单库存对账任务写入失败，orderId={}", order.getId(), exception);
                }
            }
        }

        // 支付回调先落库、后同步医院；进程若在两步之间退出，按支付记录补发医院状态。
        QueryWrapper<PaymentInfo> paymentWrapper = new QueryWrapper<>();
        paymentWrapper.eq("payment_status", PaymentStatusEnum.PAID.getStatus())
                .isNotNull("callback_time")
                .ge("callback_time", lookbackCutoff())
                .orderByAsc("callback_time")
                .last("LIMIT " + RECONCILE_BATCH_SIZE);
        List<PaymentInfo> paidPayments = paymentInfoMapper.selectList(paymentWrapper);
        for (PaymentInfo payment : paidPayments) {
            if (payment.getOrderId() == null) {
                continue;
            }
            OrderInfo order = orderInfoMapper.selectById(payment.getOrderId());
            if (order == null || !OrderStatusEnum.PAID.getStatus().equals(order.getOrderStatus())) {
                continue;
            }
            String taskKey = TYPE_HOSPITAL_PAYMENT_SYNC + ":" + payment.getOrderId();
            if (taskMapper.selectByTaskKey(taskKey) == null) {
                try {
                    enqueue(TYPE_HOSPITAL_PAYMENT_SYNC, payment.getOrderId());
                } catch (RuntimeException exception) {
                    log.error("支付状态对账任务写入失败，orderId={}", payment.getOrderId(), exception);
                }
            }
        }
    }

    private Date lookbackCutoff() {
        int hours = Math.max(1, reconcileLookbackHours);
        return new Date(System.currentTimeMillis() - hours * 3_600_000L);
    }

    private void execute(OrderCompensationTask task) {
        OrderInfo order = orderInfoMapper.selectById(task.getOrderId());
        if (order == null) {
            throw new IllegalStateException("订单不存在");
        }
        String taskType = task.getTaskType();
        if (TYPE_CANCEL_FLOW.equals(taskType)) {
            if (!orderInfoService.retryCancellationFromCompensation(order.getId())) {
                throw new IllegalStateException("取消补偿未完成");
            }
            return;
        }
        if (TYPE_STOCK_RESTORE.equals(taskType)) {
            if (!Boolean.TRUE.equals(hospitalFeignClient.restoreAvailableNumber(order.getScheduleId()))) {
                throw new IllegalStateException("号源回补未确认成功");
            }
            return;
        }
        if (TYPE_STOCK_SYNC.equals(taskType)) {
            publishStockSync(order);
            return;
        }
        if (TYPE_HOSPITAL_PAYMENT_SYNC.equals(taskType)) {
            if (!orderInfoService.updatePayStatusToHospital(order.getId())) {
                throw new IllegalStateException("医院支付状态同步未确认成功");
            }
            return;
        }
        throw new IllegalStateException("未知补偿任务类型：" + taskType);
    }

    private void publishStockSync(OrderInfo order) {
        OrderMqVo orderMqVo = new OrderMqVo();
        orderMqVo.setScheduleId(order.getScheduleId());
        MsmVo msmVo = new MsmVo();
        msmVo.setPhone(order.getPatientPhone());
        orderMqVo.setMsmVo(msmVo);
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);
    }

    private void recordFailure(OrderCompensationTask task, RuntimeException exception) {
        int attemptCount = task.getAttemptCount() == null ? 0 : task.getAttemptCount();
        int nextAttempt = attemptCount + 1;
        boolean dead = nextAttempt >= Math.max(1, maxAttempts);
        Date nextRetry = dead ? new Date() : Date.from(Instant.now().plusSeconds(backoffSeconds(nextAttempt)));
        String error = exception.getMessage();
        if (!StringUtils.hasText(error)) {
            error = exception.getClass().getSimpleName();
        }
        if (error.length() > 500) {
            error = error.substring(0, 500);
        }
        int status = dead ? STATUS_DEAD : STATUS_PENDING;
        if (taskMapper.markFailure(task.getId(), workerId, status, nextAttempt, nextRetry, error) != 1) {
            log.error("补偿任务失败但状态落库失败，taskId={}", task.getId(), exception);
        } else if (dead) {
            log.error("补偿任务进入死信状态，taskId={}, taskType={}, orderId={}",
                    task.getId(), task.getTaskType(), task.getOrderId(), exception);
        } else {
            log.warn("补偿任务将重试，taskId={}, taskType={}, orderId={}, attempt={}",
                    task.getId(), task.getTaskType(), task.getOrderId(), nextAttempt, exception);
        }
    }

    private long backoffSeconds(int attempt) {
        int exponent = Math.min(attempt - 1, 6);
        return Math.min(300L, 5L << exponent);
    }
}
