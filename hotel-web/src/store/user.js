import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, getUserInfo } from '@/api/user'
import { setToken, removeToken, getToken, setUser, getUser } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: getUser() || null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    roles: (state) => state.userInfo?.roles || [],
    userId: (state) => state.userInfo?.id
  },

  actions: {
    async login(loginData) {
      const data = await loginApi(loginData)
      this.token = data.token
      setToken(data.token)
      setUser({ id: data.userId, nickname: data.nickname, roles: data.roles })
      this.userInfo = { id: data.userId, nickname: data.nickname, roles: data.roles }
    },

    async register(registerData) {
      await registerApi(registerData)
    },

    async fetchUserInfo() {
      const data = await getUserInfo()
      this.userInfo = data
      setUser({ id: data.id, nickname: data.nickname, roles: data.roles, permissions: data.permissions })
    },

    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
    }
  }
})
