# Debug Session: system-regression-sweep
- **Status**: [OPEN]
- **Issue**: 全量回归检查系统启动、后台入口、搜索排序、支付、评价发布等链路，定位仍存在的运行时问题并完成最小修复。
- **Debug Server**: Pending
- **Log File**: .dbg/trae-debug-log-system-regression-sweep.ndjson

## Reproduction Steps
1. 启动后端服务并检查数据库连接、Swagger 页面与核心公开接口。
2. 启动前端服务，检查首页、搜索页、酒店详情、订单页、后台入口。
3. 使用住客账号验证登录、搜索、下单、支付、评价链路。
4. 使用管理员账号验证后台酒店、订单、用户管理入口。

## Hypotheses & Verification
| ID | Hypothesis | Likelihood | Effort | Evidence |
|----|------------|------------|--------|----------|
| A | 本地运行配置仍不稳定，导致后端在不同启动方式下读不到数据库或支付宝配置 | High | Low | Pending |
| B | 搜索排序前后端参数已传递，但后端查询层未真正参与排序 | High | Low | Pending |
| C | 评价链路缺少订单状态/归属校验或前端缺少提交流程，导致“无法写评价” | High | Low | Pending |
| D | 支付前端仍停留在占位逻辑，或后端沙箱配置缺失导致支付无法发起 | High | Medium | Pending |
| E | 后台访问问题来自管理员身份链路、路由守卫或启动入口理解偏差，而非页面本身缺失 | Medium | Low | Pending |

## Log Evidence
- 后端已稳定启动在 `8090`，`/swagger-ui.html` 返回 `200`。
- 搜索排序接口复现：
  - `sortBy=price` 返回顺序为酒店 `3 -> 1 -> 2`，对应最低价 `498 -> 568 -> 888`。
  - `sortBy=score` 返回顺序为酒店 `1 -> 2 -> 3`（当前同分时回退到创建时间）。
- 真实住客链路复现：
  - `POST /api/user/send-code` 成功返回 `debugCode`。
  - `POST /api/user/register` 成功。
  - `POST /api/user/login` 成功，返回用户 token。
  - 使用旧脚本调用 `/api/order/create` 返回 `400: 订单明细不能为空`，确认是脚本仍按旧协议发送请求。
  - 使用当前前端协议（含 `items` 数组）再次调用 `/api/order/create` 成功创建订单。
- 管理员链路复现：
  - `13800000000 / admin123` 登录成功。
  - `GET /api/admin/orders` 成功返回数据，后台权限链路正常。
- 支付链路复现：
  - `POST /api/order/3/pay` 返回 `400: 支付宝配置未完成，请先在 application-local.yml 中填写真实沙箱参数`。
  - 说明支付代码已接线，当前阻塞点是沙箱配置缺失，不是前端仍为占位页。
- 评价链路复现与分析：
  - 代码中几乎不存在把订单推进到 `status = 6` 的业务流。
  - 已确认支付回调只会把订单置为 `1`，退房确认会置为 `5`。
  - 原先评价逻辑仅允许 `6`，因此“写评价”入口几乎无法触达。
  - 运行时复测中，`POST /api/review/create` 一度出现“接口返回 200 但列表仍为空”的现象。
  - 直接查询 `t_review` 后确认数据库与列表查询本身正常，问题集中在运行中的评价写入链路。
  - 已为评价创建补上事务、显式 `createTime` 赋值，并在插入结果不为 `1` 时主动抛错，避免静默成功。
  - 重启后端后重新验证：
    - 使用住客账号创建测试订单 `#4`
    - 使用管理员接口将订单推进为 `status = 5`
    - 再次调用 `POST /api/review/create` 返回 `200`
    - `GET /api/review/hotel/1?page=1&size=10` 返回 `total = 2`，最新评价 `id = 2`
  - 结论：评价链路现已完成端到端闭环验证。

## Verification Conclusion
| ID | Hypothesis | Status | Evidence Summary |
|----|------------|--------|------------------|
| A | 本地运行配置仍不稳定，导致后端在不同启动方式下读不到数据库或支付宝配置 | ✅ Confirmed | 支付接口明确因 `application-local.yml` 缺少真实沙箱参数而拒绝；数据库需本地密码或环境变量。 |
| B | 搜索排序前后端参数已传递，但后端查询层未真正参与排序 | ✅ Confirmed & Fixed | 修复后接口返回顺序已按 `min_price` 生效。 |
| C | 评价链路缺少订单状态/归属校验或前端缺少提交流程，导致“无法写评价” | ✅ Confirmed & Fixed | 原先仅允许 `status=6`，而实际流程几乎只能到 `5`；现已放宽为 `5/6` 并保留订单归属校验。 |
| D | 支付前端仍停留在占位逻辑，或后端沙箱配置缺失导致支付无法发起 | ✅ Confirmed | 前端已真正调用支付接口；当前运行时错误是后端沙箱参数缺失。 |
| E | 后台访问问题来自管理员身份链路、路由守卫或启动入口理解偏差，而非页面本身缺失 | ✅ Confirmed | 管理员登录与 `/api/admin/orders` 均成功，后台入口本身正常。 |
