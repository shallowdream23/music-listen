import axios from 'axios'

const service = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
})

service.interceptors.request.use(
  (config) => {
    // 直接从 localStorage 读取 token，避免多个 Pinia 实例不同步的问题
    const token = localStorage.getItem('music_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

service.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const token = localStorage.getItem('music_token')
      // 仅在本地认为“已登录”的情况下才做登出和跳转
      if (token) {
        localStorage.removeItem('music_token')
        window.location.href = '/'
      }
    }
    return Promise.reject(error)
  },
)

export default service

