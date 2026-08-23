# 数据库脚本说明

`schema/` 包含从 `D:\XZYL\代码\01-数据库脚本` 提取的纯 DDL 初始化脚本：

| 脚本 | 数据库 | 负责模块 |
| --- | --- | --- |
| `yygh_cmn.sql` | `yygh_cmn` | 通用字典服务 |
| `yygh_hosp.sql` | `yygh_hosp` | 医院服务的关系数据 |
| `yygh_manage.sql` | `yygh_manage` | 医院模拟端 |
| `yygh_order.sql` | `yygh_order` | 订单、支付和退款 |
| `yygh_user.sql` | `yygh_user` | 用户、就诊人与登录记录 |

生成器刻意排除了所有 INSERT。源 `yygh_order.sql` 与 `yygh_user.sql` 中可识别出手机号、证件号等个人信息模式，不得直接复制到仓库或共享测试环境。需要演示数据时，应使用脱敏后的独立种子文件，并由评审确认后再提交。

重新生成：

```powershell
python tools\generate_reference_docs.py --sql-source "D:\XZYL\代码\01-数据库脚本"
```

详细字段说明见 [数据库字典](../docs/database-dictionary.md)。
