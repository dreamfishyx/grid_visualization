import { defineStore } from 'pinia'
import { login, register, info, logout, tokenRefresh } from '@/api/auth'
import {
  getAccessToken, setAccessToken,
  getRefreshToken, setRefreshToken, removeAll, getExpireTime, setExpireTime,
  getUserId, setUserId, getUserName, setUserName, getEmail, setEmail,
  getAvatar, setAvatar
} from '@/utils/user'
import { getRandomAvatar } from '@/config/avatarConfig'

export const useAuthStore = defineStore('user', {
  state: () => ({
    accessToken: getAccessToken() || null,
    refreshToken: getRefreshToken() || null,
    expireTime: getExpireTime() || null,
    userName: getUserName() || '',
    userId: getUserId() || null,
    email: getEmail() || '',
    avatar: getAvatar() || getRandomAvatar()
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken
  },

  actions: {
    async userLogin(userInfo) {
      try {
        const response = await login(userInfo)
        const { accessToken, refreshToken, expiration, userId, userName, email } = response.data
        console.log('response', response)
        
        this.clear()

        this.accessToken = accessToken
        this.expireTime = Number(expiration)
        this.refreshToken = refreshToken
        this.userId = userId
        this.userName = userName
        this.email = email
        this.avatar = getRandomAvatar()

        setAccessToken(accessToken)
        setRefreshToken(refreshToken)
        setExpireTime(expiration)
        setUserId(userId)
        setUserName(userName)
        setEmail(email)
        setAvatar(this.avatar)

        return Promise.resolve()
      } catch (error) {
        return Promise.reject(error)
      }
    },
    async refreshToken() {
      try {
        const response = await tokenRefresh({ refreshToken: this.refreshToken });
        const { accessToken, refreshToken, expiration } = response.data
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expireTime = Number(expiration)
        setAccessToken(accessToken)
        setRefreshToken(refreshToken)

        return true;
      } catch (error) {
        // 刷新失败
        this.logout();
        return false;
      }
    },

    async userRegister(userInfo) {
      try {
        await register(userInfo)
        return Promise.resolve()
      } catch (error) {
        return Promise.reject(error)
      }
    },

    async logout() {
      // 先后端token加入黑名单，然后本地删除token
      await logout()
      this.clear()
    },
    
    clear() {
      this.accessToken = null
      this.refreshToken = null
      this.expireTime = null
      this.userName = ''
      this.userId = null
      removeAll()
    }
  }
}) 
