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
    PRIMARY KEY (`id`),
    -- 功能完善：并发请求下由数据库兜底防止同一患者重复预约相同时段。
    UNIQUE KEY `uk_appointment_identity_slot` (`id_card`, `department`, `date`, `time`)
);
