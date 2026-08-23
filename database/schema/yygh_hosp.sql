-- 医院预约挂号平台：yygh_hosp 纯结构初始化脚本
-- 由 tools/generate_reference_docs.py 生成；不包含源文件中的 INSERT 数据。
CREATE DATABASE IF NOT EXISTS `yygh_hosp` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yygh_hosp`;

DROP TABLE IF EXISTS `hospital_set`;
CREATE TABLE `hospital_set` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `hosname` varchar(100) DEFAULT NULL COMMENT '医院名称',
  `hoscode` varchar(30) DEFAULT NULL COMMENT '医院编号',
  `api_url` varchar(100) DEFAULT NULL COMMENT 'api基础路径',
  `sign_key` varchar(50) DEFAULT NULL COMMENT '签名秘钥',
  `contacts_name` varchar(20) DEFAULT NULL COMMENT '联系人',
  `contacts_phone` varchar(11) DEFAULT NULL COMMENT '联系人手机',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(1:已删除，0:未删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hoscode` (`hoscode`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 COMMENT='医院设置表';
