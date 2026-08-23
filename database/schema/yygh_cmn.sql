-- 医院预约挂号平台：yygh_cmn 纯结构初始化脚本
-- 由 tools/generate_reference_docs.py 生成；不包含源文件中的 INSERT 数据。
CREATE DATABASE IF NOT EXISTS `yygh_cmn` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yygh_cmn`;

DROP TABLE IF EXISTS `dict`;
CREATE TABLE `dict` (
  `id` bigint NOT NULL DEFAULT '0' COMMENT 'id',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '上级id',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '名称',
  `value` bigint DEFAULT NULL COMMENT '值',
  `dict_code` varchar(20) DEFAULT NULL COMMENT '编码',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`),
  KEY `idx_dict_code` (`dict_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='组织架构表';
