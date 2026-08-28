# 系统架构

## 业务系统边界

```mermaid
flowchart LR
    subgraph Hospitals[医院侧：每家医院独立部署]
        HM[医院系统]
        HospitalData[(医院基本信息<br/>预约规则<br/>科室与排班)]
        ClinicalData[(院内业务与就诊数据)]
        Retained[仅保留在医院侧]
        HM --> HospitalData
        HM --> ClinicalData
        ClinicalData --> Retained
    end

    subgraph Platform[预约挂号平台]
        Admin[平台管理端]
        Portal[用户操作端]
        Services[平台微服务]
        PlatformData[(平台业务数据)]
        Admin --> Services
        Portal --> Services
        Services --> PlatformData
    end

    HospitalData -->|签名上报与状态同步| Services
    Services -->|挂号、支付及退号状态| HM
```

- 医院系统维护医院、预约规则、科室和排班，并负责院内挂号记录与取号状态。
- 平台管理端维护接入配置、医院上线状态、公共字典、用户认证和统计等平台数据。
- 用户操作端只通过平台访问已上线医院，完成就诊人、预约订单、支付和取消操作。
- 医院的完整病历与诊疗数据留在院内系统，平台不复制无关的医疗数据。

## 总体结构

```mermaid
flowchart LR
    Patient[患者浏览器] --> Site[Nuxt 用户门户 :3000]
    Operator[平台管理员] --> Admin[Vue 管理端 :9528]
    HospitalUser[医院人员] --> HM[医院模拟端 :9998]
    Site --> Gateway[Spring Cloud Gateway :8222]
    Admin --> Gateway
    HM --> Gateway

    Gateway --> Hosp[service-hosp :8201]
    Gateway --> Cmn[service-cmn :8202]
    Gateway --> Msm[service-msm :8204]
    Gateway --> Oss[service-oss :8205]
    Gateway --> Orders[service-orders :8207]
    Gateway --> User[service-user :8160]
    Gateway --> Sta[service-sta :8260]

    Task[service-task :8208] --> Rabbit[(RabbitMQ)]
    Hosp --> MySQL[(MySQL)]
    Cmn --> MySQL
    Orders --> MySQL
    User --> MySQL
    HM --> MySQL
    Hosp --> Mongo[(MongoDB)]
    Orders --> Mongo
    Cmn --> Redis[(Redis)]
    Msm --> Redis
    Orders --> Redis
    User --> Redis
    Hosp --> Rabbit
    Msm --> Rabbit
    Orders --> Rabbit
    Gateway -. 服务发现 .-> Nacos[(Nacos)]
    Hosp -. 服务发现 .-> Nacos
    Orders -. Feign .-> Hosp
    Orders -. Feign .-> User
    User -. Feign .-> Cmn
```

## 核心业务流程

```mermaid
sequenceDiagram
    autonumber
    participant H as 医院系统
    participant HP as 医院服务
    participant A as 平台管理端
    participant U as 用户操作端
    participant O as 订单服务
    participant W as 微信支付
    participant MQ as RabbitMQ

    H->>HP: 签名上传医院、预约规则、科室和排班
    A->>HP: 审核并上线/下线医院
    HP-->>U: 展示已上线医院、科室和可预约排班
    U->>O: 选择排班与就诊人并确认挂号
    O->>H: 签名提交医院挂号订单
    H-->>O: 返回医院记录号、取号信息和剩余号源
    O->>O: 保存平台待支付订单
    O->>MQ: 异步同步号源并发送预约通知
    U->>W: 发起微信支付
    W-->>O: 支付回调或支付状态查询成功
    O->>H: 同步医院支付状态
    O-->>U: 更新为已支付
    opt 用户在截止时间前取消
        U->>O: 申请取消预约
        O->>H: 取消医院挂号记录
        opt 已支付订单
            O->>W: 申请退款
            W-->>O: 返回退款结果
        end
        O->>MQ: 恢复号源并发送取消通知
        O-->>U: 更新为已取消
    end
    H->>HP: 同步停诊、取号或其他预约状态
```

生产链路通过医院 `apiUrl/signKey` 和微信商户配置调用真实外部接口。本地演示可启用 `yygh.mock-hospital.enabled` 与 `yygh.mock-payment.enabled`，用于演示下单和支付；模拟模式不是生产实现，上线前必须关闭，并重新验证取消、退款和医院状态同步链路。

## 服务职责

| 服务 | 端口 | 主要职责 | 主要存储/依赖 |
| --- | ---: | --- | --- |
| `service-gateway` | 8222 | 统一入口、跨域、按 `/hosp`、`/cmn`、`/user`、`/msm`、`/oss`、`/order`、`/statistics` 路由 | Nacos |
| `service-hosp` | 8201 | 医院设置、医院/科室/排班数据、医院平台接口 | `yygh_hosp`、MongoDB、RabbitMQ |
| `service-cmn` | 8202 | 行政区划与通用字典、Excel 导入导出 | `yygh_cmn`、Redis |
| `service-msm` | 8204 | 验证码与短信消息消费 | Redis、RabbitMQ、短信供应商 |
| `service-oss` | 8205 | 文件上传 | 阿里云 OSS |
| `service-orders` | 8207 | 预约下单、订单查询/取消、支付与退款 | `yygh_order`、MongoDB、Redis、RabbitMQ、微信支付、Sentinel |
| `service-task` | 8208 | 定时任务与预约状态消息 | RabbitMQ |
| `service-sta` | 8260 | 预约统计 | Feign 调用订单服务 |
| `service-user` | 8160 | 登录、实名、用户与就诊人 | `yygh_user`、Redis、微信开放平台 |

## 数据边界

- MySQL 保存配置、字典、用户、就诊人、订单、支付和退款等关系数据。
- MongoDB 保存医院、科室和排班等文档数据；这部分没有出现在 MySQL 数据字典中。
- Redis 保存验证码和热点缓存；RabbitMQ 负责预约与短信等异步消息。
- 医院模拟端使用独立的 `yygh_manage` 数据库，不与平台业务服务共用表。
- 服务间调用通过 OpenFeign；外部流量应统一经过网关，`/inner/` 路由不得直接暴露到公网。
