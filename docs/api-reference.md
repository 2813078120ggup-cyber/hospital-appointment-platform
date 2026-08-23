# 接口文档

> 本文档由 `tools/generate_reference_docs.py` 对 Controller 注解进行静态提取。它描述代码中已声明的路由，不替代联调、权限测试或运行时 OpenAPI 文档。

## 调用约定

- 统一入口：`http://localhost:8222`（Spring Cloud Gateway）。
- 业务服务通常返回 `R`，成功码为 `20000`；医院模拟端的页面控制器返回视图或重定向。
- 路径包含 `/auth/` 的接口通过请求头 `token: <JWT>` 识别登录用户。
- 路径包含 `/inner/` 的接口供 Feign 等内部调用，生产环境必须限制外部访问。
- `ANY` 表示源码使用了未限定 method 的 `@RequestMapping`，实际可接受多个 HTTP 方法。

## 运行时文档入口

| 模块 | 端口 | 文档入口 |
| --- | ---: | --- |
| Spring Cloud 业务服务 | 各服务端口 | `http://localhost:<port>/doc.html`（Knife4j/OpenAPI，需服务启动） |
| 医院模拟端 | 9998 | `http://localhost:9998/swagger-ui.html`（Springfox Swagger 2） |

静态扫描共发现 22 个 Controller、88 个映射方法。

## `hospital-manage` / 医院管理接口

Controller：`ApiController`  ·  端口：`9998`  ·  源码：`hospital-manage/hospital-manage/src/main/java/com/atguigu/hospital/controller/ApiController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `ANY` | `/hospitalSet/index` | getHospitalSet | 公开/按业务校验 | — | `String` |
| `ANY` | `/hospitalSet/save` | createHospitalSet | 公开/按业务校验 | HospitalSet hospitalSet | `String` |
| `ANY` | `/hospital/index` | getHospital | 公开/按业务校验 | — | `String` |
| `ANY` | `/hospital/create` | createHospital | 公开/按业务校验 | — | `String` |
| `POST` | `/hospital/save` | saveHospital | 公开/按业务校验 | String data | `String` |
| `ANY` | `/department/list` | findDepartment | 公开/按业务校验 | RequestParam  int pageNum; RequestParam  int pageSize | `String` |
| `ANY` | `/department/create` | create | 公开/按业务校验 | — | `String` |
| `POST` | `/department/save` | save | 公开/按业务校验 | String data | `String` |
| `ANY` | `/schedule/list` | findSchedule | 公开/按业务校验 | RequestParam  int pageNum; RequestParam  int pageSize | `String` |
| `ANY` | `/schedule/create` | createSchedule | 公开/按业务校验 | — | `String` |
| `POST` | `/schedule/save` | saveSchedule | 公开/按业务校验 | String data | `String` |
| `ANY` | `/hospital/createBatch` | createHospitalBatch | 公开/按业务校验 | — | `String` |
| `POST` | `/hospital/saveBatch` | saveBatchHospital | 公开/按业务校验 | — | `String` |
| `GET` | `/department/remove/{depcode}` | removeDepartment | 公开/按业务校验 | PathVariable  String depcode | `String` |
| `GET` | `/schedule/remove/{hosScheduleId}` | removeSchedule | 公开/按业务校验 | PathVariable  String hosScheduleId | `String` |

## `hospital-manage` / BaseController

Controller：`BaseController`  ·  端口：`9998`  ·  源码：`hospital-manage/hospital-manage/src/main/java/com/atguigu/hospital/controller/BaseController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| — | — | 未发现可静态提取的映射方法 | — | — | — |

## `hospital-manage` / 医院设置管理接口

Controller：`HospSetController`  ·  端口：`9998`  ·  源码：`hospital-manage/hospital-manage/src/main/java/com/atguigu/hospital/controller/HospSetController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/hospSet/updateSignKey` | updateSignKey | 公开/按业务校验 | — | `Result` |

## `hospital-manage` / 医院管理接口

Controller：`HospitalController`  ·  端口：`9998`  ·  源码：`hospital-manage/hospital-manage/src/main/java/com/atguigu/hospital/controller/HospitalController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/order/submitOrder` | AgreeAccountLendProject | 公开/按业务校验 | — | `Result` |
| `POST` | `/order/updatePayStatus` | updatePayStatus | 公开/按业务校验 | — | `Result` |
| `POST` | `/order/updateCancelStatus` | updateCancelStatus | 公开/按业务校验 | — | `Result` |

## `hospital-manage` / IndexController

Controller：`IndexController`  ·  端口：`9998`  ·  源码：`hospital-manage/hospital-manage/src/main/java/com/atguigu/hospital/controller/IndexController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `ANY` | `/` | index | 公开/按业务校验 | — | `String` |
| `GET` | `/main` | main | 公开/按业务校验 | — | `String` |
| `GET` | `/auth` | auth | 公开/按业务校验 | — | `String` |
| `GET` | `/login` | login | 公开/按业务校验 | — | `String` |

## `service-cmn` / 数据字典接口

Controller：`DictController`  ·  端口：`8202`  ·  源码：`yygh_parent/yygh_parent/service/service_cmn/src/main/java/com/atguigu/yygh/cmn/controller/DictController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/admin/cmn/dict/findByDictCode/{dictCode}` | 根据dictCode获取下级节点 | 管理端 | PathVariable  String dictCode | `R` |
| `GET` | `/admin/cmn/dict/getName/{parentDictCode}/{value}` | 获取数据字典名称 | 管理端 | PathVariable  String parentDictCode; PathVariable  String value | `String` |
| `GET` | `/admin/cmn/dict/getName/{value}` | 获取数据字典名称 | 管理端 | PathVariable  String value | `String` |
| `POST` | `/admin/cmn/dict/importDataNew` | 导入 | 管理端 | MultipartFile file | `R` |
| `POST` | `/admin/cmn/dict/importData` | 导入 | 管理端 | MultipartFile file | `R` |
| `GET` | `/admin/cmn/dict/exportData` | 导出 | 管理端 | — | `void` |
| `GET` | `/admin/cmn/dict/findDataById/{id}` | 数据字典列表 | 管理端 | PathVariable  Long id | `R` |

## `service-hosp` / 医院管理数据API接口

Controller：`ApiController`  ·  端口：`8201`  ·  源码：`yygh_parent/yygh_parent/service/service_hosp/src/main/java/com/atguigu/yygh/hosp/controller/api/ApiController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/hosp/schedule/remove` | 删除排班 | 公开/按业务校验 | — | `Result` |
| `POST` | `/api/hosp/schedule/list` | 获取排班分页列表 | 公开/按业务校验 | — | `Result` |
| `POST` | `/api/hosp/saveSchedule` | 上传排班 | 公开/按业务校验 | — | `Result` |
| `POST` | `/api/hosp/department/remove` | 删除科室 | 公开/按业务校验 | — | `Result` |
| `POST` | `/api/hosp/department/list` | 科室获取分页列表 | 公开/按业务校验 | — | `Result` |
| `POST` | `/api/hosp/saveDepartment` | 上传科室 | 公开/按业务校验 | — | `Result` |
| `POST` | `/api/hosp/hospital/show` | 获取医院信息 | 公开/按业务校验 | — | `Result` |
| `POST` | `/api/hosp/saveHospital` | 上传医院 | 公开/按业务校验 | — | `Result` |

## `service-hosp` / 科室数据接口

Controller：`DepartmentController`  ·  端口：`8201`  ·  源码：`yygh_parent/yygh_parent/service/service_hosp/src/main/java/com/atguigu/yygh/hosp/controller/DepartmentController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/admin/hosp/department/getDeptList/{hoscode}` | 查询医院所有科室列表 | 管理端 | PathVariable  String hoscode | `R` |

## `service-hosp` / 医院显示接口

Controller：`HospitalApiController`  ·  端口：`8201`  ·  源码：`yygh_parent/yygh_parent/service/service_hosp/src/main/java/com/atguigu/yygh/hosp/controller/api/HospitalApiController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/hosp/hospital/getSchedule/{id}` | 获取排班详情 | 公开/按业务校验 | PathVariable  String id | `R` |
| `GET` | `/api/hosp/hospital/auth/getBookingScheduleRule/{page}/{limit}/{hoscode}/{depcode}` | 获取可预约排班数据 | 登录用户 | PathVariable  Integer page; PathVariable  Integer limit; PathVariable  String hoscode; PathVariable  String depcode | `R` |
| `GET` | `/api/hosp/hospital/auth/findScheduleList/{hoscode}/{depcode}/{workDate}` | 获取排班数据 | 登录用户 | PathVariable  String hoscode; PathVariable  String depcode; PathVariable  String workDate | `R` |
| `GET` | `/api/hosp/hospital/department/{hoscode}` | 获取科室列表 | 公开/按业务校验 | PathVariable  String hoscode | `R` |
| `GET` | `/api/hosp/hospital/{page}/{limit}` | 获取分页列表 | 公开/按业务校验 | PathVariable  Integer page; PathVariable  Integer limit; HospitalQueryVo hospitalQueryVo | `R` |
| `GET` | `/api/hosp/hospital/findByHosname/{hosname}` | 根据医院名称获取医院列表 | 公开/按业务校验 | PathVariable  String hosname | `R` |
| `GET` | `/api/hosp/hospital/{hoscode}` | 医院预约挂号详情 | 公开/按业务校验 | PathVariable  String hoscode | `R` |
| `GET` | `/api/hosp/hospital/inner/getScheduleOrderVo/{scheduleId}` | 医院预约挂号详情 | 内部调用 | PathVariable  String scheduleId | `ScheduleOrderVo` |

## `service-hosp` / 医院接口

Controller：`HospitalController`  ·  端口：`8201`  ·  源码：`yygh_parent/yygh_parent/service/service_hosp/src/main/java/com/atguigu/yygh/hosp/controller/HospitalController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/admin/hosp/hospital/{page}/{limit}` | 医院获取分页列表 | 管理端 | PathVariable  Integer page; PathVariable  Integer limit; HospitalQueryVo hospitalQueryVo | `R` |
| `GET` | `/admin/hosp/hospital/updateStatus/{id}/{status}` | 更新上线状态 | 管理端 | PathVariable  String id; PathVariable  Integer status | `R` |
| `GET` | `/admin/hosp/hospital/show/{id}` | 获取医院详情 | 管理端 | PathVariable  String id | `R` |

## `service-hosp` / 医院设置管理接口

Controller：`HospitalSetController`  ·  端口：`8201`  ·  源码：`yygh_parent/yygh_parent/service/service_hosp/src/main/java/com/atguigu/yygh/hosp/controller/HospitalSetController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `PUT` | `/admin/hosp/hospitalSet/lockHospitalSet/{id}/{status}` | lockHospitalSet | 管理端 | PathVariable  Long id; PathVariable  Integer status | `R` |
| `DELETE` | `/admin/hosp/hospitalSet/batchRemove` | 批量删除 | 管理端 | RequestBody  List<Long> idList | `R` |
| `PUT` | `/admin/hosp/hospitalSet/updateHospSet` | 修改 | 管理端 | RequestBody  HospitalSet hospitalSet | `R` |
| `POST` | `/admin/hosp/hospitalSet/addHospSet` | 修改 | 管理端 | RequestBody  HospitalSet hospitalSet | `R` |
| `GET` | `/admin/hosp/hospitalSet/getHospSetById/{id}` | 根据id查询 | 管理端 | PathVariable  Long id | `R` |
| `POST` | `/admin/hosp/hospitalSet/saveHospSet` | 添加 | 管理端 | RequestBody  HospitalSet hospitalSet | `R` |
| `POST` | `/admin/hosp/hospitalSet/findPageQueryHospSet/{current}/{limit}` | 条件分页查询requestbody | 管理端 | PathVariable  long current; PathVariable  long limit; RequestBody (required = false) HospitalSetQueryVo hospitalSetQueryVo | `R` |
| `GET` | `/admin/hosp/hospitalSet/findPageQuery/{current}/{limit}` | 条件分页查询 | 管理端 | PathVariable  long current; PathVariable  long limit; HospitalSetQueryVo hospitalSetQueryVo | `R` |
| `GET` | `/admin/hosp/hospitalSet/findPage/{current}/{limit}` | 分页查询 | 管理端 | PathVariable  long current; PathVariable  long limit | `R` |
| `GET` | `/admin/hosp/hospitalSet/findAll` | 查询所有数据 | 管理端 | — | `R` |
| `DELETE` | `/admin/hosp/hospitalSet/remove/{id}` | 逻辑删除 | 管理端 | PathVariable  Long id | `R` |

## `service-hosp` / LoginController

Controller：`LoginController`  ·  端口：`8201`  ·  源码：`yygh_parent/yygh_parent/service/service_hosp/src/main/java/com/atguigu/yygh/hosp/controller/LoginController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/user/hosp/login` | login | 公开/按业务校验 | — | `R` |
| `GET` | `/user/hosp/info` | info | 公开/按业务校验 | — | `R` |

## `service-hosp` / 排班数据接口

Controller：`ScheduleController`  ·  端口：`8201`  ·  源码：`yygh_parent/yygh_parent/service/service_hosp/src/main/java/com/atguigu/yygh/hosp/controller/ScheduleController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/admin/hosp/schedule/getScheduleRule/{page}/{limit}/{hoscode}/{depcode}` | 查询排班规则数据 | 管理端 | PathVariable  long page; PathVariable  long limit; PathVariable  String hoscode; PathVariable  String depcode | `R` |
| `GET` | `/admin/hosp/schedule/getScheduleDetail/{hoscode}/{depcode}/{workDate}` | 查询排班详细信息 | 管理端 | PathVariable  String hoscode; PathVariable  String depcode; PathVariable  String workDate | `R` |

## `service-msm` / MsmController

Controller：`MsmController`  ·  端口：`8204`  ·  源码：`yygh_parent/yygh_parent/service/service_msm/src/main/java/com/atguigu/yygh/msm/controller/MsmController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/msm/send/{phone}` | code | 公开/按业务校验 | PathVariable  String phone | `R` |

## `service-orders` / OrderInfoController

Controller：`OrderInfoController`  ·  端口：`8207`  ·  源码：`yygh_parent/yygh_parent/service/service_orders/src/main/java/com/atguigu/yygh/orders/controller/OrderInfoController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/order/orderInfo/auth/submitOrder/{scheduleId}/{patientId}` | submitOrder | 登录用户 | PathVariable  String scheduleId; PathVariable  Long patientId | `R` |
| `GET` | `/api/order/orderInfo/auth/getOrders/{orderId}` | getOrders | 登录用户 | PathVariable  Long orderId | `R` |
| `GET` | `/api/order/orderInfo/auth/cancelOrder/{orderId}` | cancelOrder | 登录用户 | PathVariable  Long orderId | `R` |
| `POST` | `/api/order/orderInfo/inner/getCountMap` | getCountMap | 内部调用 | RequestBody  OrderCountQueryVo orderCountQueryVo | `Map<String,Object>` |

## `service-orders` / WeixinController

Controller：`WeixinController`  ·  端口：`8207`  ·  源码：`yygh_parent/yygh_parent/service/service_orders/src/main/java/com/atguigu/yygh/orders/controller/WeixinController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/order/weixin/createNative/{orderId}` | createNative | 公开/按业务校验 | PathVariable  Long orderId | `R` |
| `GET` | `/api/order/weixin/queryPayStatus/{orderId}` | 查询支付状态 | 公开/按业务校验 | PathVariable  Long orderId | `R` |

## `service-oss` / 阿里云文件管理

Controller：`FileUploadController`  ·  端口：`8205`  ·  源码：`yygh_parent/yygh_parent/service/service_oss/src/main/java/com/atguigu/yygh/oss/controller/FileUploadController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/admin/oss/file/upload` | uploadFile | 管理端 | MultipartFile file | `R` |

## `service-sta` / StatisticsController

Controller：`StatisticsController`  ·  端口：`8260`  ·  源码：`yygh_parent/yygh_parent/service/service_statistics/src/main/java/com/atguigu/yygh/statistics/controller/StatisticsController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/admin/statistics/getCountMap` | getCountMap | 管理端 | OrderCountQueryVo orderCountQueryVo | `R` |

## `service-user` / PatientController

Controller：`PatientController`  ·  端口：`8160`  ·  源码：`yygh_parent/yygh_parent/service/service_user/src/main/java/com/atguigu/yygh/user/controller/PatientController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/user/patient/inner/getPatientInfoById/{id}` | getPatientInfoById | 内部调用 | PathVariable  Long id | `Patient` |
| `GET` | `/api/user/patient/auth/get/{id}` | getPatient | 登录用户 | PathVariable  Long id | `R` |
| `GET` | `/api/user/patient/auth/findAll` | findAll | 登录用户 | — | `R` |
| `POST` | `/api/user/patient/auth/save` | savePatient | 登录用户 | RequestBody  Patient patient | `R` |
| `POST` | `/api/user/patient/auth/update` | updatePatient | 登录用户 | RequestBody  Patient patient | `R` |
| `DELETE` | `/api/user/patient/auth/remove/{id}` | removePatient | 登录用户 | PathVariable  Long id | `R` |

## `service-user` / UserController

Controller：`UserController`  ·  端口：`8160`  ·  源码：`yygh_parent/yygh_parent/service/service_user/src/main/java/com/atguigu/yygh/user/controller/admin/UserController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/admin/user/approval/{userId}/{authStatus}` | approval | 管理端 | PathVariable  Long userId; PathVariable  Integer authStatus | `R` |
| `GET` | `/admin/user/show/{userId}` | showUserInfo | 管理端 | PathVariable  Long userId | `R` |
| `GET` | `/admin/user/{page}/{limit}` | list | 管理端 | PathVariable  Long page; PathVariable  Long limit; UserInfoQueryVo userInfoQueryVo | `R` |

## `service-user` / UserInfoController

Controller：`UserInfoController`  ·  端口：`8160`  ·  源码：`yygh_parent/yygh_parent/service/service_user/src/main/java/com/atguigu/yygh/user/controller/UserInfoController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/user/auth/userAuth` | userAuth | 登录用户 | RequestBody  UserAuthVo userAuthVo | `R` |
| `GET` | `/api/user/auth/getUserInfo` | getUserInfo | 登录用户 | — | `R` |
| `POST` | `/api/user/login` | 会员登录 | 公开/按业务校验 | RequestBody  LoginVo loginVo | `R` |

## `service-user` / WeixinApiController

Controller：`WeixinApiController`  ·  端口：`8160`  ·  源码：`yygh_parent/yygh_parent/service/service_user/src/main/java/com/atguigu/yygh/user/controller/WeixinApiController.java`

| 方法 | 路径 | 用途 | 访问范围 | 参数摘要 | 返回类型 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/user/wx/getLoginParam` | getLoginParam | 公开/按业务校验 | — | `R` |
| `GET` | `/api/user/wx/callback` | callback | 公开/按业务校验 | String code; String state; HttpSession session | `String` |
