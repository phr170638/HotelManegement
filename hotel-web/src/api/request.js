import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, removeToken } from '@/utils/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

function getStatusMessage(status, fallbackMessage) {
  if (status === 500) return fallbackMessage || '后端服务异常，请检查 hotel-server 日志'
  if (status === 502) return '前端代理无法连接后端，请确认 hotel-server 已启动'
  if (status === 503) return '后端服务不可用，请确认 hotel-server 已启动在 localhost:8080'
  if (status === 504) return '后端响应超时，请稍后重试'
  return fallbackMessage || '网络异常'
}

// 请求拦截器：注入 Token
request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：统一解包 R<T>
request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code === 200) return data
    if (!response.config?.silent) {
      ElMessage.error(message || '请求失败')
    }
    return Promise.reject(new Error(message))
  },
  error => {
    const silent = error.config?.silent
    if (error.response) {
      const { status } = error.response
      const backendMessage = error.response.data?.message
      if (status === 401) {
        removeToken()
        router.push('/login')
        if (!silent) ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        if (!silent) ElMessage.error('无权限访问')
      } else {
        if (!silent) ElMessage.error(getStatusMessage(status, backendMessage))
      }
    } else {
      if (!silent) ElMessage.error('无法连接后端服务，请确认 hotel-server 已启动并监听 localhost:8080')
    }
    return Promise.reject(error)
  }
)

export default request
