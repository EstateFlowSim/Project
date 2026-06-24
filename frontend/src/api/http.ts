import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'

let isRefreshing = false
let waitQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = []

function flushQueue(token: string | null, err: unknown = null) {
  waitQueue.forEach(p => (token ? p.resolve(token) : p.reject(err)))
  waitQueue = []
}

export const http = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

http.interceptors.request.use(cfg => {
  const token = localStorage.getItem('access_token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

http.interceptors.response.use(
  res => res,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }
    original._retry = true

    // 큐에 쌓인 요청은 refresh 완료 후 토큰 받아서 재시도
    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        waitQueue.push({ resolve, reject })
      }).then(token => {
        original.headers.Authorization = `Bearer ${token}`
        return http(original)
      })
    }

    isRefreshing = true

    try {
      const refreshToken = localStorage.getItem('refresh_token')
      if (!refreshToken) throw new Error('no_refresh_token')

      const { data } = await axios.post<{ accessToken: string; refreshToken: string }>(
        '/api/auth/refresh',
        { refreshToken },
      )

      localStorage.setItem('access_token', data.accessToken)
      localStorage.setItem('refresh_token', data.refreshToken)

      // authStore의 accessToken ref를 동기화
      window.dispatchEvent(new CustomEvent('auth:refreshed', { detail: data.accessToken }))

      flushQueue(data.accessToken)
      original.headers.Authorization = `Bearer ${data.accessToken}`
      return http(original)
    } catch (err) {
      flushQueue(null, err)
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('nickname')
      window.location.replace('/login')
      return Promise.reject(err)
    } finally {
      isRefreshing = false
    }
  },
)
