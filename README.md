# 医院预约挂号平台

医院预约挂号平台是一套面向医院接入、运营管理和患者预约的前后端分离微服务项目。仓库同时包含医院模拟端、Spring Cloud 业务后端、运营管理端和用户门户，便于在一个版本中完成联调与交付。

## 项目组成

| 子项目 | 技术栈 | 默认端口 | 说明 |
| --- | --- | ---: | --- |
| `hospital-manage/hospital-manage` | Spring Boot 2.2、Thymeleaf、MyBatis-Plus | 9998 | 医院接入与数据上报模拟端 |
| `yygh_parent/yygh_parent` | Java 17、Spring Boot 3.0、Spring Cloud、Nacos | 8160、8201–8260、8222 | 网关、8 个业务服务和公共模块 |
| `yygh-admin/yygh-admin` | Vue 2、Element UI、Vue CLI | 9528 | 运营管理端 |
| `yygh-sitedemo` | Nuxt 2、Vue 2、Element UI | 3000 | 患者预约门户 |

核心依赖包括 MySQL、MongoDB、Redis、RabbitMQ、Nacos；订单服务可选接入 Sentinel，支付、登录和文件服务分别需要微信与阿里云 OSS 凭据。

## 快速开始

1. 安装 JDK 17、Maven 3.9、Node.js 16、MySQL 8、MongoDB、Redis、RabbitMQ 和 Nacos。
2. 按 [配置说明](docs/configuration.md) 设置数据库密码、JWT 密钥和第三方凭据。
3. 导入 `database/schema/` 下的 5 个纯结构 SQL；业务演示数据按 [数据库说明](database/README.md) 单独处理。
4. 按 [开发指南](docs/development-guide.md) 启动依赖、业务服务、网关和两个前端。
5. 通过网关 `http://localhost:8222` 联调；静态接口清单见 [接口文档](docs/api-reference.md)。

## 文档

- [文档导航](docs/README.md)
- [系统架构](docs/architecture.md)
- [开发指南](docs/development-guide.md)
- [配置说明](docs/configuration.md)
- [接口文档](docs/api-reference.md)
- [数据库字典](docs/database-dictionary.md)
- [验证与排障](docs/testing-and-troubleshooting.md)
- [贡献规范](CONTRIBUTING.md)

## 安全说明

仓库配置只引用环境变量，不应提交真实密码、访问密钥、证书或带个人信息的数据库导出。初始化来源中的 INSERT 数据未复制到仓库。曾写入源码的第三方凭据应在对应平台轮换后再用于开发或部署。
