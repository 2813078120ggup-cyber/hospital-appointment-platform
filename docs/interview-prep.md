# 医院预约挂号平台 - 面试技术栈全景手册

> 基于项目真实代码整理，每个技术点 = 概念 + 项目落地 + 追问树 + 参考回答。
> 项目一句话开场白：
> "我做了一个医院预约挂号平台，采用 Spring Cloud 微服务架构，包含网关、用户、医院、订单、短信、OSS、统计、定时任务等 9 个微服务，数据层混用 MySQL + MongoDB + Redis + RabbitMQ，前端是 Vue 管理端 + Nuxt 用户门户，还集成了一套基于 LangChain4j + DeepSeek 的 AI 智能导诊挂号助手。我在项目里深度处理过分布式事务、订单幂等、号源并发扣减、JWT 统一鉴权等典型难题。"

---

## 目录

- [一、整体架构](#一整体架构)
- [二、Spring Cloud Gateway 网关](#二spring-cloud-gateway-网关)
- [三、Nacos 注册与配置中心](#三nacos-注册与配置中心)
- [四、OpenFeign 服务间调用](#四openfeign-服务间调用)
- [五、RabbitMQ 异步消息](#五rabbitmq-异步消息)
- [六、Redis 应用](#六redis-应用)
- [七、MongoDB 与号源并发](#七mongodb-与号源并发)
- [八、MySQL + MyBatis-Plus](#八mysql--mybatis-plus)
- [九、订单系统（幂等 + 状态机 + 最终一致性）](#九订单系统)
- [十、JWT 认证链路](#十jwt-认证链路)
- [十一、AI 模块 LangChain4j + DeepSeek](#十一ai-模块)
- [十二、前端（Vue 管理端 + Nuxt 门户）](#十二前端)
- [十三、通用工程能力](#十三通用工程能力)
- [十四、高频开放题](#十四高频开放题)
- [附录 A：踩坑记录速查表](#附录-a踩坑记录速查表)
- [附录 B：技术栈清单速查表](#附录-b技术栈清单速查表)

---

# 一、整体架构

## 1.1 架构图（口述版）

```
用户浏览器 ──┐
             ├─> Gateway(8222) ──> Nacos 服务发现 ──> service_user(8160)
管理端浏览器 ─┘        │                            service_hosp(8201)
                       │                            service_orders(8207)
                       │                            service_cmn(8202)
                       │                            service_msm(8204)
                       │                            service_oss(8205)
                       │                            service_sta(8260)
                       │                            service_task(8208)
                       │
AI 助手(8080, LangChain4j) ──RestTemplate──> Gateway ──> 订单/医院服务
                       │
医院模拟系统(9998, hospital-manage) ──HTTP+签名──> service_hosp
                       │
中间件：MySQL(3306) / MongoDB(27017) / Redis(6379) / RabbitMQ(5672) @ 192.168.6.101
```

## 1.2 模块职责

| 模块 | 端口 | 职责 |
|---|---|---|
| service_gateway | 8222 | 统一入口、JWT 鉴权、路由、屏蔽 inner 接口 |
| service_user | 8160 | 登录注册、就诊人管理、用户认证 |
| service_hosp | 8201 | 医院/科室/排班（MongoDB）、号源扣减 |
| service_orders | 8207 | 订单创建/取消/支付、补偿 outbox |
| service_cmn | 8202 | 字典数据（省市区等） |
| service_msm | 8204 | 短信验证码发送 |
| service_oss | 8205 | 文件上传 |
| service_sta | 8260 | 统计 |
| service_task | 8208 | 定时任务调度 |
| java-ai-langchain4j | 8080 | 小智 AI 助手（导诊/查号/挂号/取消） |
| hospital-manage | 9998 | 模拟外部医院系统上报数据 |
| yygh-sitedemo | 3000 | 用户门户（Nuxt SSR） |
| yygh-admin | 9528 | 平台管理端（Vue2 + ElementUI） |

## 1.3 为什么用微服务（必考）

**答**：
1. **按业务域拆分**：用户、医院、订单等九个模块独立部署，预约高峰只横向扩容订单/医院服务，不用整体扩容
2. **故障隔离**：短信服务挂了不影响下单主流程（MQ 异步解耦后更是如此）
3. **技术异构**：医院数据用 MongoDB、订单用 MySQL，单体里混用多种存储虽然可行但耦合度高
4. **团队并行**：接口契约（inner API + MQ 消息契约）定义好即可并行开发

**代价**（主动说，显得清醒）：
- 运维复杂度上升：需要 Nacos 注册中心、网关、链路排查手段
- 分布式问题：幂等、最终一致性、分布式锁全部要自己处理
- 排查成本：一次"全服务正常但无法登录"的事故，根因是网关和业务服务 JWT 密钥不一致——单体不会有这类问题

---

# 二、Spring Cloud Gateway 网关

## 2.1 核心实现

**代码位置**：`service_gateway/.../filter/AuthGlobalFilter.java`

**要点**：
- 实现 `GlobalFilter + Ordered`，全局过滤器对所有路由生效
- 路由配置（application.properties）：

```properties
spring.cloud.gateway.routes[2].id=service-user
spring.cloud.gateway.routes[2].uri=lb://service-user
spring.cloud.gateway.routes[2].predicates=Path=/*/user/**
```

- `lb://` 前缀 = 走 Spring Cloud LoadBalancer，从 Nacos 拉服务实例做客户端负载均衡
- 路径断言 `Path=/*/user/**` 匹配 `/api/user/**` 形式的请求

## 2.2 鉴权流程（背熟）

```
请求进来
  ├─ OPTIONS 请求 → 直接放行（CORS 预检）
  ├─ 路径含 /inner/ → 403（内部 Feign 接口禁止外网绕过）
  ├─ 白名单路径（登录、医院列表等） → 放行
  └─ 需要认证的路径：
       ├─ 取 token：token 头 → X-Token 头 → query 参数（兼容旧管理端）
       ├─ 无 token → 401 + code 50008 "请先登录"
       └─ 有 token：
            ├─ JWT 验签（HMAC-SHA256）
            ├─ 解析 userId / userName / 管理员角色
            ├─ 管理端接口校验管理员权限
            └─ 认证通过 → 请求透传下游
```

## 2.3 追问树

**Q：为什么用 Gateway 不用 Zuul / Nginx？**
> Zuul 1.x 是阻塞 IO，Gateway 基于 WebFlux + Netty 响应式非阻塞，吞吐更好；相比 Nginx，Gateway 是 Java 生态内的 API 网关，过滤器可以用 Java 写复杂业务逻辑（JWT 验签、角色判断），和 Nacos/LoadBalancer 无缝集成。

**Q：Gateway 的三种过滤器？**
> 1. GlobalFilter：全局生效，我的 AuthGlobalFilter 就是
> 2. GatewayFilter：只对指定路由生效（配置文件里 filters）
> 3. DefaultFilter：对所有路由生效的配置式过滤器
> 执行顺序由 `Ordered.getOrder()` 决定，值越小优先级越高。

**Q：实际踩过什么坑？（有真实故事，强烈建议讲）**
> 事故：全部服务启动正常，但前端登录后所有 auth 接口返回 50008"登录凭证已失效"。
> 排查过程：
> 1. 端口全监听 → Nacos 注册全正常 → 排除服务挂掉
> 2. 绕过网关直连 service_user（8160）→ 正常 → 锁定网关
> 3. 走网关登录拿 token 再走网关调 auth 接口 → 50008
> 4. 用 HMAC 手工验签 token 签名 → 发现用户服务用的是密钥 A，网关用的是密钥 B
> 根因：JWT 密钥有两条读取路径——网关读 Spring 属性 `${yygh.jwt.secret}`（来自环境变量），业务服务 `JwtHelper` 读 `System.getenv("YYGH_JWT_SECRET")`，两边进程继承的环境不同。
> 修复：重启网关时显式注入相同密钥。
> 沉淀：**签发方和验签方的密钥必须收敛到单一配置来源**（生产用 Nacos 配置中心或 K8s Secret）；排查微服务问题的分层法（端口 → 注册中心 → 直连绕网关）很高效。

**Q：网关怎么做限流？**
> 可以用内置 RequestRateLimiter 过滤器 + Redis 令牌桶（`spring.cloud.gateway.routes[].filters=RequestRateLimiter`），按 IP 或用户维度限 key。当前项目预约量级不大暂未上，但方案门儿清。

---

# 三、Nacos 注册与配置中心

## 3.1 项目落地

```properties
spring.cloud.nacos.discovery.server-addr=192.168.6.101:8848
# Windows 多网卡环境显式注册 VMnet8 地址，避免注册成错误网卡
spring.cloud.nacos.discovery.ip=192.168.6.1
spring.cloud.nacos.discovery.ip-type=IPv4
```

## 3.2 必答要点

**Q：Nacos 服务注册的流程？**
> 服务启动时向 Nacos 发注册请求（服务名 + IP + 端口 + 元数据）；Nacos 客户端定时（默认 5 秒）发心跳续约，超过 15 秒没心跳标记不健康，30 秒剔除；消费方（如网关）订阅服务列表，实例上下线时 Nacos 主动推送变更（UDP/长轮询通知），消费方本地缓存实例列表。

**Q：为什么显式指定 discovery.ip？**
> 我的 Windows 开发机有多个网卡（物理网卡、VMnet8 虚拟网卡），Nacos 自动探测可能把虚拟机网卡的 IP 注册上去，导致服务间调用不通。显式配置注册 IP 是多网卡环境的标准做法。生产 K8s 环境同理要关注 Pod IP 注册。

**Q：Nacos 既是注册中心又是配置中心，配置中心怎么用？**
> 引入 `nacos-config` 依赖，`bootstrap.yml` 配 dataId，`@RefreshScope + @Value` 实现配置热更新；支持 namespace（环境隔离）、group（项目隔离）、dataId（具体配置）。我的项目目前配置在本地 properties + 环境变量占位符，生产计划把密钥类配置迁入 Nacos——正好引出密钥事故的教训。

**Q：Nacos vs Eureka / Zookeeper？**
> - Eureka：AP 系统，2.x 已停止维护
> - Zookeeper：CP 系统，选举期间不可用，重客户端
> - Nacos：默认 AP（Distro 协议，临时实例），可切 CP（Raft，持久实例），同时融合配置中心，一站式
> 我选 Nacos 是因为生态整合度最好（Spring Cloud Alibaba 全家桶）。

---

# 四、OpenFeign 服务间调用

## 4.1 项目落地

订单服务下单流程的典型跨服务调用：

```java
// service_orders 通过 Feign 接口声明式调用
@FeignClient(value = "service-user")   // 拿就诊人
public interface PatientFeignClient {
    @GetMapping("/api/user/patient/inner/getPatientInfoById/{id}")
    Patient getPatientInfo(@PathVariable("id") Long id);
}

@FeignClient(value = "service-hosp")   // 扣减号源
public interface HospitalFeignClient {
    @PostMapping("/api/hosp/department/inner/decrementAvailableNumber")
    ...
}
```

特点：
- `inner/` 路径前缀 = 服务间专用接口，网关层禁止外网访问
- 客户端模块（service_hosp_client、service_user_client 等）独立成 Maven 模块，被消费方依赖，接口契约复用

## 4.2 追问树

**Q：Feign 的原理？**
> 动态代理：`@FeignClient` 接口启动时生成代理对象，调用方法时被拦截，把方法上的注解（@GetMapping、@PathVariable）翻译成 HTTP 请求 → 通过 LoadBalancer 从 Nacos 实例列表选一个地址 → HTTP 客户端（默认 JDK URLConnection，可换 OkHttp/HttpClient）发出请求 → 响应反序列化成返回类型。本质是"声明式 RPC over HTTP"。

**Q：Feign 调用 404 的真实排查？**
> 一次 Feign 调医院服务扣号接口 404。根因是医院侧 Controller 方法定义了 `@PostMapping` 但文件缺少对应 import 注解，导致该路径没有注册成 POST 映射。教训：Feign 404 先确认双方 method 类型一致，再看对方服务直连是否通，最后检查注解是否真正生效。

**Q：Feign 超时/重试怎么配？**
> 连接超时/读超时通过 `feign.client.config.default.connectTimeout/readTimeout` 配置。重试默认不开启（ Retryer.NEVER_RETRY），重试要小心——**对写操作开重试可能造成重复下单**，我项目里幂等由订单层的 Redis 锁 + 幂等键兜底。

**Q：Feign 怎么传 token？**
> 实现 `RequestInterceptor`，从 `RequestContextHolder` 拿当前请求的 token 头塞进 Feign 请求。AI 模块里更复杂：LangChain4j 工具执行切了线程，ThreadLocal 丢 token，我做了三级降级取 token（见 AI 模块章节）。

**Q：什么时候用 Feign，什么时候用 MQ？**
> 需要同步拿结果的用 Feign（下单必须先拿到就诊人信息）；通知类、可异步的用 MQ（下单后发短信、通知医院端）。下单主链路里"扣号"必须同步（失败就不能下单），但"通知医院端更新订单状态"走 MQ 异步。

---

# 五、RabbitMQ 异步消息

## 5.1 项目落地

**常量定义**：`common/rabbit_util/.../MqConst.java`（统一契约）

```java
// 订单状态同步：订单服务 → 医院服务
exchange: exchange.direct.order
routing key: routing.order / routing.cancel.order (取消)
queue: queue.hospital.order / queue.hospital.cancel-order

// 短信：订单服务 → 短信服务
exchange: exchange.direct.msm
queue: queue.msm.sms

// 定时任务
exchange: exchange.direct.task
```

**发送方封装**：`RabbitService.sendMessage(exchange, routingKey, message)`

**消费方**：`service_hosp/.../receiver/HospitalReceiver.java`
- `@RabbitListener(bindings = @QueueBinding(...))` 声明式绑定交换机/队列/路由键
- 监听订单队列：处理预约下单后的号源同步、取消预约的号源回补，再转发短信通知

## 5.2 消息流转图

```
用户下单成功
  → 订单服务本地事务提交（订单落库）
  → 发消息到 exchange.direct.order (routing: routing.order)
  → 医院服务 HospitalReceiver 消费：
      ├─ 更新医院端本地订单副本
      ├─ 号源同步/回补
      └─ 转发短信服务（exchange.direct.msm）发就诊通知
```

## 5.3 追问树

**Q：为什么用 direct 交换机？**
> 业务需要精确路由：不同 routing key（下单 vs 取消）进不同队列，direct 按 key 精确匹配，语义清晰开销小。topic 适合通配符场景，fanout 适合广播，这里用不上。

**Q：消息丢了怎么办？（可靠性投递，必考）**
> 三个环节分别保：
> 1. **生产端**：`publisher-confirms` 开启确认回调，失败重发或落补偿表（我的订单补偿 outbox 本质就是消息表思想）
> 2. **Broker 端**：交换机/队列/消息都设置持久化（durable + deliveryMode=2）
> 3. **消费端**：手动 ack，业务处理成功再 `basicAck`；失败 `basicNack` 重回队列（配合重试上限 + 死信队列防无限循环）

**Q：消息重复消费怎么办？**
> 消费端幂等：我的消费场景（号源同步、取消回补）在 MongoDB 层做条件更新——回补号源用 CAS 式条件 `$inc`，重复消费时第二次更新条件不满足自然不生效。通用方案是消费前查幂等表（消息 ID 唯一索引）。

**Q：MQ 的削峰怎么体现？**
> 就诊提醒短信在预约高峰集中触发，短信服务（依赖外部供应商）吞吐有限，消息在队列排队消费，避免直接打爆短信服务。这与秒杀场景 MQ 削峰同一原理。

**Q：死信队列了解吗？**
> 消费重试超限、队列满了、TTL 过期的消息进 DLX。我的场景可以把取消补偿类消息的重试失败死信化，转人工处理。生产通常 TTL + DLX 组成延迟队列——比如"订单 15 分钟未支付自动取消"就是典型延迟队列场景（我的项目取消由用户主动发起 + outbox 补偿，暂未用延迟队列）。

---

# 六、Redis 应用

## 6.1 四个真实场景（难度递增）

### 场景 1：登录验证码（含防刷 + 原子校验）

**代码位置**：`UserInfoServiceImpl.loginUser()` / `MsmController.send()`

```
发送：
  SETNX sms:login:cooldown:{phone} 1 EX 60    ← 60 秒冷却，防刷（短信接口被刷会产生真金白银的费用）
  发送成功后 SET sms:login:code:{phone} {code} EX 300   ← 5 分钟有效

校验（Lua 脚本原子执行）：
  if redis.call('get', KEYS[1]) == ARGV[1]
  then return redis.call('del', KEYS[1])
  else return 0 end
```

**为什么 Lua 必要**："先 GET 判断再 DEL" 是两步，并发下两个请求都 GET 成功然后都 DEL，验证码被用两次。Lua 在 Redis 单线程里原子执行"比对+删除"，天然防竞态，保证验证码**一次性使用**且并发登录只成功一次。

### 场景 2：订单幂等分布式锁

**代码位置**：`OrderInfoServiceImpl`（订单提交）

```
SETNX 幂等锁（基于 outTradeNo）→ 抢到才继续下单 → 重复提交抢不到直接拒绝
```

防止用户双击提交按钮 / 网络重试导致重复扣号。

### 场景 3：数据缓存

字典（省市区）、医院基础数据缓存，减压 MySQL。

### 场景 4：JWT 配套

JWT 本身无状态，Redis 存短生命周期数据（验证码、冷却键）。

## 6.2 追问树

**Q：SETNX 锁有什么问题？怎么做成"完善的"分布式锁？**
> 裸 SETNX 三个坑：
> 1. **锁过期但业务没做完** → 别人拿到锁出现并发 → 看门狗续期（Redisson 的 watchdog，每 10 秒续 30 秒）
> 2. **误删别人的锁**（A 超时后 B 拿到锁，A 执行完把 B 的锁删了）→ value 存线程标识，删之前 Lua 校验"是自己的锁才删"
> 3. **主从切换锁丢失** → RedLock 或接受小概率风险；我的场景锁窗口短、幂等键兜底，简单 SETNX + 短 TTL 够用
> 生产标准答案：Redisson。

**Q：Redis 为什么快？**
> 纯内存 + 单线程无锁竞争（命令执行）+ IO 多路复用（epoll）+ 高效数据结构（SDS、跳表、压缩列表）。6.0 后网络 IO 多线程，命令执行仍单线程，所以命令天然原子。

**Q：缓存三兄弟（穿透/击穿/雪崩）在你项目怎么防？**
> - **穿透**（查不存在的数据打穿到 DB）：医院详情按 hoscode 查，不存在的 hoscode 可以缓存空值短 TTL
> - **击穿**（热 key 过期瞬间大量请求进 DB）：热门医院数据可逻辑过期 / 互斥重建
> - **雪崩**（大量 key 同时过期）：TTL 加随机抖动
> 我的数据量级没到，但方案知道在哪加。

**Q：Redis 单线程为什么 Lua 脚本是原子的？**
> Redis 命令执行线程只有一个，Lua 脚本作为一条命令整体进入执行队列，期间不插入其他命令——所以脚本里的 GET+DEL 不会被其他客户端插队。

---

# 七、MongoDB 与号源并发

## 7.1 为什么医院数据用 MongoDB（必考）

> 三个理由：
> 1. **异构半结构化**：医院/科室/排班数据由各外部医院系统上报，不同医院字段不完全一致，MongoDB 无固定 schema，新医院加字段不用改表
> 2. **原子数值更新**：`findAndModify` 条件原子更新，天然适合号源扣减
> 3. **文档模型贴业务**：医院详情嵌套科室、排班对象，一次文档读取拿整棵树，不用多表 join
>
> 同时**强事务数据（订单/用户/支付）仍在 MySQL**——混合持久化，按业务特性选存储，这个决策本身就是面试加分点。

**追问：MongoDB 事务支持弱，不怕吗？**
> 号源一致性靠原子条件更新保证（单文档操作天然原子），不需要跨文档事务；订单一致性在 MySQL + outbox 里保证。分区治理。

## 7.2 号源扣减防超卖（核心亮点）

**代码位置**：`ScheduleServiceImpl`（`decrementAvailableNumber` / `restoreAvailableNumber`）

```
扣号：findAndModify(
    条件: _id = scheduleId AND availableNumber > 0   ← 条件原子
    更新: $inc: { availableNumber: -1 }
)
→ 找到并扣成功，或条件不满足返回空（没号了/已被并发扣完）

回补（取消预约）：CAS 式条件更新，$inc: +1 且校验当前值，防重复回补多加
```

**为什么不会超卖**：`findAndModify` 是服务端原子执行"查+改"，并发请求只有一个能满足 `availableNumber > 0` 并完成扣减，其余条件不满足直接失败。与电商 `UPDATE stock SET n = n - 1 WHERE n > 0` 同一思想：**把"判断+修改"合并成一条原子指令，而不是先读后写**。

**对比方案**：
| 方案 | 评价 |
|---|---|
| 先查后改（内存判断） | 有 TOCTOU 竞态，超卖 |
| Redis 预扣库存 | 性能更高，但需要回写 DB，复杂度上升，预约量级用不上 |
| MongoDB findAndModify | 一层存储搞定，够用（√ 我的选择） |
| 悲观锁/分布式锁 | 串行化吞吐低 |

## 7.3 时区坑（真实故事，主动讲）

> MongoDB 存 `Date` 是 UTC。北京 0 点 = UTC 前一天 16:00。我曾在修"前端无号"问题时误把存储时间改成"北京 0 点的本地表示"，结果查询条件（UTC）和存储值错位，前端查不到排班，显示"无号"。
> 最终修复：**存原始 UTC 16:00，Java 端 `parseWorkDate` 按 JVM 默认时区解析**，保证"存储值-查询条件-前端展示"三者对齐。
> 沉淀：跨时区日期问题先统一"存储基准"再谈别的；改数据前先备份。

## 7.4 追问树

**Q：MongoDB 索引怎么建？**
> 排班查询按 hoscode + depcode + workDate 组合，建复合索引；eq + 范围查询的组合索引把等值列放前面，范围列放最后（ESR 原则）。

**Q：副本集了解吗？**
> 一主多从 + oplog 异步复制，主挂了自动选主（Raft 变种）。读偏好可配 primaryPreferred。我的开发环境单机，生产会至少三节点副本集。

---

# 八、MySQL + MyBatis-Plus

## 8.1 项目落地

- 订单、用户、就诊人、字典等强一致数据
- MyBatis-Plus：`LambdaQueryWrapper` 类型安全条件构造、逻辑删除 `is_deleted` 字段、分页插件 `PaginationInnerInterceptor`、`ServiceImpl` 通用 CRUD
- 多库拆分：`yygh_user` / `yygh_hosp` / `yygh_order` / `yygh_cmn`，每个微服务独立库——微服务数据隔离原则（禁止跨库 join，跨服务取数走 Feign）

## 8.2 真实踩坑（讲出来很加分）

**hos_record_id 字段长度**：
> `order_info.hos_record_id` 原设计 `varchar(30)`。AI 渠道下单的幂等键格式是 `"MOCK-" + outTradeNo`，最长超过 30，写入报 Data too long，被全局异常处理器兜底成"执行全局异常处理"，前端只看到笼统错误。
> 修复：扩为 `varchar(100)`。
> 教训两条：
> 1. 字段长度要预估上层业务拼接内容的长度上限
> 2. 全局异常处理器是双刃剑——对用户友好，但会掩盖真实堆栈；排查时必须看服务端日志（我的 logback 把 ERROR 写到 `D:/yygh_log/log_error.log`，这次就是靠日志文件定位的）

## 8.3 追问树

**Q：索引失效场景？**
> 最左前缀不满足、列上用函数/隐式类型转换（varchar 列 = 数字）、like '%xx' 前导通配、or 连接非索引列、优化器评估回表成本高。面试常延伸 explain：重点看 type（至少 range）、key、rows、Extra（Using filesort / Using index）。

**Q：为什么订单号用 varchar 而不是 bigint？**
> 业务订单号含语义前缀（MOCK- 区分 AI 渠道单），且不做算术运算，varchar 合理。纯自增主键另用 bigint。

**Q：逻辑删除原理？**
> MP 全局配置 `logic-delete-field=is_deleted`，DELETE 自动转 `UPDATE ... SET is_deleted = 1`，查询自动拼 `is_deleted = 0`。注意唯一索引冲突问题：逻辑删除的记录还占着唯一键，我的处理是删除时给唯一列拼时间戳，或用联合唯一索引。

---

# 九、订单系统

> 面试重头戏，三个关键词：**幂等、状态机、最终一致性（outbox 补偿）**。

## 9.1 下单流程全景

```
前端提交（就诊人 + 排班 ID）
  1. 校验登录态（网关已完成 JWT 验签）
  2. Feign 调 service-user 拿就诊人（校验归属）
  3. Feign 调 service-hosp 扣减号源（MongoDB findAndModify 原子扣）
  4. 幂等检查：outTradeNo 幂等键 + Redis SETNX 锁
  5. 订单落库（MySQL，状态 NOT_PAY）
  6. 发 MQ：exchange.direct.order → 医院端同步 + 短信通知（异步）
  7. 返回订单号 → 前端跳支付
```

**同步链路只做"必须同步"的事**（2、3、4、5），通知类全部异步（6）。

## 9.2 幂等性设计

**面试官问：用户重复点击/网络重试怎么保证不重复下单？**

> 两层防线：
> 1. **业务幂等键**：基于 `outTradeNo`（商户订单号）生成幂等记录，同一单号重复提交命中已有幂等记录，直接返回原结果
> 2. **Redis 分布式锁**：提交瞬间 `setIfAbsent(outTradeNo 锁键)`，抢不到说明并发提交进行中，拒绝
>
> 另外数据库层 `hos_record_id` 存幂等键 + 唯一约束，是最后兜底（insert 冲突即重复）。

**追问：为什么不能只靠数据库唯一索引？**
> 能兜底但体验差：重复请求直接撞 500，而且号源已扣（下单顺序是先扣号再落库）还要补偿回补。Redis 锁把并发挡在最外层，幂等键保证"返回结果一致"（用户看到自己的第一笔订单），数据库约束是最后防线。**纵深防御**。

## 9.3 订单状态机

```
NOT_PAY(未支付) ──支付回调──> PAID(已支付) ──就诊完成──> FINISHED
     │
     └──用户取消/超时取消──> CANCELED(已取消)

状态机约束：
  - 只有合法迁移允许执行（NOT_PAY 才能 CANCELED）
  - 取消申请抢占：并发取消只有一个成功
  - 非法状态操作直接拒绝（已支付走退款流程而非直接取消）
```

**追问：为什么用状态机思想？**
> 订单状态是强约束的领域模型。所有修改入口（用户取消、管理员取消、MQ 回调、补偿任务）都先过"当前状态是否允许此迁移"的校验，防止并发回调把订单改乱。比散落的 if-else 集中、可测试。

## 9.4 取消订单与最终一致性（最大亮点，必须讲透）

**面试官问：取消预约要跨订单/医院/支付三个服务，怎么保证一致性？**

> 用 **outbox 补偿模式（本地消息表）**，不用 2PC：
>
> **为什么不用 2PC/Seata**：强一致事务在跨 MySQL + MongoDB + 外部支付的场景里根本无法统一协调（Mongo 不支持 XA），且 2PC 同步阻塞，锁窗口大。预约取消业务允许秒级延迟一致，最终一致是更合适的取舍。
>
> **流程**：
> 1. 取消主流程只做本地事务（都在订单库）：状态校验 → **取消申请抢占**（并发取消只有一个请求抢到资格）→ 订单状态落库 → 跨系统副作用写入 `order_compensation_outbox` 补偿任务表（同一本地事务提交，天然原子）
> 2. `CompensationTaskServiceImpl` worker 轮询 outbox：**任务认领**（多实例不重复执行）→ 调医院服务回补号源 / 同步支付状态 → 成功/失败落库，失败自动重试
> 3. 医院端处理失败打**失败标记**，重复取消有**重复取消防护**（第二次取消请求直接拒绝），不会二次回补号源
>
> **保证的性质**：本地事务保证"订单状态变更"和"补偿任务记录"原子提交（不会出现状态改了没人补偿）；worker 重试保证补偿最终执行；条件更新保证补偿本身幂等。三者合起来 = 最终一致性。

**追问：outbox 任务一直失败怎么办？**
> 重试上限 + 死信状态 + 告警人工介入。补偿任务表本身带状态流转（待执行/已认领/成功/失败），失败任务可查询、可手动重放。

**追问：这跟本地消息表、事务消息（RocketMQ）什么关系？**
> 同一思想家族：
> - 本地消息表 = outbox 最朴素形态（业务表 + 消息表同事务，定时扫描发送）
> - RocketMQ 事务消息 = 把"消息表+扫描"内置到 MQ 里（half 消息 + 回查）
> - 我的 outbox = 把"消息"泛化为"补偿任务"，支持任意跨系统副作用（不限于发消息），因为我的副作用是调 REST 接口回补号源

## 9.5 支付与退款

> 支付走回调更新订单状态（回调同样要幂等——按订单号判重）；退款在取消补偿流程里触发支付侧退款接口，同样走 outbox 补偿。

---

# 十、JWT 认证链路

## 10.1 完整流程（背熟，一条龙讲完）

```
1. 登录：手机号 + 验证码
   → service_user：Lua 原子校验 Redis 验证码
   → 查/注册用户（首次登录自动注册）
   → JwtHelper.createToken(userId, name)：
        header  {alg: HS256, zip: GZIP}
        payload {sub: USER_INFO, userId, userName, exp: 1小时}
        签名    HMAC-SHA256(密钥, header.payload)
        最终 token = base64(header).base64(payload).base64(signature)

2. 前端存 cookie（path: '/'，不能用 domain: 'localhost'）
   每次请求带 token 头

3. 网关 AuthGlobalFilter：验签 → 解析 userId/userName → 权限判断 → 放行

4. 业务服务：AuthContextHolder.getUserId(request) 解析 userId
   PatientController.requireUserId() + requireOwnedPatient()：
   校验"这个就诊人是不是当前用户的"——资源归属校验，防横向越权
```

## 10.2 追问树

**Q：为什么 payload 只放 userId/userName？**
> JWT payload 只是 Base64 编码，**不是加密**，任何人可解码。敏感信息（手机号、证件号）绝不能放。放 userId 足够标识身份，其他数据按需查库。

**Q：JWT 怎么强制失效（登出/改密踢下线）？**
> 原生 JWT 做不到（无状态是特性也是缺陷）。方案：
> 1. Redis 黑名单：登出时把 token 的 jti 存黑名单到自然过期
> 2. 短过期 + refresh token 轮换（我项目 1 小时短过期兜底）
> 3. 改密场景：用户维度 token 版本号，改密后版本 +1，旧 token 全失效
> 我的取舍：1 小时过期对预约挂号场景够用，未引入黑名单复杂度。

**Q：JWT vs Session？**
> - Session：状态在服务端，分布式要 Session 共享（Spring Session + Redis）
> - JWT：状态在 token 里，网关验签即可、天然水平扩展；代价是无法主动失效、token 体积随 claim 增长
> 微服务 + 网关架构下 JWT 更主流，配合 Redis 补"可控失效"短板。

**Q：HMAC-SHA256 密钥的要求？**
> ≥ 32 字节（我的代码显式校验并启动即失败），因为 HMAC-SHA256 安全强度依赖密钥熵。密钥只从环境注入（`${YYGH_JWT_SECRET:}` 占位符），严禁硬编码进仓库——我项目里网关和 JwtHelper 双路径读密钥踩过不一致的事故（见网关章节），生产会统一收敛到 Nacos/K8s Secret。

**Q：CSRF 需要防吗？**
> 用 token 头（非 cookie 自动携带）的方案天然免疫 CSRF——攻击者跨站请求无法自动附带自定义 token 头。这比纯 cookie-session 方案省了 CSRF token。

---

# 十一、AI 模块

> 差异化亮点，传统 CRUD 项目没有的东西。关键词：**LangChain4j、@AiService、Tool Calling、ChatMemory、AI 安全**。

## 11.1 架构

- 独立 Spring Boot 3.2.6 服务，`langchain4j-spring-boot-starter`（1.4.0-beta10）
- 模型：DeepSeek（OpenAI 兼容协议）`base-url=https://api.deepseek.com`，model `deepseek-chat`，temperature 0.0（工具调用场景要确定性）
- 记忆：MongoDB `chat_memory_db`（多轮会话持久化，重启不丢）

## 11.2 AiService + Tool Calling（必考：AI 怎么"真的"挂上号）

**Agent 声明**（`XiaozhiAgent.java`）：

```java
@AiService(
    wiringMode = EXPLICIT,
    chatModel = "openAiChatModel",
    chatMemoryProvider = "chatMemoryProviderXiaozhi",
    tools = "appointmentTools")
public interface XiaozhiAgent {
    @SystemMessage(fromResource = "xiaozhi-prompt-template.txt")
    String chat(@MemoryId Long memoryId, @UserMessage String userMessage);
}
```

**工具定义**（`AppointmentTools.java`）：

```java
@Tool(name = "book_appointment",
      value = "预约挂号：用户确认预约信息后，使用当前登录账号下匹配的就诊人创建平台正式订单。")
public String bookAppointment(
    @ToolMemoryId Long memoryId,
    @P(value = "就诊人姓名") String username,
    @P(value = "证件号后四位", required = false) String idCardLast4,
    @P(value = "科室名称") String department,
    @P(value = "日期") String date,
    @P(value = "时间，可选值：上午、下午") String time,
    @P(value = "医生名称", required = false) String doctorName) { ... }
// 另有 query_schedule、cancel_appointment
```

**机制串讲**：
> 1. LangChain4j 启动时把 `@Tool` 方法 + `@P` 参数描述自动转成 OpenAI function schema，随每次请求的 `tools` 字段发给大模型
> 2. 模型判断需要查号/挂号时，不输出文字而是返回 `tool_calls`（工具名 + JSON 参数）
> 3. 框架本地反射执行对应方法，把返回值作为 tool 消息回传模型
> 4. 模型基于真实工具结果生成最终自然语言回答
> **关键设计：预约/取消工具内部用 RestTemplate 走网关调真实订单接口**——AI 只是自然语言入口，落单仍走统一 JWT 鉴权链路，绝不直连 inner 接口绕过资源归属校验。

## 11.3 多轮记忆

> `@MemoryId Long memoryId` 标识会话，`ChatMemoryProvider` 按会话存取消息历史（MongoDB 持久化）。用户 A 和用户 B 的 memoryId 不同，记忆互不污染；服务重启对话续得上。工具方法上也有 `@ToolMemoryId`，工具执行时能拿到会话标识（用于 token 注册表查找）。

## 11.4 AI 安全设计（主动讲，强加分）

> 1. **敏感信息零收集**：提示词明文约束"绝对不要让用户在对话中发送密码、JWT、短信验证码或任何密钥"；证件信息由工具从登录账号的就诊人数据匹配，AI 只接收姓名 + 证件后四位，完整证件号永远不出现在对话里
> 2. **双层确认机制**：
>    - 提示词层：创建/取消订单前必须复述清单（就诊人/科室/日期/时段/医生）获得用户明确确认
>    - 代码层：工具执行前正则校验用户消息（`^(确认|确认预约|...)$`），不匹配直接拒——**防提示注入绕过 AI 直接触发下单**
> 3. **越权防护**：工具不信任 AI 传入的用户身份，身份一律取当前请求 token 解析（走网关统一鉴权），AI 只是传话员
> 4. **输出防泄漏**：小模型偶尔把工具调用当文本输出（`<query>{json}</query>` 形式），`XiaozhiConversationService` 用正则拦截并解析白名单工具，白名单外的原始调用一律不展示给用户

## 11.5 Token 三级降级（真实 bug 修复故事）

> **问题**：AI 预约报"登录状态可能存在问题"，但用户明明登录了。
> **定位**：LangChain4j 的工具执行可能切到异步线程，`ThreadLocal` 里存的 token 丢失。
> **修复**：三级降级取 token（`AppointmentTools.currentToken()`）：
> 1. `AuthenticatedRequestContext`（ThreadLocal，Controller 入口存入）
> 2. `RequestContextHolder`（同线程的 HTTP 请求头兜底）
> 3. `SessionTokenRegistry`（`ConcurrentHashMap<Long, String>` 按 memoryId 注册，Controller 存入、finally 移除）
> **沉淀**：凡是"异步执行 + 请求上下文"组合，ThreadLocal 必丢，要做显式传递或注册表。

## 11.6 其他工程细节

- RestTemplate 连接 5s / 读取 30s 超时，防 AI 对话线程被远程服务拖死
- `ModelReadinessService` 探活模型，`/xiaozhi/status` 返回 ready/degraded
- `XiaozhiExceptionHandler`（`@RestControllerAdvice(assignableTypes=...)`）AI 独立异常语义：503 + 50300
- starter 冲突教训：同时引 Ollama/DashScope 的 starter 会 bean 冲突，**注释配置解决不了 starter 冲突，必须从 pom 移除**，只保留 open-ai 一个

## 11.7 追问树

**Q：Function Calling 的原理？**
> 大模型本身只输出文本，但可以在 system/user 消息附带工具 JSON Schema；训练过的模型（DeepSeek/OpenAI 均支持）会在需要时输出结构化的 tool_calls 调用请求，由**应用侧**执行真实函数再把结果喂回去。本质是"模型决策 + 代码执行"的分权：模型决定调什么、传什么参，但真正执行在你的服务器。

**Q：为什么 temperature 设 0.0？**
> 工具调用和参数提取要确定性输出。高温会增加"编造参数/不调工具直接闲聊"的概率。创意闲聊场景才升温。

**Q：RAG 用了吗？**
> 当前业务（查号/挂号）是工具型 Agent，不需要检索增强。如果要做"医院介绍/就医指南问答"，会加 RAG：文档切片 → 向量化（EmbeddingModel）→ 向量库（MongoDB Atlas Vector Search / Milvus）→ 检索 top-k 拼进 prompt。方案清楚，量级暂不需要。

**Q：Prompt 注入怎么防？**
> 分层：输入侧（用户消息永远作为 user role，不拼进 system）；权限侧（工具不信任 AI 传的身份，身份只从 token 解析）；动作侧（写操作前代码层正则校验确认词，不依赖模型的自我约束）。核心思想：**模型输出的任何内容都不可信，权限判断必须在代码层**。

---

# 十二、前端

## 12.1 管理端（yygh-admin，9528）

**栈**：Vue 2 + Element UI + Vuex + Vue Router + axios + ECharts（统计）

**核心设计（面试点）**：
- **axios 拦截器双截**：
  - 请求拦截：统一注入 `token` 头
  - 响应拦截：`code !== 20000` 统一弹错；`50008/50012/50014`（凭证失效/异地登录/过期）自动提示"登录过期"→ `store.dispatch('user/resetToken')` → 回登录页（HTTP 层的 401 + 50008 同样处理）
- **路由守卫**：`permission.js` 全局 beforeEach，无 token 跳登录并带 redirect 参数，登录后回跳
- **环境配置**：`.env.development/.env.production` 管理 `VUE_APP_BASE_API`

## 12.2 用户门户（yygh-sitedemo，3000）

**栈**：Nuxt.js（SSR）+ Element UI

**为什么 Nuxt（SEO 答案）**：
> 医院/科室页面是公开内容页，需要搜索引擎收录引流。SPA 纯 CSR 首屏白屏且爬虫执行 JS 能力有限；Nuxt SSR 服务端直出 HTML，SEO 友好 + 首屏快。管理端不需要 SEO，用 SPA 即可——**按页面属性选渲染方案**。

**真实坑（讲）**：
1. **SSR 网络错误**：SSR 阶段网关没起，axios reject 了 undefined，Nuxt 渲染器后面报误导性的 renderResourceHints 错误。修正为 `Promise.reject(error.response || error)`——SSR 环境的异常要在渲染前给干净的对象
2. **cookie 作用域**：登录 cookie 必须用 `{ path: '/' }`，写死 `domain: 'localhost'` 会导致非 localhost 访问（如局域网 IP）登录态丢失

---

# 十三、通用工程能力

## 13.1 全局异常处理 + 统一返回

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(YyghException.class)  // 业务异常：code + msg 返回，warn 级日志
    @ExceptionHandler(Exception.class)      // 未知异常：统一"执行全局异常处理"，error 级日志含堆栈
}
```

统一 `R` 封装（success/code/message/data）。设计细节：业务异常不打完整堆栈（日志降噪 + 防敏感请求信息泄漏）。

**坑**：全局兜底会掩盖真实错误（hos_record_id 长度问题就是被它盖住的），排查必须看服务端日志（logback ERROR 单独落 `log_error.log`）。

## 13.2 日志体系

> logback-spring.xml：CONSOLE + INFO/WARN/ERROR 三级 RollingFileAppender，按天 + 100MB 滚动，保留 15 天；`springProfile` 区分 dev/prod。跨服务排查链路的意识：网关 → 业务 → 中间件逐层看日志。

## 13.3 环境配置管理

```properties
spring.datasource.password=${YYGH_DB_PASSWORD:123456}
yygh.jwt.secret=${YYGH_JWT_SECRET:}
```

> 占位符 + 环境变量注入，密钥零硬编码（`secret` 默认值为空 + 启动校验 ≥32 字节，缺配置 fail-fast）。上线：K8s Secret / Nacos 配置中心。

## 13.4 医院模拟系统（hospital-manage）

> Thymeleaf + Spring Boot 的独立系统，模拟"外部合作医院"上报医院/科室/排班数据，与主平台交互走 HTTP + 签名（sign + 时间戳防重放，bootstrap-token 引导）。体现**外部系统集成**能力：真实世界里医院数据来自外部，签名验签 + 排班上报接口是平台边界。

## 13.5 排查方法论（面试官最爱"你怎么排查问题"）

> 我的分层定位法（贯穿所有事故）：
> 1. **基础设施层**：端口监听（Get-NetTCPConnection）→ Nacos 注册列表 → 中间件 TCP 连通
> 2. **绕过中间层直连**：跳过网关直连业务服务 → 问题在网关还是业务，一测便知
> 3. **看真实日志**：全局异常掩盖堆栈时，去 log_error.log 找 Caused by
> 4. **手工验证密码学问题**：JWT 不一致那次，我用 PowerShell HMAC-SHA256 对两个候选密钥分别验签，直接锁定密钥归属
> 案例：AI "50300" 报错 → 表面是 AI 服务故障 → 日志发现是 memoryId 类型反序列化错误 / 中文 body 编码丢失（PowerShell 发中文 JSON 必须 UTF8 GetBytes，否则变 ????）→ 测试方法问题而非服务故障。**先证明问题存在，再怀疑代码**。

---

# 十四、高频开放题

## Q1：项目最大的挑战？

> 两个，都讲数据和方案：
> 1. **号源并发**：超卖风险 → MongoDB `findAndModify` 条件原子扣减 + Redis 幂等锁 + CAS 回补，三层纵深
> 2. **跨服务一致性**：取消涉及订单/医院/支付三方且跨 MySQL+Mongo 存储，2PC 不可行 → outbox 补偿表 + 认领式 worker + 重试 + 抢占式取消申请，最终一致

## Q2：为什么用微服务而不是单体？

> 见 [1.3](#13-为什么用微服务必考)。记得补代价（运维/分布式复杂度）显清醒。

## Q3：如果重新设计会改进什么？

> 1. 密钥/配置统一进 Nacos（消灭双路径读取）
> 2. 号源查询加多级缓存（Redis + 本地 Caffeine，防查号高峰）
> 3. MQ 消费补全幂等表（现在是条件更新幂等，通用性差些）
> 4. 可观测性：SkyWalking 全链路追踪 + Prometheus 告警
> 5. AI 服务加限流熔折（Sentinel，防 LLM 调用被刷烧钱）
> 6. CI/CD 与容器化（目前 IDEA 起服务是开发形态）

## Q4：并发量能扛多少？怎么提升？

> 瓶颈分析：下单链路串行点在 MongoDB 条件扣减和 MySQL 落库。估算单实例几百 QPS，水平扩容订单服务即可线性提升（幂等已保证多实例安全）。再往上：号源预扣移 Redis（Lua 扣减）、读写分离、热点医院数据缓存。

## Q5：你负责的部分？遇到最难 debug 的一次？

> 首推 JWT 密钥不一致事故（完整故事见网关章节）——因为排查过程展示：分层定位 → 绕网关直连 → 手工验签锁定根因。次推 LangChain4j ThreadLocal 丢 token（异步线程 + 请求上下文组合坑）。

---

# 附录 A：踩坑记录速查表

| 坑 | 现象 | 根因 | 修复 | 沉淀 |
|---|---|---|---|---|
| JWT 密钥双路径 | 全部 auth 接口 50008 | 网关读 Spring 属性、业务服务读环境变量，两个进程密钥不同 | 网关显式注入同密钥重启 | 密钥收敛单一来源；分层排查法 |
| hos_record_id 短 | 下单"执行全局异常处理" | 幂等键 "MOCK-"+单号 超 varchar(30) | 扩 varchar(100) | 字段长度预估拼接上限；看日志别信兜底文案 |
| MongoDB 时区 | 前端显示无号 | 存储时间被误改，查询条件错位 | 恢复 UTC 16:00 存储 + JVM 时区解析 | 先统一存储基准；改数据先备份 |
| Feign 404 | 扣号接口 404 | 医院侧 Controller 缺 @PostMapping import | 补 import | 404 先对 method 和注解生效性 |
| AI token 丢失 | AI 预约报登录问题 | 工具异步线程 ThreadLocal 丢 | 三级降级取 token + memoryId 注册表 | 异步 + 请求上下文必丢 ThreadLocal |
| AI 50300 假故障 | AI"不可用" | 测试端中文 JSON 编码成 ???? / memoryId 类型错 | UTF8 GetBytes 发请求 | 先证明问题存在再怀疑代码 |
| starter 冲突 | AI 服务起不来 | 同时引多个 AI model starter | pom 只留 open-ai | 注释配置救不了 starter 冲突 |
| SSR 假错误 | renderResourceHints 报错 | axios reject(undefined) | reject(error.response \|\| error) | SSR 异常要给干净对象 |
| cookie 作用域 | 局域网访问丢登录态 | domain 写死 localhost | { path: '/' } | 域名写死是隐形炸弹 |

# 附录 B：技术栈清单速查表

| 层 | 技术 |
|---|---|
| 微服务 | Spring Boot 3.x、Spring Cloud Alibaba、Gateway、Nacos、OpenFeign、LoadBalancer |
| 存储 | MySQL（订单/用户/字典，多库隔离）、MongoDB（医院/排班/聊天记忆）、Redis（验证码/锁/缓存） |
| 消息 | RabbitMQ（direct 交换机：订单同步/短信/任务） |
| ORM | MyBatis-Plus（Lambda 条件/逻辑删除/分页插件） |
| 认证 | JWT（HMAC-SHA256 + GZIP 负载压缩，网关统一验签） |
| AI | LangChain4j 1.4.0-beta10（@AiService/@Tool/@MemoryId）、DeepSeek（OpenAI 兼容）、Tool Calling、MongoDB ChatMemory |
| 前端 | Vue 2 + ElementUI + Vuex + axios（管理端）；Nuxt SSR（门户） |
| 运维 | logback 分级滚动日志、环境变量占位符配置、log_error.log 定位 |
| 分布式模式 | 幂等键 + Redis 锁、outbox 补偿表、取消申请抢占、条件原子扣减、Lua 原子校验 |
