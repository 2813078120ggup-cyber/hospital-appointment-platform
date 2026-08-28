-- 医院端就诊人档案：修复医院订单固定关联患者编号的问题。
-- 可在已有 yygh_manage 数据库上重复执行；不会覆盖现有患者或订单数据。
USE `yygh_manage`;

CREATE TABLE IF NOT EXISTS `patient` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint DEFAULT NULL COMMENT '平台用户id',
  `name` varchar(20) NOT NULL COMMENT '姓名',
  `certificates_type` varchar(3) DEFAULT NULL COMMENT '证件类型',
  `certificates_no` varchar(30) DEFAULT NULL COMMENT '证件编号',
  `sex` tinyint DEFAULT NULL COMMENT '性别',
  `birthdate` date DEFAULT NULL COMMENT '出生年月',
  `phone` varchar(11) NOT NULL COMMENT '手机',
  `is_marry` tinyint DEFAULT NULL COMMENT '是否结婚',
  `province_code` varchar(20) DEFAULT NULL COMMENT '省code',
  `city_code` varchar(20) DEFAULT NULL COMMENT '市code',
  `district_code` varchar(20) DEFAULT NULL COMMENT '区code',
  `address` varchar(100) DEFAULT NULL COMMENT '详情地址',
  `contacts_name` varchar(20) DEFAULT NULL COMMENT '联系人姓名',
  `contacts_certificates_type` varchar(3) DEFAULT NULL COMMENT '联系人证件类型',
  `contacts_certificates_no` varchar(30) DEFAULT NULL COMMENT '联系人证件号',
  `contacts_phone` varchar(11) DEFAULT NULL COMMENT '联系人手机',
  `card_no` varchar(50) DEFAULT NULL COMMENT '就诊卡号',
  `is_insure` tinyint NOT NULL DEFAULT '0' COMMENT '是否有医保',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态（0：默认 1：已认证）',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(1:已删除，0:未删除)',
  PRIMARY KEY (`id`),
  KEY `idx_patient_user_id` (`user_id`),
  KEY `idx_patient_certificates` (`certificates_type`,`certificates_no`),
  KEY `idx_patient_phone_name` (`phone`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医院侧就诊人档案';

-- 旧版医院订单曾写入固定患者编号。让新档案从所有历史引用之后开始编号，
-- 避免新患者被旧订单误关联；已有患者的自增值不会被降低。
SELECT GREATEST(
    COALESCE((SELECT MAX(`id`) FROM `patient`), 0),
    COALESCE((SELECT MAX(`patient_id`) FROM `order_info`), 0)
  ) + 1 INTO @next_hospital_patient_id;
SET @ddl = CONCAT('ALTER TABLE `patient` AUTO_INCREMENT=', @next_hospital_patient_id);
PREPARE patient_auto_increment_stmt FROM @ddl;
EXECUTE patient_auto_increment_stmt;
DEALLOCATE PREPARE patient_auto_increment_stmt;
