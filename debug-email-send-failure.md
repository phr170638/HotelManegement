# Debug Session: email-send-failure
- **Status**: [OPEN]
- **Issue**: 点击发送验证码时弹出“验证码发送失败，请稍后再试”，需要确认是 SMTP 认证失败、配置未生效还是运行时发信异常。
- **Debug Server**: Pending
- **Log File**: .dbg/trae-debug-log-email-send-failure.ndjson

## Reproduction Steps
1. 启动 `hotel-server`
2. 打开注册页并输入邮箱
3. 点击“发送验证码”
4. 观察后端邮件发送日志与异常堆栈

## Hypotheses & Verification
| ID | Hypothesis | Likelihood | Effort | Evidence |
|----|------------|------------|--------|----------|
| A | QQ 邮箱 SMTP 授权码错误或失效，导致认证失败 | High | Low | Pending |
| B | `application-local.yml` 没有被运行时实际加载，导致用户名/密码为空 | High | Low | Pending |
| C | 发件人地址格式或 from 配置异常，导致 `MimeMessageHelper`/`JavaMailSender` 发信失败 | Medium | Low | Pending |
| D | QQ 邮箱对当前环境做了风控拦截，返回认证/安全策略错误 | Medium | Medium | Pending |
| E | Redis/其他依赖错误被前端误显示为邮件失败 | Low | Low | Pending |

## Log Evidence
- 运行日志显示 `Mail send attempt: fromAddress=, fromName=酒店管理系统, host=smtp.qq.com, port=465`
- 紧接着抛出 `jakarta.mail.internet.AddressException: Empty address`
- 异常发生在 `MimeMessageHelper.setFrom(...)`，说明还未进入 SMTP 认证阶段
- `application-dev.yml` 中存在 `spring.mail.username/password` 的空默认值，运行时覆盖了本地配置

## Verification Conclusion
- `A`：Rejected，当前证据不是授权码错误，SMTP 认证尚未发生
- `B`：Confirmed，运行时邮件用户名未正确加载
- `C`：Confirmed，直接表现为发件人地址为空导致 `setFrom()` 失败
- `D`：Rejected，当前没有风控或认证失败证据
- `E`：Rejected，问题确实发生在邮件发送链路
