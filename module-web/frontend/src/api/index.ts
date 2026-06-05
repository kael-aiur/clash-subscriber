import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

type UnauthorizedHandler = (error: unknown) => void

let unauthorizedHandler: UnauthorizedHandler | undefined

export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler
}

function isAuthEndpoint(url?: string): boolean {
  return Boolean(url?.startsWith('/auth/'))
}

// 响应拦截器：统一错误处理
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    console.error('API 错误:', message)
    if (error.response?.status === 401 && !isAuthEndpoint(error.config?.url)) {
      unauthorizedHandler?.(error)
    }
    return Promise.reject(error)
  }
)

export default api
