# 医院预约挂号平台

医院预约挂号平台是一套连接医院自有系统与患者的前后端分离微服务项目。仓库同时包含医院接入模拟端、Spring Cloud 平台后端、平台管理端和用户操作端，覆盖医院数据上报、平台治理、患者预约、订单、支付与退号等核心流程。

## 业务系统边界

项目由医院系统和预约挂号平台系统两部分组成：

- **医院系统：**每家医院拥有独立系统，负责维护医院基本信息、预约规则、科室和排班，同时保留院内业务与就诊数据。当前仓库通过 `hospital-manage` 模拟医院接入、签名调用和挂号状态同步。
- **预约挂号平台系统：**承接医院数据并向患者提供统一预约服务，由平台管理端和用户操作端组成。
  - **平台管理端：**供管理员维护医院设置、医院上线状态、数据字典、用户认证和统计等平台基础数据。
  - **用户操作端：**供患者登录、维护就诊人、筛选医院、选择科室与医生排班、提交订单、支付、查询和取消预约。

医院的完整病历、诊疗和院内运营数据不进入预约平台；平台只保存完成预约、支付、状态同步和审计所需的数据。详细边界和业务时序见 [系统架构](docs/architecture.md)。

## 项目组成

| 子项目 | 技术栈 | 默认端口 | 说明 |
| --- | --- | ---: | --- |
| `hospital-manage/hospital-manage` | Spring Boot 2.2、Thymeleaf、MyBatis-Plus | 9998 | 医院接入与数据上报模拟端（含医生端登录与排班停诊管理） |
| `yygh_parent/yygh_parent` | Java 17、Spring Boot 3.0、Spring Cloud、Nacos | 8160、8201–8260、8222 | 网关、8 个业务服务和公共模块 |
| `yygh-admin/yygh-admin` | Vue 2、Element UI、Vue CLI | 9528 | 平台管理端 |
| `yygh-sitedemo` | Nuxt 2、Vue 2、Element UI | 3000 | 用户操作端（患者预约门户） |

## 技术栈

| 分类 | 技术 | 当前用途与状态 |
| --- | --- | --- |
| 基础框架 | Spring Boot | 平台服务使用 Spring Boot 3.0.5 / Java 17；医院模拟端使用 Spring Boot 2.2.1 / Java 11 |
| 微服务 | Spring Cloud、Spring Cloud Alibaba | Nacos 负责注册发现，Gateway 提供统一入口与鉴权，OpenFeign 负责服务间调用，订单服务接入 Sentinel |
| 持久层 | MyBatis-Plus | 简化 MySQL 实体映射、分页和 CRUD |
| 关系数据库 | MySQL | 分库存储医院设置、字典、用户、订单、支付和退款等关系数据 |
| 文档数据库 | MongoDB | 保存医院、科室和排班等文档数据 |
| 缓存 | Redis | 保存验证码及业务缓存 |
| 消息队列 | RabbitMQ | 用于排班数量、预约通知、短信和定时任务的异步解耦 |
| API 文档 | Knife4J / OpenAPI、Springfox Swagger | 平台后端使用 Knife4J 依赖，医院模拟端提供 Swagger 配置；仓库另有代码生成的静态接口文档 |
| 对象存储 | 阿里云 OSS | `service-oss` 已接入阿里云 OSS；MinIO 尚未接入，可作为私有化部署扩展 |
| 支付 | 微信支付 | 支持下单、支付查询、回调验签和退款；本地可启用模拟支付，生产必须关闭模拟开关并配置真实商户凭据 |
| 辅助开发 | Lombok | 生成实体类常用访问方法，减少样板代码 |
| 前端 | Nuxt 2、Vue 2、Element UI | 分别实现用户操作端与平台管理端 |
| 容器化 | Docker | 作为部署目标；当前仓库尚未提供 Dockerfile 或 Compose 编排，不能视为已完成的交付能力 |

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
