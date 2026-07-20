# Debug Session: payment-result-stuck
- **Status**: [OPEN]
- **Issue**: 支付结果页长期停留在“支付结果确认中/处理中”，页面未进入已支付状态；同时用户反馈下单流程看不到优惠券使用环节。
- **Debug Server**: Pending
- **Log File**: .dbg/trae-debug-log-payment-result-stuck.ndjson

## Reproduction Steps
1. 提交订单进入支付流程
2. 完成支付或进入支付结果页
3. 观察支付结果页是否持续停留在“确认中”
4. 检查服务端是否收到支付回调、是否发布 MQ、是否消费更新订单
5. 检查下单页是否存在优惠券选择/抵扣入口

## Hypotheses & Verification
| ID | Hypothesis | Likelihood | Effort | Evidence |
|----|------------|------------|--------|----------|
| A | 支付回调未成功到达后端，导致支付记录和订单状态都未更新 | High | Medium | Pending |
| B | 支付回调已执行，但 RabbitMQ 未连接或消费失败，导致订单状态未被异步更新 | High | Medium | Pending |
| C | 支付结果页轮询接口拿到的订单状态字段或判断条件不匹配，前端一直显示处理中 | Medium | Low | Pending |
| D | 优惠券功能只实现了领券和列表展示，根本没有接入下单结算页，所以用户看不到使用入口 | High | Low | Pending |
| E | 支付成功后没有把用户券核销/订单金额重算接入后端，导致即使有券也不会出现在支付环节 | Medium | Low | Pending |

## Log Evidence
- Static evidence confirmed:
  - `alipay.notify-url` 默认值为 `http://localhost:8090/api/order/pay-notify`，外部支付宝服务器不可达。
  - 支付结果页只认订单状态 `1` 为成功，订单状态未更新时会持续展示“确认中”。
  - 下单确认页原先未提交任何优惠券字段，优惠券仅在个人中心支持兑换领取。
- Fix verification:
  - 后端已新增主动对账同步能力：支付结果页轮询时会触发服务端向支付宝查询支付状态，查到成功后补写支付记录并继续走 MQ 更新订单。
  - 后端已接入订单优惠券锁定、支付后核销、待支付取消后释放。
  - 本地验证通过：`mvn -q -DskipTests compile` 成功，`npm run build` 成功。

## Verification Conclusion
- Hypothesis A confirmed: 支付宝异步回调使用 `localhost` 导致本地开发环境下回调无法从外部访问。
- Hypothesis B partially mitigated: 即使异步回调未到达，结果页现可通过主动对账补齐支付状态并继续走 MQ 链路。
- Hypothesis D confirmed and fixed: 优惠券此前仅完成领取展示，现已接入下单确认与订单创建流程。
- Hypothesis E confirmed and fixed: 下单时会锁定用户券，支付成功后由订单消费端核销，待支付取消时释放。
