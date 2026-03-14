import { defineStore } from 'pinia'
import adminRequest from '../utils/adminRequest'
import { ADMIN_TOKEN_KEY } from '../utils/adminRequest'

export const useAdminAuthStore = defineStore('adminAuth', {
  state: () => ({
    token: localStorage.getItem(ADMIN_TOKEN_KEY) || '',
    userInfo: null,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => !!state.userInfo?.isAdmin,
  },
  actions: {
    setToken(token) {
      this.token = token || ''
      if (this.token) {
        localStorage.setItem(ADMIN_TOKEN_KEY, this.token)
      } else {
        localStorage.removeItem(ADMIN_TOKEN_KEY)
      }
    },
    setUserInfo(info) {
      this.userInfo = info || null
    },
    async fetchUserInfo() {
      if (!this.token) {
        this.userInfo = null
        return null
      }
      try {
        const res = await adminRequest.get('/user/getUserInfo')
        const code = res.data?.code
        const data = res.data?.data
        if ((code === 2000 || code === 200) && data) {
          this.userInfo = data
          return this.userInfo
        }
      } catch (_) {
        this.userInfo = null
      }
      return null
    },
    logout() {
      this.setToken('')
      this.setUserInfo(null)
    },
  },
})
