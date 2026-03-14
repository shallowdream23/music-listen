import { defineStore } from 'pinia'
import request from '../utils/request'

const TOKEN_KEY = 'music_token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: null, // { id, username, avatarUrl, isAdmin, ... }
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => !!state.userInfo?.isAdmin,
  },
  actions: {
    setToken(token) {
      this.token = token || ''
      if (this.token) {
        localStorage.setItem(TOKEN_KEY, this.token)
      } else {
        localStorage.removeItem(TOKEN_KEY)
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
        const res = await request.get('/user/getUserInfo')
        if (res.data?.code === 2000 && res.data?.data) {
          this.userInfo = res.data.data
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

