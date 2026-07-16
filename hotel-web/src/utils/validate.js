export function validatePhone(rule, value, callback) {
  const normalizedValue = String(value || '').trim()

  if (!normalizedValue) {
    callback(new Error('请输入手机号'))
  } else if (!/^1[3-9]\d{9}$/.test(normalizedValue)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

export function validateEmail(rule, value, callback) {
  if (!value) {
    callback()
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

export function validatePassword(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码长度至少6位'))
  } else {
    callback()
  }
}
