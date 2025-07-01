import axios from 'axios'
import { ElMessage, SelectProps } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/store/useAuthStore'

const http = axios.create({
  baseURL: '/api/app',
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 30000
})


// 请求拦截器
http.interceptors.request.use(config => {

  if (config.url.includes('/login') || config.url.includes('/refresh-token')
    || config.url.includes('/register') || config.url.includes('/send-code')
    || config.url.includes('/maintenanceRecords/push') || config.url.includes('/maintenanceRecords/detial')
  ) {
    return config;
  }

  // 添加Token
  const authStore = useAuthStore();
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
},
  error => {
    return Promise.reject(error)
  }
)

let isRefreshing = false
let requests = []

// 响应拦截器: 处理响应数据
http.interceptors.response.use(
  async response => {
    const res = response.data
    const originalRequest = response.config

    if (res.status === 401) {
      if (!isRefreshing) {

        // 后端表示不是token过期
        if (!res.code === 'A406') {
          isRefreshing = false
          requests = []
          router.push('/auth')
          return Promise.reject(e)
        }
        console.log('token过期')
        console.log(res)
        console.log(originalRequest)

        // 尝试刷新token
        isRefreshing = true
        try {
          await useAuthStore().refreshToken()
          isRefreshing = false
          // 执行队列中的请求
          requests.forEach(cb => cb())
          requests = []

          // 重新发起当前请求
          originalRequest.headers.Authorization = `Bearer ${useAuthStore().accessToken}`
          return http(originalRequest)
        } catch (e) {
          isRefreshing = false
          requests = []
          router.push('/auth')
          return Promise.reject(e)
        }
      } else {
        // 正在刷新，队列等待
        return new Promise(resolve => {
          requests.push(() => {
            originalRequest.headers.Authorization = `Bearer ${useAuthStore().accessToken}`
            resolve(http(originalRequest))
          })
        })
      }
    }

    if (res.status !== 200) {
      // ElMessage({
      //   message: res.message || '请求错误',
      //   type: 'error',
      //   duration: 5 * 1000
      // })
      return Promise.reject(new Error(res.message || '请求错误'))
    } else {
      return res
    }
  },
  error => {
    // 处理分后端主动报错
    let message = error.response?.data?.message || '请求失败'
    ElMessage({
      message: message,
      type: 'error',
      duration: 3000
    })
    return Promise.reject(error)
  }
)

export default http