-- 功能完善：AI 正式订单关联与医院端幂等字段。
-- 本脚本通过 information_schema 判断后再变更，可在基础建表后重复执行。

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'yygh_manage' AND TABLE_NAME = 'order_info'
             AND COLUMN_NAME = 'platform_order_no'),
    'SELECT 1',
    'ALTER TABLE `yygh_manage`.`order_info` ADD COLUMN `platform_order_no` VARCHAR(30) DEFAULT NULL COMMENT ''平台订单号，用于跨系统幂等'' AFTER `patient_id`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = 'yygh_manage' AND TABLE_NAME = 'order_info'
             AND INDEX_NAME = 'uk_platform_order_no'),
    'SELECT 1',
    'ALTER TABLE `yygh_manage`.`order_info` ADD UNIQUE KEY `uk_platform_order_no` (`platform_order_no`)'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'guiguxiaozhi' AND TABLE_NAME = 'appointment'
             AND COLUMN_NAME = 'patient_id'),
    'SELECT 1',
    'ALTER TABLE `guiguxiaozhi`.`appointment` ADD COLUMN `patient_id` BIGINT DEFAULT NULL COMMENT ''平台就诊人ID'' AFTER `doctor_name`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'guiguxiaozhi' AND TABLE_NAME = 'appointment'
             AND COLUMN_NAME = 'schedule_id'),
    'SELECT 1',
    'ALTER TABLE `guiguxiaozhi`.`appointment` ADD COLUMN `schedule_id` VARCHAR(50) DEFAULT NULL COMMENT ''平台排班ID'' AFTER `patient_id`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'guiguxiaozhi' AND TABLE_NAME = 'appointment'
             AND COLUMN_NAME = 'platform_order_id'),
    'SELECT 1',
    'ALTER TABLE `guiguxiaozhi`.`appointment` ADD COLUMN `platform_order_id` BIGINT DEFAULT NULL COMMENT ''平台正式订单ID'' AFTER `schedule_id`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = 'guiguxiaozhi' AND TABLE_NAME = 'appointment'
             AND COLUMN_NAME = 'status'),
    'SELECT 1',
    'ALTER TABLE `guiguxiaozhi`.`appointment` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING或SUBMITTED'' AFTER `platform_order_id`'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

-- 功能完善：存量 AI 预约表也必须补齐唯一约束，避免并发请求绕过应用层查询后重复落库。
SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = 'guiguxiaozhi' AND TABLE_NAME = 'appointment'
             AND INDEX_NAME = 'uk_appointment_identity_slot'),
    'SELECT 1',
    'ALTER TABLE `guiguxiaozhi`.`appointment` ADD UNIQUE KEY `uk_appointment_identity_slot` (`id_card`, `department`, `date`, `time`)'
);
PREPARE migration_stmt FROM @ddl;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
