-- 订单跨系统副作用 outbox：取消/退款/医院状态同步失败后可重试，status=3 为数据库死信。
-- 使用 IF NOT EXISTS，可在远程 yygh_order 库重复执行。

CREATE TABLE IF NOT EXISTS `yygh_order`.`order_compensation_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '补偿任务编号',
  `task_key` varchar(100) NOT NULL COMMENT '业务幂等键，例如 STOCK_SYNC:订单号',
  `task_type` varchar(40) NOT NULL COMMENT '任务类型',
  `order_id` bigint NOT NULL COMMENT '平台订单编号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2成功 3死信',
  `attempt_count` int NOT NULL DEFAULT 0 COMMENT '已执行次数',
  `next_retry_time` datetime NOT NULL COMMENT '下一次处理时间',
  `last_error` varchar(500) DEFAULT NULL COMMENT '最近一次失败原因',
  `locked_by` varchar(100) DEFAULT NULL COMMENT '租约持有 worker',
  `locked_until` datetime DEFAULT NULL COMMENT '租约过期时间',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_compensation_task_key` (`task_key`),
  KEY `idx_order_compensation_due` (`status`, `next_retry_time`, `id`),
  KEY `idx_order_compensation_order` (`order_id`, `task_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单补偿 outbox 任务';
