import axios from 'axios'

const ADMIN_TOKEN_KEY = 'music_admin_token'

const adminRequest = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
})

adminRequest.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(ADMIN_TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

adminRequest.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const token = localStorage.getItem(ADMIN_TOKEN_KEY)
      if (token) {
        localStorage.removeItem(ADMIN_TOKEN_KEY)
        window.location.href = '/admin/login'
      }
    }
    return Promise.reject(error)
  },
)

export default adminRequest
export { ADMIN_TOKEN_KEY }
