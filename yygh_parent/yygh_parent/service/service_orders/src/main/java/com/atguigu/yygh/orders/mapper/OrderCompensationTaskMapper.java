package com.atguigu.yygh.orders.mapper;

import com.atguigu.yygh.model.order.OrderCompensationTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/** 持久化 outbox 任务 Mapper。 */
public interface OrderCompensationTaskMapper extends BaseMapper<OrderCompensationTask> {

    @Insert("""
            INSERT INTO order_compensation_outbox
              (task_key, task_type, order_id, status, attempt_count, next_retry_time,
               create_time, update_time, is_deleted)
            VALUES
              (#{taskKey}, #{taskType}, #{orderId}, 0, 0, COALESCE(#{nextRetryTime}, NOW()),
               NOW(), NOW(), 0)
            ON DUPLICATE KEY UPDATE
              /* 重复事件只唤醒待处理任务；处理中和死信保留租约/错误，避免并发重复副作用。 */
              status = status,
              next_retry_time = IF(status = 0, NOW(), next_retry_time),
              last_error = IF(status = 0, NULL, last_error),
              locked_by = IF(status = 0, NULL, locked_by),
              locked_until = IF(status = 0, NULL, locked_until),
              update_time = NOW()
            """)
    int insertPending(OrderCompensationTask task);

    @Select("SELECT * FROM order_compensation_outbox WHERE task_key = #{taskKey} AND is_deleted = 0 LIMIT 1")
    OrderCompensationTask selectByTaskKey(@Param("taskKey") String taskKey);

    @Select("""
            SELECT * FROM order_compensation_outbox
            WHERE is_deleted = 0
              AND next_retry_time <= NOW()
              AND (status = 0 OR (status = 1 AND locked_until < NOW()))
            ORDER BY next_retry_time ASC, id ASC
            LIMIT #{limit}
            """)
    List<OrderCompensationTask> selectDueTasks(@Param("limit") int limit);

    @Update("""
            UPDATE order_compensation_outbox
            SET status = 1,
                locked_by = #{workerId},
                locked_until = #{lockedUntil},
                update_time = NOW()
            WHERE id = #{id}
              AND is_deleted = 0
              AND next_retry_time <= NOW()
              AND (status = 0 OR (status = 1 AND locked_until < NOW()))
            """)
    int claim(@Param("id") Long id,
              @Param("workerId") String workerId,
              @Param("lockedUntil") Date lockedUntil);

    @Update("""
            UPDATE order_compensation_outbox
            SET status = 2,
                attempt_count = attempt_count + 1,
                last_error = NULL,
                locked_by = NULL,
                locked_until = NULL,
                completed_time = NOW(),
                update_time = NOW()
            WHERE id = #{id} AND status = 1 AND locked_by = #{workerId}
            """)
    int markSuccess(@Param("id") Long id, @Param("workerId") String workerId);

    @Update("""
            UPDATE order_compensation_outbox
            SET status = #{status},
                attempt_count = #{attemptCount},
                next_retry_time = #{nextRetryTime},
                last_error = #{lastError},
                locked_by = NULL,
                locked_until = NULL,
                update_time = NOW()
            WHERE id = #{id} AND status = 1 AND locked_by = #{workerId}
            """)
    int markFailure(@Param("id") Long id,
                    @Param("workerId") String workerId,
                    @Param("status") int status,
                    @Param("attemptCount") int attemptCount,
                    @Param("nextRetryTime") Date nextRetryTime,
                    @Param("lastError") String lastError);

    @Update("""
            UPDATE order_compensation_outbox
            SET status = 0,
                attempt_count = 0,
                next_retry_time = NOW(),
                last_error = NULL,
                locked_by = NULL,
                locked_until = NULL,
                completed_time = NULL,
                update_time = NOW()
            WHERE id = #{id} AND is_deleted = 0 AND status = 3
            """)
    int retryDeadLetter(@Param("id") Long id);
}
