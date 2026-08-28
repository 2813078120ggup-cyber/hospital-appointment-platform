-- 平台订单取消运营闭环：记录处理进度、来源、原因、操作人和失败信息。
-- 所有变更均先检查 information_schema，可重复执行。

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'yygh_order' AND TABLE_NAME = 'order_info'
             AND COLUMN_NAME = 'cancel_status'),
    'SELECT 1',
    'ALTER TABLE `yygh_order`.`order_info` ADD COLUMN `cancel_status` TINYINT NOT NULL DEFAULT 0 COMMENT ''取消处理状态（0：未申请 1：处理中 2：成功 3：失败）'' AFTER `order_status`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'yygh_order' AND TABLE_NAME = 'order_info'
             AND COLUMN_NAME = 'cancel_source'),
    'SELECT 1',
    'ALTER TABLE `yygh_order`.`order_info` ADD COLUMN `cancel_source` TINYINT NOT NULL DEFAULT 0 COMMENT ''取消来源（1：用户 2：平台管理员）'' AFTER `cancel_status`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'yygh_order' AND TABLE_NAME = 'order_info'
             AND COLUMN_NAME = 'cancel_reason'),
    'SELECT 1',
    'ALTER TABLE `yygh_order`.`order_info` ADD COLUMN `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT ''取消原因'' AFTER `cancel_source`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'yygh_order' AND TABLE_NAME = 'order_info'
             AND COLUMN_NAME = 'cancel_operator'),
    'SELECT 1',
    'ALTER TABLE `yygh_order`.`order_info` ADD COLUMN `cancel_operator` VARCHAR(100) DEFAULT NULL COMMENT ''取消操作人'' AFTER `cancel_reason`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'yygh_order' AND TABLE_NAME = 'order_info'
             AND COLUMN_NAME = 'cancel_time'),
    'SELECT 1',
    'ALTER TABLE `yygh_order`.`order_info` ADD COLUMN `cancel_time` DATETIME DEFAULT NULL COMMENT ''取消完成时间'' AFTER `cancel_operator`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'yygh_order' AND TABLE_NAME = 'order_info'
             AND COLUMN_NAME = 'cancel_error'),
    'SELECT 1',
    'ALTER TABLE `yygh_order`.`order_info` ADD COLUMN `cancel_error` VARCHAR(500) DEFAULT NULL COMMENT ''取消处理错误'' AFTER `cancel_time`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = 'yygh_order' AND TABLE_NAME = 'order_info'
             AND INDEX_NAME = 'idx_cancel_status'),
    'SELECT 1',
    'ALTER TABLE `yygh_order`.`order_info` ADD KEY `idx_cancel_status` (`cancel_status`, `update_time`)'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

UPDATE `yygh_order`.`order_info`
SET `cancel_status` = 2,
    `cancel_source` = 1,
    `cancel_reason` = COALESCE(`cancel_reason`, '历史取消订单'),
    `cancel_operator` = COALESCE(`cancel_operator`, '历史数据'),
    `cancel_time` = COALESCE(`cancel_time`, `update_time`)
WHERE `order_status` = -1 AND `cancel_status` = 0;
