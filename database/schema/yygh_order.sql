-- 医院预约挂号平台：yygh_order 纯结构初始化脚本
-- 由 tools/generate_reference_docs.py 生成；不包含源文件中的 INSERT 数据。
CREATE DATABASE IF NOT EXISTS `yygh_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yygh_order`;

DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint DEFAULT NULL,
  `out_trade_no` varchar(300) DEFAULT NULL COMMENT '订单交易号',
  `hoscode` varchar(30) DEFAULT NULL COMMENT '医院编号',
  `hosname` varchar(100) DEFAULT NULL COMMENT '医院名称',
  `depcode` varchar(30) DEFAULT NULL COMMENT '科室编号',
  `depname` varchar(20) DEFAULT NULL COMMENT '科室名称',
  `title` varchar(20) DEFAULT NULL COMMENT '医生职称',
  `hos_schedule_id` varchar(50) DEFAULT NULL COMMENT '排班编号（医院自己的排班主键）',
  `reserve_date` date DEFAULT NULL COMMENT '安排日期',
  `reserve_time` tinyint DEFAULT '0' COMMENT '安排时间（0：上午 1：下午）',
  `patient_id` bigint DEFAULT NULL COMMENT '就诊人id',
  `patient_name` varchar(20) DEFAULT NULL COMMENT '就诊人名称',
  `patient_phone` varchar(11) DEFAULT NULL COMMENT '就诊人手机',
  `hos_record_id` varchar(30) DEFAULT NULL COMMENT '预约记录唯一标识（医院预约记录主键）',
  `number` int DEFAULT NULL COMMENT '预约号序',
  `fetch_time` varchar(50) DEFAULT NULL COMMENT '建议取号时间',
  `fetch_address` varchar(255) DEFAULT NULL COMMENT '取号地点',
  `amount` decimal(10,0) DEFAULT NULL COMMENT '医事服务费',
  `quit_time` datetime DEFAULT NULL COMMENT '退号时间',
  `order_status` tinyint DEFAULT NULL COMMENT '订单状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(1:已删除，0:未删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_hoscode` (`hoscode`),
  KEY `idx_hos_schedule_id` (`hos_schedule_id`),
  KEY `idx_hos_record_id` (`hos_record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb3 COMMENT='订单表';

DROP TABLE IF EXISTS `payment_info`;
CREATE TABLE `payment_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '编号',
  `out_trade_no` varchar(30) DEFAULT NULL COMMENT '对外业务编号',
  `order_id` bigint DEFAULT NULL COMMENT '订单id',
  `payment_type` tinyint(1) DEFAULT NULL COMMENT '支付类型（微信 支付宝）',
  `trade_no` varchar(50) DEFAULT NULL COMMENT '交易编号',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT '支付金额',
  `subject` varchar(200) DEFAULT NULL COMMENT '交易内容',
  `payment_status` tinyint DEFAULT NULL COMMENT '支付状态',
  `callback_time` datetime DEFAULT NULL COMMENT '回调时间',
  `callback_content` varchar(1000) DEFAULT NULL COMMENT '回调信息',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(1:已删除，0:未删除)',
  PRIMARY KEY (`id`),
  KEY `idx_out_trade_no` (`out_trade_no`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 COMMENT='支付信息表';

DROP TABLE IF EXISTS `refund_info`;
CREATE TABLE `refund_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '编号',
  `out_trade_no` varchar(50) DEFAULT NULL COMMENT '对外业务编号',
  `order_id` bigint DEFAULT NULL COMMENT '订单编号',
  `payment_type` tinyint DEFAULT NULL COMMENT '支付类型（微信 支付宝）',
  `trade_no` varchar(50) DEFAULT NULL COMMENT '交易编号',
  `total_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
  `subject` varchar(200) DEFAULT NULL COMMENT '交易内容',
  `refund_status` tinyint DEFAULT NULL COMMENT '退款状态',
  `callback_content` varchar(1000) DEFAULT NULL COMMENT '回调信息',
  `callback_time` datetime DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(1:已删除，0:未删除)',
  PRIMARY KEY (`id`),
  KEY `idx_out_trade_no` (`out_trade_no`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 COMMENT='退款信息表';
