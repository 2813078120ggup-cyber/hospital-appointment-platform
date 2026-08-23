-- 医院预约挂号平台：yygh_manage 纯结构初始化脚本
-- 由 tools/generate_reference_docs.py 生成；不包含源文件中的 INSERT 数据。
CREATE DATABASE IF NOT EXISTS `yygh_manage` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yygh_manage`;

DROP TABLE IF EXISTS `hospital_set`;
CREATE TABLE `hospital_set` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `hoscode` varchar(30) DEFAULT NULL COMMENT '医院编号',
  `sign_key` varchar(50) DEFAULT NULL COMMENT '签名秘钥',
  `api_url` varchar(100) DEFAULT NULL COMMENT '统一挂号平台api地址',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(1:已删除，0:未删除)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COMMENT='医院设置表';

DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `schedule_id` bigint DEFAULT NULL COMMENT '排班id',
  `patient_id` bigint DEFAULT NULL COMMENT '就诊人id',
  `number` int DEFAULT NULL COMMENT '预约号序',
  `fetch_time` varchar(50) DEFAULT NULL COMMENT '建议取号时间',
  `fetch_address` varchar(255) DEFAULT NULL COMMENT '取号地点',
  `amount` decimal(10,0) DEFAULT NULL COMMENT '医事服务费',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `quit_time` datetime DEFAULT NULL COMMENT '退号时间',
  `order_status` tinyint DEFAULT NULL COMMENT '订单状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(1:已删除，0:未删除)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb3 COMMENT='订单表';

DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule` (
  `id` bigint NOT NULL DEFAULT '0' COMMENT '编号',
  `hoscode` varchar(30) DEFAULT NULL COMMENT '医院编号',
  `depcode` varchar(30) DEFAULT NULL COMMENT '科室编号',
  `title` varchar(20) DEFAULT NULL COMMENT '职称',
  `docname` varchar(20) DEFAULT NULL COMMENT '医生名称',
  `skill` text COMMENT '擅长技能',
  `work_date` date DEFAULT NULL COMMENT '安排日期',
  `work_time` tinyint DEFAULT '0' COMMENT '安排时间（0：上午 1：下午）',
  `reserved_number` int DEFAULT '0' COMMENT '可预约数',
  `available_number` int DEFAULT '0' COMMENT '剩余预约数',
  `amount` decimal(10,0) DEFAULT NULL COMMENT '挂号费',
  `status` tinyint DEFAULT NULL COMMENT '排班状态（-1：停诊 0：停约 1：可约）',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(1:已删除，0:未删除)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='医生日程安排表';
