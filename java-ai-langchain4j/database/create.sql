CREATE DATABASE `guiguxiaozhi`;
USE `guiguxiaozhi`;
CREATE TABLE `appointment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `id_card` VARCHAR(18) NOT NULL,
    `department` VARCHAR(50) NOT NULL,
    `date` VARCHAR(10) NOT NULL,
    `time` VARCHAR(10) NOT NULL,
    `doctor_name` VARCHAR(50) DEFAULT NULL,
    `patient_id` BIGINT DEFAULT NULL COMMENT '平台就诊人ID',
    `schedule_id` VARCHAR(50) DEFAULT NULL COMMENT '平台排班ID',
    `platform_order_id` BIGINT DEFAULT NULL COMMENT '平台正式订单ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING或SUBMITTED',
    PRIMARY KEY (`id`),
    -- 功能完善：并发请求下由数据库兜底防止同一患者重复预约相同时段。
    UNIQUE KEY `uk_appointment_identity_slot` (`id_card`, `department`, `date`, `time`)
);
