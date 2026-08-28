package com.atguigu.yygh.model.order;

import com.atguigu.yygh.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 订单跨系统副作用的持久化 outbox 任务。
 *
 * <p>任务本身位于订单库，发送医院、退款和库存同步消息失败后仍可由
 * 定时 worker 或管理端继续处理。status=3 表示数据库死信状态。</p>
 */
@Data
@TableName("order_compensation_outbox")
public class OrderCompensationTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("task_key")
    private String taskKey;

    @TableField("task_type")
    private String taskType;

    @TableField("order_id")
    private Long orderId;

    /** 0待处理，1处理中，2成功，3死信。 */
    private Integer status;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("next_retry_time")
    private Date nextRetryTime;

    @TableField("last_error")
    private String lastError;

    @TableField("locked_by")
    private String lockedBy;

    @TableField("locked_until")
    private Date lockedUntil;

    @TableField("completed_time")
    private Date completedTime;
}
