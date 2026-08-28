package com.atguigu.yygh.orders.service;

import com.atguigu.yygh.model.order.OrderCompensationTask;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/** 订单跨系统副作用的 outbox、重试和对账服务。 */
public interface CompensationTaskService {

    String TYPE_CANCEL_FLOW = "CANCEL_FLOW";
    String TYPE_STOCK_RESTORE = "STOCK_RESTORE";
    String TYPE_STOCK_SYNC = "STOCK_SYNC";
    String TYPE_HOSPITAL_PAYMENT_SYNC = "HOSPITAL_PAYMENT_SYNC";

    int STATUS_PENDING = 0;
    int STATUS_PROCESSING = 1;
    int STATUS_SUCCESS = 2;
    int STATUS_DEAD = 3;

    Long enqueue(String taskType, Long orderId);

    Long enqueueAndDispatch(String taskType, Long orderId);

    void dispatchNow(Long taskId);

    boolean retryDeadLetter(Long taskId);

    IPage<OrderCompensationTask> selectPage(Page<OrderCompensationTask> page,
                                             Integer status,
                                             String taskType);

    /** 领取并处理到期任务。由定时器调用，也便于测试和运维手动触发。 */
    void processDueTasks();

    /** 扫描长期处于取消处理中/失败的订单，确保任务不会因进程重启而丢失。 */
    void reconcile();
}
